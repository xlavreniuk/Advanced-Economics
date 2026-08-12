package com.example.advancedeconomics.fabric;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import com.example.advancedeconomics.economy.EconomyManager;
import com.example.advancedeconomics.fabric.network.*;
import com.example.advancedeconomics.profession.Profession;
import com.example.advancedeconomics.profession.ProfessionManager;
import com.example.advancedeconomics.shop.PlayerUnlockManager;
import com.example.advancedeconomics.shop.ShopItem;
import com.example.advancedeconomics.shop.ShopSettings;
import com.example.advancedeconomics.shop.ShopTable;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.File;
import java.util.Locale;
import java.util.Set;

/**
 * Main Fabric Initializer (v0.33).
 * Complete /ae Command Suite:
 * - /ae (opens info / help)
 * - /ae send <player> <amount> (or /ae pay)
 * - /ae give <amount> [player] (OP command)
 * - /ae take <amount> <player> (OP command)
 * - /ae setmoney <player> <amount> (OP command)
 * - /ae setlevel <player> <level> (OP command)
 * - /ae addxp <player> <amount> (OP command)
 * - /ae help
 */
public class AdvancedEconomicsFabric implements ModInitializer {

    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        AdvancedEconomicsCommon.init();

        // 1. Register Clientbound Payloads
        PayloadTypeRegistry.clientboundPlay().register(SyncBalancePayload.TYPE, SyncBalancePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncProfessionPayload.TYPE, SyncProfessionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncSettingsPayload.TYPE, SyncSettingsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncUnlocksPayload.TYPE, SyncUnlocksPayload.CODEC);

        // 2. Register Serverbound Payloads
        PayloadTypeRegistry.serverboundPlay().register(RequestUnlockPayload.TYPE, RequestUnlockPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestBuyPayload.TYPE, RequestBuyPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestSellPayload.TYPE, RequestSellPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestUpdateSettingsPayload.TYPE, RequestUpdateSettingsPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestSetProfessionPayload.TYPE, RequestSetProfessionPayload.CODEC);

        // 3. Register Commands Suite (/ae)
        registerCommands();

