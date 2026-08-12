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
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.File;
import java.util.Set;

/**
 * Main Fabric Initializer (v0.30).
 * Handles double base prices, cents-based economy, and profession leveling.
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

        // 3. World Lifecycle Events
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

        // 4. Sync & Auto-unlock inventory items on Player Join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            scanAndUnlockInventory(player);
            syncPlayerState(player);
        });

        // 5. Serverbound Packet Handlers
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

                            // Grant Profession XP when selling matching items!
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

        // 6. Continuous Inventory Scanner (Every 10 ticks / 0.5 seconds)
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
