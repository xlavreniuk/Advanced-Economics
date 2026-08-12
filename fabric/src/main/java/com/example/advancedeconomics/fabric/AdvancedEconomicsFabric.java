package com.example.advancedeconomics.fabric;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import com.example.advancedeconomics.economy.EconomyManager;
import com.example.advancedeconomics.fabric.network.*;
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

import java.io.File;
import java.util.Set;

/**
 * Main Fabric Initializer (v0.10).
 * Handles server-authoritative logic:
 * - Economy, Profession, Shop Settings & Player Unlocks
 * - Automatic inventory ownership item unlocks
 * - Secure transaction packet handling (Buy, Sell, Unlock, Settings update)
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

        // 3. World Lifecycle Events (Load & Save)
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

        // 4. Sync on Player Join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            syncPlayerState(player);
        });

        // 5. Serverbound Packet Handlers
        ServerPlayNetworking.registerGlobalReceiver(RequestUnlockPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            ShopItem item = ShopTable.findById(payload.itemId());
            if (player != null && item != null) {
                long cost = ShopSettings.calculateUnlockPrice(item.basePrice());
                if (EconomyManager.withdraw(player.getUUID(), cost)) {
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
                    long cost = ShopSettings.calculateBuyPrice(shopItem.basePrice());
                    if (EconomyManager.withdraw(player.getUUID(), cost)) {
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
                if (mcItem != null && player.getInventory().contains(new ItemStack(mcItem))) {
                    // Remove 1 item from player inventory
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack stack = player.getInventory().getItem(i);
                        if (!stack.isEmpty() && stack.is(mcItem)) {
                            stack.shrink(1);
                            long payout = ShopSettings.calculateSellPrice(shopItem.basePrice());
                            EconomyManager.deposit(player.getUUID(), payout);
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

                // Sync new multipliers to all online players
                for (ServerPlayer onlinePlayer : context.server().getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(onlinePlayer, new SyncSettingsPayload(
                            ShopSettings.getSellMultiplier(),
                            ShopSettings.getBuyMultiplier(),
                            ShopSettings.getUnlockMultiplier()
                    ));
                }
            }
        });

        // 6. Automatic Ownership Inventory Scanner (Unlocks items player has ever owned)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter % 20 == 0) { // Every 1 second
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    boolean newlyUnlocked = false;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack stack = player.getInventory().getItem(i);
                        if (!stack.isEmpty()) {
                            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                            if (id != null) {
                                String itemId = id.getPath();
                                if (ShopTable.findById(itemId) != null && !PlayerUnlockManager.isUnlocked(player.getUUID(), itemId)) {
                                    PlayerUnlockManager.unlock(player.getUUID(), itemId);
                                    newlyUnlocked = true;
                                }
                            }
                        }
                    }
                    if (newlyUnlocked) {
                        syncPlayerState(player);
                    }
                }
            }
        });

        AdvancedEconomicsCommon.LOGGER.info("Advanced Economics Fabric fully initialized.");
    }

    public static void syncPlayerState(ServerPlayer player) {
        if (player == null) return;
        long balance = EconomyManager.getBalance(player.getUUID());
        String profession = ProfessionManager.getProfession(player.getUUID()).getDisplayName();

        Set<String> unlocks = PlayerUnlockManager.getUnlockedItems(player.getUUID());
        String unlocksStr = String.join(",", unlocks);

        ServerPlayNetworking.send(player, new SyncBalancePayload(balance));
        ServerPlayNetworking.send(player, new SyncProfessionPayload(profession));
        ServerPlayNetworking.send(player, new SyncSettingsPayload(
                ShopSettings.getSellMultiplier(),
                ShopSettings.getBuyMultiplier(),
                ShopSettings.getUnlockMultiplier()
        ));
        ServerPlayNetworking.send(player, new SyncUnlocksPayload(unlocksStr));
    }
}
