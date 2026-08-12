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
import com.mojang.brigadier.arguments.StringArgumentType;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

import java.io.File;
import java.util.Locale;
import java.util.Set;

/**
 * Main Fabric Initializer (v0.41).
 * Indexes 100% of all vanilla Minecraft items (1,400+) into the shop table!
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

        // 4. Index ALL 1,400+ Minecraft Items into ShopTable!
        registerAllMinecraftItems();

        // 5. World Lifecycle Events
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

        // 6. Sync & Auto-unlock inventory items on Player Join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            scanAndUnlockInventory(player);
            syncPlayerState(player);
        });

        // 7. Serverbound Packet Handlers
        ServerPlayNetworking.registerGlobalReceiver(RequestSetProfessionPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player != null && ShopSettings.isEnableProfessions()) {
                Profession prof = Profession.fromId(payload.professionId());
                ProfessionManager.setProfession(player.getUUID(), prof);
                syncPlayerState(player);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestUnlockPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            ShopItem item = ShopTable.findById(payload.itemId());
            if (player != null && item != null && ShopSettings.isAllowUnlocking()) {
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
                executeBuyItem(player, shopItem, 1);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestSellPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            ShopItem shopItem = ShopTable.findById(payload.itemId());
            if (player != null && shopItem != null) {
                executeSellItem(player, shopItem, 1);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestUpdateSettingsPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player != null) {
                ShopSettings.setSellMultiplier(payload.sellMultiplier());
                ShopSettings.setBuyMultiplier(payload.buyMultiplier());
                ShopSettings.setUnlockMultiplier(payload.unlockMultiplier());

                ShopSettings.setAllowSelling(payload.allowSelling());
                ShopSettings.setAllowBuying(payload.allowBuying());
                ShopSettings.setAllowUnlocking(payload.allowUnlocking());
                ShopSettings.setEnableProfessions(payload.enableProfessions());
                ShopSettings.setEnableXpLeveling(payload.enableXpLeveling());

                for (ServerPlayer onlinePlayer : context.server().getPlayerList().getPlayers()) {
                    syncPlayerState(onlinePlayer);
                }
            }
        });

        // 8. Continuous Inventory Scanner (Every 10 ticks / 0.5 seconds)
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

        AdvancedEconomicsCommon.LOGGER.info("Advanced Economics Fabric fully initialized with {} shop items.", ShopTable.getItems().size());
    }

    private static void registerAllMinecraftItems() {
        try {
            for (Identifier key : BuiltInRegistries.ITEM.keySet()) {
                String path = key.getPath();
                if (ShopTable.isProcessed(path)) continue;

                Item item = BuiltInRegistries.ITEM.getValue(key);
                if (item == null || item == Items.AIR) continue;

                double price = calculateDynamicPrice(item, path);
                String displayName = formatDisplayName(path);

                ShopTable.registerDynamicItem(path, displayName, price);
            }
        } catch (Throwable t) {
            AdvancedEconomicsCommon.LOGGER.error("[AE] Failed to scan Minecraft items: {}", t.getMessage());
        }
    }

    private static double calculateDynamicPrice(Item item, String path) {
        try {
            Rarity rarity = item.components().getOrDefault(DataComponents.RARITY, Rarity.COMMON);
            if (rarity == Rarity.EPIC) return 50.0;
            if (rarity == Rarity.RARE) return 10.0;
            if (rarity == Rarity.UNCOMMON) return 2.0;

            int maxDamage = item.components().getOrDefault(DataComponents.MAX_DAMAGE, 0);
            if (maxDamage > 0) {
                return Math.max(0.10, Math.round(maxDamage * 0.003 * 100.0) / 100.0);
            }

            FoodProperties food = item.components().get(DataComponents.FOOD);
            if (food != null) {
                double val = (food.nutrition() * 0.02) + (food.saturation() * 0.03);
                return Math.max(0.02, Math.round(val * 100.0) / 100.0);
            }
        } catch (Throwable ignored) {}

        if (path.contains("spawn_egg")) return 15.0;
        if (path.contains("music_disc")) return 12.0;
        if (path.contains("pattern") || path.contains("template")) return 8.0;
        if (path.contains("slab") || path.contains("stairs") || path.contains("wall")) return 0.01;
        if (path.contains("banner") || path.contains("carpet") || path.contains("candle")) return 0.03;
        if (path.contains("dye") || path.contains("flower") || path.contains("sapling")) return 0.02;

        return 0.05;
    }

    private static String formatDisplayName(String id) {
        String[] words = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                  .append(w.substring(1))
                  .append(" ");
            }
        }
        return sb.toString().trim();
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
                    context.getSource().sendSuccess(() -> Component.literal("§a[Advanced Economics] §fPress 'N' to open UI menu, or use §e/ae help§f."), false);
                    return 1;
                })
                .then(Commands.literal("help").executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("§e--- Advanced Economics Unified Commands ---\n" +
                            "§a/ae send <amount> <player> §7- Transfer money to player\n" +
                            "§a/ae buy <item> [quantity] §7- Buy item(s) from shop\n" +
                            "§a/ae sell [item] [quantity] §7- Sell item(s) from inventory\n" +
                            "§a/ae unlock <item> §7- Unlock item in shop\n" +
                            "§a/ae give <amount> [player] §7- (Admin) Give money (default self)\n" +
                            "§a/ae take <amount> [player] §7- (Admin) Take money (default self)\n" +
                            "§a/ae setmoney <amount> [player] §7- (Admin) Set balance (default self)\n" +
                            "§a/ae setlevel <level> [player] §7- (Admin) Set level (default self)\n" +
                            "§a/ae addxp <amount> [player] §7- (Admin) Add XP (default self)"), false);
                    return 1;
                }))

                // 1. /ae send <amount> <player>
                .then(Commands.literal("send")
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> {
                                ServerPlayer sender = context.getSource().getPlayerOrException();
                                ServerPlayer target = EntityArgument.getPlayer(context, "player");
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

                // 2. /ae give <amount> [player]
                .then(Commands.literal("give").requires(AdvancedEconomicsFabric::isOperator)
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(context -> executeGive(context.getSource(), DoubleArgumentType.getDouble(context, "amount"), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> executeGive(context.getSource(), DoubleArgumentType.getDouble(context, "amount"), EntityArgument.getPlayer(context, "player"))))))

                // 3. /ae take <amount> [player]
                .then(Commands.literal("take").requires(AdvancedEconomicsFabric::isOperator)
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(context -> executeTake(context.getSource(), DoubleArgumentType.getDouble(context, "amount"), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> executeTake(context.getSource(), DoubleArgumentType.getDouble(context, "amount"), EntityArgument.getPlayer(context, "player"))))))

                // 4. /ae setmoney <amount> [player]
                .then(Commands.literal("setmoney").requires(AdvancedEconomicsFabric::isOperator)
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                        .executes(context -> executeSetMoney(context.getSource(), DoubleArgumentType.getDouble(context, "amount"), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> executeSetMoney(context.getSource(), DoubleArgumentType.getDouble(context, "amount"), EntityArgument.getPlayer(context, "player"))))))

                // 5. /ae setlevel <level> [player]
                .then(Commands.literal("setlevel").requires(AdvancedEconomicsFabric::isOperator)
                    .then(Commands.argument("level", IntegerArgumentType.integer(1))
                        .executes(context -> executeSetLevel(context.getSource(), IntegerArgumentType.getInteger(context, "level"), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> executeSetLevel(context.getSource(), IntegerArgumentType.getInteger(context, "level"), EntityArgument.getPlayer(context, "player"))))))

                // 6. /ae addxp <amount> [player]
                .then(Commands.literal("addxp").requires(AdvancedEconomicsFabric::isOperator)
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(context -> executeAddXp(context.getSource(), IntegerArgumentType.getInteger(context, "amount"), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> executeAddXp(context.getSource(), IntegerArgumentType.getInteger(context, "amount"), EntityArgument.getPlayer(context, "player"))))))

                // 7. /ae buy <item> [quantity]
                .then(Commands.literal("buy")
                    .then(Commands.argument("item", StringArgumentType.word())
                        .executes(context -> executeBuyCmd(context.getSource(), StringArgumentType.getString(context, "item"), 1))
                        .then(Commands.argument("quantity", IntegerArgumentType.integer(1, 640))
                            .executes(context -> executeBuyCmd(context.getSource(), StringArgumentType.getString(context, "item"), IntegerArgumentType.getInteger(context, "quantity"))))))

                // 8. /ae sell [item] [quantity]
                .then(Commands.literal("sell")
                    .executes(context -> executeSellHeldCmd(context.getSource(), 1))
                    .then(Commands.argument("item", StringArgumentType.word())
                        .executes(context -> executeSellCmd(context.getSource(), StringArgumentType.getString(context, "item"), 1))
                        .then(Commands.argument("quantity", IntegerArgumentType.integer(1, 640))
                            .executes(context -> executeSellCmd(context.getSource(), StringArgumentType.getString(context, "item"), IntegerArgumentType.getInteger(context, "quantity"))))))

                // 9. /ae unlock <item>
                .then(Commands.literal("unlock")
                    .then(Commands.argument("item", StringArgumentType.word())
                        .executes(context -> executeUnlockCmd(context.getSource(), StringArgumentType.getString(context, "item")))))
            );
        });
    }

    private static int executeGive(CommandSourceStack source, double amountDollars, ServerPlayer target) {
        long cents = Math.round(amountDollars * 100.0);
        EconomyManager.deposit(target.getUUID(), cents);
        syncPlayerState(target);
        String formatted = String.format(Locale.US, "$%.2f", amountDollars);
        source.sendSuccess(() -> Component.literal("§a[AE] Gave " + formatted + " to " + target.getScoreboardName()), true);
        return 1;
    }

    private static int executeTake(CommandSourceStack source, double amountDollars, ServerPlayer target) {
        long cents = Math.round(amountDollars * 100.0);
        EconomyManager.withdraw(target.getUUID(), cents);
        syncPlayerState(target);
        String formatted = String.format(Locale.US, "$%.2f", amountDollars);
        source.sendSuccess(() -> Component.literal("§a[AE] Took " + formatted + " from " + target.getScoreboardName()), true);
        return 1;
    }

    private static int executeSetMoney(CommandSourceStack source, double amountDollars, ServerPlayer target) {
        long cents = Math.round(amountDollars * 100.0);
        EconomyManager.setBalance(target.getUUID(), cents);
        syncPlayerState(target);
        String formatted = String.format(Locale.US, "$%.2f", amountDollars);
        source.sendSuccess(() -> Component.literal("§a[AE] Set " + target.getScoreboardName() + "'s balance to " + formatted), true);
        return 1;
    }

    private static int executeSetLevel(CommandSourceStack source, int level, ServerPlayer target) {
        ProfessionManager.PlayerProfessionState state = ProfessionManager.getPlayerState(target.getUUID());
        state.level = level;
        state.xp = 0;
        syncPlayerState(target);
        source.sendSuccess(() -> Component.literal("§a[AE] Set " + target.getScoreboardName() + "'s profession level to " + level), true);
        return 1;
    }

    private static int executeAddXp(CommandSourceStack source, int xp, ServerPlayer target) {
        ProfessionManager.addXp(target.getUUID(), xp);
        syncPlayerState(target);
        source.sendSuccess(() -> Component.literal("§a[AE] Added " + xp + " XP to " + target.getScoreboardName()), true);
        return 1;
    }

    private static int executeBuyCmd(CommandSourceStack source, String itemId, int quantity) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ShopItem shopItem = ShopTable.findById(itemId);
            if (shopItem == null) {
                source.sendFailure(Component.literal("§c[AE] Unknown item: " + itemId));
                return 0;
            }
            if (!PlayerUnlockManager.isUnlocked(player.getUUID(), shopItem.id())) {
                source.sendFailure(Component.literal("§c[AE] Item " + shopItem.displayName() + " is locked!"));
                return 0;
            }
            return executeBuyItem(player, shopItem, quantity);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int executeBuyItem(ServerPlayer player, ShopItem shopItem, int quantity) {
        if (!ShopSettings.isAllowBuying()) {
            player.sendSystemMessage(Component.literal("§c[AE] Buying items is currently disabled in settings!"));
            return 0;
        }

        double totalCostDollars = ShopSettings.calculateBuyPrice(shopItem.basePrice()) * quantity;
        long totalCostCents = Math.round(totalCostDollars * 100.0);

        if (EconomyManager.withdraw(player.getUUID(), totalCostCents)) {
            Item mcItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", shopItem.id()));
            if (mcItem != null && mcItem != Items.AIR) {
                player.getInventory().add(new ItemStack(mcItem, quantity));
            }
            syncPlayerState(player);
            String formatted = String.format(Locale.US, "$%.2f", totalCostDollars);
            player.sendSystemMessage(Component.literal("§a[AE] Purchased " + quantity + "x " + shopItem.displayName() + " for " + formatted));
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§c[AE] Insufficient funds!"));
            return 0;
        }
    }

    private static int executeSellCmd(CommandSourceStack source, String itemId, int quantity) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ShopItem shopItem = ShopTable.findById(itemId);
            if (shopItem == null) {
                source.sendFailure(Component.literal("§c[AE] Unknown item: " + itemId));
                return 0;
            }
            return executeSellItem(player, shopItem, quantity);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int executeSellHeldCmd(CommandSourceStack source, int quantity) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.isEmpty()) {
                source.sendFailure(Component.literal("§c[AE] You are not holding any item to sell!"));
                return 0;
            }
            Identifier id = BuiltInRegistries.ITEM.getKey(mainHand.getItem());
            ShopItem shopItem = ShopTable.findById(id.getPath());
            if (shopItem == null) {
                source.sendFailure(Component.literal("§c[AE] Item " + mainHand.getHoverName().getString() + " cannot be sold!"));
                return 0;
            }
            return executeSellItem(player, shopItem, quantity);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int executeSellItem(ServerPlayer player, ShopItem shopItem, int quantity) {
        if (!ShopSettings.isAllowSelling()) {
            player.sendSystemMessage(Component.literal("§c[AE] Selling items is currently disabled in settings!"));
            return 0;
        }

        Item mcItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", shopItem.id()));
        if (mcItem == null || mcItem == Items.AIR) return 0;

        int remainingToSell = quantity;
        int countSold = 0;

        for (int i = 0; i < player.getInventory().getContainerSize() && remainingToSell > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(mcItem)) {
                int take = Math.min(stack.getCount(), remainingToSell);
                stack.shrink(take);
                remainingToSell -= take;
                countSold += take;
            }
        }

        if (countSold > 0) {
            double basePayoutDollars = ShopSettings.calculateSellPrice(shopItem.basePrice()) * countSold;
            double bonusRatio = ShopSettings.isEnableProfessions() ? ProfessionManager.getProfessionSellBonusRatio(player.getUUID(), shopItem.id()) : 1.0;
            double finalPayoutDollars = basePayoutDollars * bonusRatio;

            long payoutCents = Math.max(1L, Math.round(finalPayoutDollars * 100.0));

            if (ShopSettings.isEnableXpLeveling()) {
                long xpAmount = Math.max(1L, Math.round(shopItem.basePrice() * 100.0 * countSold));
                ProfessionManager.addXp(player.getUUID(), xpAmount);
            }

            EconomyManager.deposit(player.getUUID(), payoutCents);
            syncPlayerState(player);

            String formatted = String.format(Locale.US, "$%.2f", finalPayoutDollars);
            player.sendSystemMessage(Component.literal("§a[AE] Sold " + countSold + "x " + shopItem.displayName() + " for " + formatted));
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§c[AE] You don't have " + shopItem.displayName() + " in your inventory to sell!"));
            return 0;
        }
    }

    private static int executeUnlockCmd(CommandSourceStack source, String itemId) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            if (!ShopSettings.isAllowUnlocking()) {
                player.sendSystemMessage(Component.literal("§c[AE] Unlocking items is currently disabled in settings!"));
                return 0;
            }

            ShopItem shopItem = ShopTable.findById(itemId);
            if (shopItem == null) {
                source.sendFailure(Component.literal("§c[AE] Unknown item: " + itemId));
                return 0;
            }
            if (PlayerUnlockManager.isUnlocked(player.getUUID(), shopItem.id())) {
                source.sendSystemMessage(Component.literal("§a[AE] Item " + shopItem.displayName() + " is already unlocked!"));
                return 1;
            }

            double costDollars = ShopSettings.calculateUnlockPrice(shopItem.basePrice());
            long costCents = Math.round(costDollars * 100.0);

            if (EconomyManager.withdraw(player.getUUID(), costCents)) {
                PlayerUnlockManager.unlock(player.getUUID(), shopItem.id());
                syncPlayerState(player);

                String formatted = String.format(Locale.US, "$%.2f", costDollars);
                player.sendSystemMessage(Component.literal("§a[AE] Unlocked " + shopItem.displayName() + " for " + formatted));
                return 1;
            } else {
                player.sendSystemMessage(Component.literal("§c[AE] Insufficient funds to unlock " + shopItem.displayName() + "!"));
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }
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
                ShopSettings.getUnlockMultiplier(),
                ShopSettings.isAllowSelling(),
                ShopSettings.isAllowBuying(),
                ShopSettings.isAllowUnlocking(),
                ShopSettings.isEnableProfessions(),
                ShopSettings.isEnableXpLeveling()
        ));
        ServerPlayNetworking.send(player, new SyncUnlocksPayload(unlocksStr));
    }
}