        // 4. World Lifecycle Events
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
            File aeDataDir = new File(worldDir, "data/advanced_economics");
            EconomyManager.load(aeDataDir);
            ProfessionManager.load(aeDataDir);
            ShopSettings.load(aeDataDir);
            PlayerUnlockManager.load(aeDataDir);
        });

        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> {
            File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
            File aeDataDir = new File(worldDir, "data/advanced_economics");
            EconomyManager.save(aeDataDir);
            ProfessionManager.save(aeDataDir);
            ShopSettings.save(aeDataDir);
            PlayerUnlockManager.save(aeDataDir);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
            File aeDataDir = new File(worldDir, "data/advanced_economics");
            EconomyManager.save(aeDataDir);
            ProfessionManager.save(aeDataDir);
            ShopSettings.save(aeDataDir);
            PlayerUnlockManager.save(aeDataDir);
        });

        // 5. Sync & Auto-unlock inventory items on Player Join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            scanAndUnlockInventory(player);
            syncPlayerState(player);
        });

        // 6. Serverbound Packet Handlers
        ServerPlayNetworking.registerGlobalReceiver(RequestSetProfessionPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player != null) {
                Profession prof = Profession.fromId(payload.professionId());
                ProfessionManager.setProfession(player.getUUID(), prof);
                syncPlayerState(player);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestUnlockPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            ShopItem item = ShopTable.findById(payload.itemId());
            if (player != null && item != null) {
                double costDollars = ShopSettings.calculateUnlockPrice(item.basePrice());
                long costCents = Math.round(costDollars * 100.0);
                if (EconomyManager.withdraw(player.getUUID(), costCents)) {
                    PlayerUnlockManager.unlock(player.getUUID(), item.id());
                    syncPlayerState(player);
                }
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestBuyPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            ShopItem shopItem = ShopTable.findById(payload.itemId());
            if (player != null && shopItem != null) {
                if (PlayerUnlockManager.isUnlocked(player.getUUID(), shopItem.id())) {
                    double costDollars = ShopSettings.calculateBuyPrice(shopItem.basePrice());
                    long costCents = Math.round(costDollars * 100.0);
                    if (EconomyManager.withdraw(player.getUUID(), costCents)) {
                        Item mcItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", shopItem.id()));
                        if (mcItem != null) {
                            player.getInventory().add(new ItemStack(mcItem, 1));
                        }
                        syncPlayerState(player);
                    }
                }
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestSellPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            ShopItem shopItem = ShopTable.findById(payload.itemId());
            if (player != null && shopItem != null) {
                Item mcItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", shopItem.id()));
                if (mcItem != null) {
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack stack = player.getInventory().getItem(i);
                        if (!stack.isEmpty() && stack.is(mcItem)) {
                            stack.shrink(1);

                            double basePayoutDollars = ShopSettings.calculateSellPrice(shopItem.basePrice());
                            double bonusRatio = ProfessionManager.getProfessionSellBonusRatio(player.getUUID(), shopItem.id());
                            double finalPayoutDollars = basePayoutDollars * bonusRatio;

                            long payoutCents = Math.max(1L, Math.round(finalPayoutDollars * 100.0));

                            long xpAmount = Math.max(1L, Math.round(shopItem.basePrice() * 100.0));
                            ProfessionManager.addXp(player.getUUID(), xpAmount);

                            EconomyManager.deposit(player.getUUID(), payoutCents);
                            syncPlayerState(player);
                            break;
                        }
                    }
                }
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestUpdateSettingsPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player != null) {
                ShopSettings.setSellMultiplier(payload.sellMultiplier());
                ShopSettings.setBuyMultiplier(payload.buyMultiplier());
                ShopSettings.setUnlockMultiplier(payload.unlockMultiplier());

                for (ServerPlayer onlinePlayer : context.server().getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(onlinePlayer, new SyncSettingsPayload(
                            ShopSettings.getSellMultiplier(),
                            ShopSettings.getBuyMultiplier(),
                            ShopSettings.getUnlockMultiplier()
                    ));
                }
            }
        });

        // 7. Continuous Inventory Scanner (Every 10 ticks / 0.5 seconds)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter % 10 == 0) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    if (scanAndUnlockInventory(player)) {
                        syncPlayerState(player);
                    }
                }
            }
        });

        AdvancedEconomicsCommon.LOGGER.info("Advanced Economics Fabric fully initialized.");
    }

    private static boolean isOperator(CommandSourceStack src) {
        if (!src.isPlayer()) return true;
        try {
            ServerPlayer player = src.getPlayer();
            if (player != null) {
                return src.getServer().getPlayerList().isOp(player.nameAndId());
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("ae")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("§a[Advanced Economics] §fPress 'N' to open the Economy Box menu, or use §e/ae help§f for commands."), false);
                    return 1;
                })
                .then(Commands.literal("help").executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("§e--- Advanced Economics Commands ---\n" +
                            "§a/ae send <player> <amount> §7- Send money to another player\n" +
                            "§a/ae give <amount> [player] §7- (Admin) Give money to a player\n" +
                            "§a/ae take <amount> <player> §7- (Admin) Remove money from a player\n" +
                            "§a/ae setmoney <player> <amount> §7- (Admin) Set a player's balance\n" +
                            "§a/ae setlevel <player> <level> §7- (Admin) Set profession level\n" +
                            "§a/ae addxp <player> <amount> §7- (Admin) Grant profession XP"), false);
                    return 1;
                }))
                // /ae send <player> <amount>
                .then(Commands.literal("send")
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(context -> {
                                ServerPlayer sender = context.getSource().getPlayerOrException();
                                ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                double amountDollars = DoubleArgumentType.getDouble(context, "amount");
                                long cents = Math.round(amountDollars * 100.0);

                                if (EconomyManager.withdraw(sender.getUUID(), cents)) {
                                    EconomyManager.deposit(target.getUUID(), cents);
                                    syncPlayerState(sender);
                                    syncPlayerState(target);

                                    String formatted = String.format(Locale.US, "$%.2f", amountDollars);
                                    sender.sendSystemMessage(Component.literal("§a[AE] Successfully sent " + formatted + " to " + target.getScoreboardName()));
                                    target.sendSystemMessage(Component.literal("§a[AE] Received " + formatted + " from " + sender.getScoreboardName()));
                                    return 1;
                                } else {
                                    sender.sendSystemMessage(Component.literal("§c[AE] Insufficient funds!"));
                                    return 0;
                                }
                            }))))
                // /ae give <amount> [player] (OP command)
                .then(Commands.literal("give").requires(AdvancedEconomicsFabric::isOperator)
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(context -> {
                            ServerPlayer target = context.getSource().getPlayerOrException();
                            double amountDollars = DoubleArgumentType.getDouble(context, "amount");
                            long cents = Math.round(amountDollars * 100.0);

                            EconomyManager.deposit(target.getUUID(), cents);
                            syncPlayerState(target);

                            String formatted = String.format(Locale.US, "$%.2f", amountDollars);
                            context.getSource().sendSuccess(() -> Component.literal("§a[AE] Gave " + formatted + " to " + target.getScoreboardName()), true);
                            return 1;
                        })
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(context -> {
                                ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                double amountDollars = DoubleArgumentType.getDouble(context, "amount");
                                long cents = Math.round(amountDollars * 100.0);

                                EconomyManager.deposit(target.getUUID(), cents);
                                syncPlayerState(target);

                                String formatted = String.format(Locale.US, "$%.2f", amountDollars);
                                context.getSource().sendSuccess(() -> Component.literal("§a[AE] Gave " + formatted + " to " + target.getScoreboardName()), true);
                                return 1;
                            }))))
                // /ae take <amount> <player> (OP command)
                .then(Commands.literal("take").requires(AdvancedEconomicsFabric::isOperator)
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(context -> {
                                ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                double amountDollars = DoubleArgumentType.getDouble(context, "amount");
                                long cents = Math.round(amountDollars * 100.0);

                                EconomyManager.withdraw(target.getUUID(), cents);
                                syncPlayerState(target);

                                String formatted = String.format(Locale.US, "$%.2f", amountDollars);
                                context.getSource().sendSuccess(() -> Component.literal("§a[AE] Took " + formatted + " from " + target.getScoreboardName()), true);
                                return 1;
                            }))))
                // /ae setmoney <player> <amount> (OP command)
                .then(Commands.literal("setmoney").requires(AdvancedEconomicsFabric::isOperator)
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                            .executes(context -> {
                                ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                double amountDollars = DoubleArgumentType.getDouble(context, "amount");
                                long cents = Math.round(amountDollars * 100.0);

                                EconomyManager.setBalance(target.getUUID(), cents);
                                syncPlayerState(target);

                                String formatted = String.format(Locale.US, "$%.2f", amountDollars);
                                context.getSource().sendSuccess(() -> Component.literal("§a[AE] Set " + target.getScoreboardName() + "'s balance to " + formatted), true);
                                return 1;
                            }))))
                // /ae setlevel <player> <level> (OP command)
                .then(Commands.literal("setlevel").requires(AdvancedEconomicsFabric::isOperator)
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("level", IntegerArgumentType.integer(1))
                            .executes(context -> {
                                ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                int level = IntegerArgumentType.getInteger(context, "level");

                                ProfessionManager.PlayerProfessionState state = ProfessionManager.getPlayerState(target.getUUID());
                                state.level = level;
                                state.xp = 0;

                                syncPlayerState(target);
                                context.getSource().sendSuccess(() -> Component.literal("§a[AE] Set " + target.getScoreboardName() + "'s level to " + level), true);
                                return 1;
                            }))))
                // /ae addxp <player> <amount> (OP command)
                .then(Commands.literal("addxp").requires(AdvancedEconomicsFabric::isOperator)
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("xp", IntegerArgumentType.integer(1))
                            .executes(context -> {
                                ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                int xp = IntegerArgumentType.getInteger(context, "xp");

                                ProfessionManager.addXp(target.getUUID(), xp);
                                syncPlayerState(target);

                                context.getSource().sendSuccess(() -> Component.literal("§a[AE] Added " + xp + " XP to " + target.getScoreboardName()), true);
                                return 1;
                            }))))
            );
        });
    }

    private boolean scanAndUnlockInventory(ServerPlayer player) {
        if (player == null) return false;
        boolean newlyUnlocked = false;

        for (ShopItem shopItem : ShopTable.getItems()) {
            if (!PlayerUnlockManager.isUnlocked(player.getUUID(), shopItem.id())) {
                Item mcItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", shopItem.id()));
                if (mcItem != null && mcItem != Items.AIR) {
                    if (player.getInventory().contains(new ItemStack(mcItem))) {
                        PlayerUnlockManager.unlock(player.getUUID(), shopItem.id());
                        newlyUnlocked = true;
                    }
                }
            }
        }
        return newlyUnlocked;
    }

    public static void syncPlayerState(ServerPlayer player) {
        if (player == null) return;
        long balance = EconomyManager.getBalance(player.getUUID());

        ProfessionManager.PlayerProfessionState profState = ProfessionManager.getPlayerState(player.getUUID());
        Profession prof = Profession.fromId(profState.profession);

        Set<String> unlocks = PlayerUnlockManager.getUnlockedItems(player.getUUID());
        String unlocksStr = String.join(",", unlocks);

        ServerPlayNetworking.send(player, new SyncBalancePayload(balance));
        ServerPlayNetworking.send(player, new SyncProfessionPayload(
                prof.getDisplayName(),
                profState.level,
                profState.xp,
                profState.getMaxXp()
        ));
        ServerPlayNetworking.send(player, new SyncSettingsPayload(
                ShopSettings.getSellMultiplier(),
                ShopSettings.getBuyMultiplier(),
                ShopSettings.getUnlockMultiplier()
        ));
        ServerPlayNetworking.send(player, new SyncUnlocksPayload(unlocksStr));
    }
}
