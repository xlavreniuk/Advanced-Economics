package com.example.advancedeconomics.fabric.client;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import com.example.advancedeconomics.fabric.network.SyncBalancePayload;
import com.example.advancedeconomics.fabric.network.SyncProfessionPayload;
import com.example.advancedeconomics.fabric.network.SyncSettingsPayload;
import com.example.advancedeconomics.fabric.network.SyncUnlocksPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Fabric Client Mod Initializer for Advanced Economics (v0.25).
 * Dynamically resizes profession inventory button to fit profession name + blue "Lvl X" label.
 */
public class EconomicsFabricClient implements ClientModInitializer {

    private static KeyMapping openBoxKey;

    private static Field fLeftPos;
    private static Field fTopPos;
    private static Field fImageWidth;

    static {
        try {
            fLeftPos    = AbstractContainerScreen.class.getDeclaredField("leftPos");
            fTopPos     = AbstractContainerScreen.class.getDeclaredField("topPos");
            fImageWidth = AbstractContainerScreen.class.getDeclaredField("imageWidth");
            fLeftPos.setAccessible(true);
            fTopPos.setAccessible(true);
            fImageWidth.setAccessible(true);
        } catch (Exception e) {
            AdvancedEconomicsCommon.LOGGER.error("[AE] Failed to cache reflection fields: {}", e.getMessage());
        }
    }

    @Override
    public void onInitializeClient() {
        try {
            AdvancedEconomicsCommon.LOGGER.info("=============================================");
            AdvancedEconomicsCommon.LOGGER.info("Advanced Economics Fabric Client v0.25");
            AdvancedEconomicsCommon.LOGGER.info("=============================================");

            // 1. Register Client Receivers (Thread-safe execution on client thread)
            ClientPlayNetworking.registerGlobalReceiver(SyncBalancePayload.TYPE, (payload, context) -> {
                long balance = payload.balance();
                context.client().execute(() -> {
                    ClientEconomyState.setBalance(balance);
                    refreshCurrentScreen(context.client());
                });
            });

            ClientPlayNetworking.registerGlobalReceiver(SyncProfessionPayload.TYPE, (payload, context) -> {
                String profession = payload.profession();
                int level = payload.level();
                long xp = payload.xp();
                long maxXp = payload.maxXp();
                context.client().execute(() -> {
                    ClientEconomyState.setProfessionData(profession, level, xp, maxXp);
                    refreshCurrentScreen(context.client());
                });
            });

            ClientPlayNetworking.registerGlobalReceiver(SyncSettingsPayload.TYPE, (payload, context) -> {
                context.client().execute(() -> {
                    ClientEconomyState.setMultipliers(payload.sellMultiplier(), payload.buyMultiplier(), payload.unlockMultiplier());
                    refreshCurrentScreen(context.client());
                });
            });

            ClientPlayNetworking.registerGlobalReceiver(SyncUnlocksPayload.TYPE, (payload, context) -> {
                String csv = payload.commaSeparatedUnlocks();
                Set<String> set = new HashSet<>();
                if (csv != null && !csv.isEmpty()) {
                    for (String s : csv.split(",")) {
                        if (!s.isBlank()) set.add(s.trim().toLowerCase());
                    }
                }
                context.client().execute(() -> {
                    ClientEconomyState.setUnlockedItems(set);
                    refreshCurrentScreen(context.client());
                });
            });

            // 2. Register 'N' keybinding
            openBoxKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.advanced_economics.open_box",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_N,
                    KeyMapping.Category.MISC
            ));

            // Tick listener
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                try {
                    if (client.level == null) return;
                    while (openBoxKey.consumeClick()) {
                        if (client.gui.screen() instanceof EconomicsBoxScreen) {
                            client.gui.setScreen(null);
                        } else if (client.gui.screen() == null) {
                            client.gui.setScreen(new EconomicsBoxScreen(EconomicsBoxScreen.Tab.SHOP));
                        }
                    }
                } catch (Throwable t) {
                    AdvancedEconomicsCommon.LOGGER.error("[AE] Key tick error: {}", t.getMessage(), t);
                }
            });

            // 3. Inject Inventory Header Widgets: [Profession] [$ Balance]
            ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
                if (!(screen instanceof InventoryScreen inv)) return;

                int leftPos;
                int topPos;
                int imageWidth;
                try {
                    leftPos    = (int) fLeftPos.get(inv);
                    topPos     = (int) fTopPos.get(inv);
                    imageWidth = (int) fImageWidth.get(inv);
                } catch (Exception e) {
                    leftPos    = (scaledWidth  - 176) / 2;
                    topPos     = (scaledHeight - 166) / 2;
                    imageWidth = 176;
                }

                int btnHeight  = 14;
                int gap        = 3;
                int rowY       = topPos - btnHeight - 2;

                long currentBalance = ClientEconomyState.getBalance();
                String profession = ClientEconomyState.getProfession();
                int level = ClientEconomyState.getLevel();

                // Format Profession label with blue Lvl text (e.g. "Weaponsmith §bLvl 1")
                String profName = profession.equalsIgnoreCase("Unemployed") ? "No Profession" : profession;
                String profLabel = profName + " §bLvl " + level;
                String balLabel = "§a$ " + currentBalance;

                // Dynamically fit text width
                int profTextWidth = client.font.width(profLabel);
                int balTextWidth  = client.font.width(balLabel);

                int profWidth = Math.max(76, profTextWidth + 10);
                int balWidth  = Math.max(46, balTextWidth + 10);

                int balX  = leftPos + imageWidth - balWidth;
                int profX = balX - gap - profWidth;

                // Profession Button
                Screens.getWidgets(screen).add(
                        Button.builder(Component.literal(profLabel), btn ->
                                client.gui.setScreen(new EconomicsBoxScreen(EconomicsBoxScreen.Tab.PROFESSION))
                        ).bounds(profX, rowY, profWidth, btnHeight).build()
                );

                // Money Balance Button
                Screens.getWidgets(screen).add(
                        Button.builder(Component.literal(balLabel), btn ->
                                client.gui.setScreen(new EconomicsBoxScreen(EconomicsBoxScreen.Tab.SHOP))
                        ).bounds(balX, rowY, balWidth, btnHeight).build()
                );
            });

            AdvancedEconomicsCommon.LOGGER.info("Ready. Dynamically sized profession header buttons active.");

        } catch (Throwable t) {
            AdvancedEconomicsCommon.LOGGER.error("[AE] Client init failed: {}", t.getMessage(), t);
        }
    }

    private static void refreshCurrentScreen(net.minecraft.client.Minecraft client) {
        if (client != null && client.gui.screen() instanceof EconomicsBoxScreen screen) {
            screen.refreshUI();
        }
    }
}
