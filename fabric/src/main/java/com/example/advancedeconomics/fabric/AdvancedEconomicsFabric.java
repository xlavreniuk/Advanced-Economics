package com.example.advancedeconomics.fabric;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import com.example.advancedeconomics.economy.EconomyManager;
import com.example.advancedeconomics.fabric.network.SyncBalancePayload;
import com.example.advancedeconomics.fabric.network.SyncProfessionPayload;
import com.example.advancedeconomics.profession.ProfessionManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;

/**
 * Main Fabric Initializer (Server & Common lifecycle v1.0).
 */
public class AdvancedEconomicsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        AdvancedEconomicsCommon.init();

        // 1. Register Clientbound payloads
        PayloadTypeRegistry.clientboundPlay().register(SyncBalancePayload.TYPE, SyncBalancePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncProfessionPayload.TYPE, SyncProfessionPayload.CODEC);

        // 2. Load data on Server Start
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
            File aeDataDir = new File(worldDir, "data/advanced_economics");
            EconomyManager.load(aeDataDir);
            ProfessionManager.load(aeDataDir);
        });

        // 3. Save data on Server Save & Stop
        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> {
            File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
            File aeDataDir = new File(worldDir, "data/advanced_economics");
            EconomyManager.save(aeDataDir);
            ProfessionManager.save(aeDataDir);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
            File aeDataDir = new File(worldDir, "data/advanced_economics");
            EconomyManager.save(aeDataDir);
            ProfessionManager.save(aeDataDir);
        });

        // 4. Sync data to player upon join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            syncPlayerBalance(player);
            syncPlayerProfession(player);
        });

        AdvancedEconomicsCommon.LOGGER.info("Advanced Economics Fabric Server-Authoritative Economy & Profession initialized.");
    }

    public static void syncPlayerBalance(ServerPlayer player) {
        if (player == null) return;
        long balance = EconomyManager.getBalance(player.getUUID());
        ServerPlayNetworking.send(player, new SyncBalancePayload(balance));
    }

    public static void syncPlayerProfession(ServerPlayer player) {
        if (player == null) return;
        String profession = ProfessionManager.getProfession(player.getUUID()).getDisplayName();
        ServerPlayNetworking.send(player, new SyncProfessionPayload(profession));
    }
}
