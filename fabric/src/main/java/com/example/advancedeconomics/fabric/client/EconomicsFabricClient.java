package com.example.advancedeconomics.fabric.client;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import com.example.advancedeconomics.fabric.network.SyncBalancePayload;
import com.example.advancedeconomics.fabric.network.SyncProfessionPayload;
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

/**
 * Fabric Client Mod Initializer for Advanced Economics (v0.10).
 *
 * Top row above inventory header (Right to Left):
 * [Profession Box: None]  [$ Balance]  [Economics]
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
            AdvancedEconomicsCommon.LOGGER.info("Advanced Economics Fabric Client v0.10");
            AdvancedEconomicsCommon.LOGGER.info("=============================================");

            // 1. Register Client Receivers
            ClientPlayNetworking.registerGlobalReceiver(SyncBalancePayload.TYPE, (payload, context) -> {
                long balance = payload.balance();
                ClientEconomyState.setBalance(balance);
                AdvancedEconomicsCommon.LOGGER.info("[AE Client] Synced balance: ${}", balance);
            });

            ClientPlayNetworking.registerGlobalReceiver(SyncProfessionPayload.TYPE, (payload, context) -> {
                String profession = payload.profession();
                ClientEconomyState.setProfession(profession);
                AdvancedEconomicsCommon.LOGGER.info("[AE Client] Synced profession: {}", profession);
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
                            client.gui.setScreen(new EconomicsBoxScreen());
                        }
                    }
                } catch (Throwable t) {
                    AdvancedEconomicsCommon.LOGGER.error("[AE] Key tick error: {}", t.getMessage(), t);
                }
            });

            // 3. Inject Inventory Header Widgets: [Profession] [$ Balance] [Economics]
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
                int ecoWidth   = 60;
                int balWidth   = 50;
                int profWidth  = 58;
                int gap        = 2;
                int rowY       = topPos - btnHeight - 2;

                // [Economics] button on far right edge
                int ecoX = leftPos + imageWidth - ecoWidth;

                // [$ Balance] box immediately to the left of [Economics]
                int balX = ecoX - gap - balWidth;

                // [Profession] box immediately to the left of [$ Balance]
                int profX = balX - gap - profWidth;

                long currentBalance = ClientEconomyState.getBalance();
                String profession = ClientEconomyState.getProfession();

                // Profession Box
                Screens.getWidgets(screen).add(
                        Button.builder(Component.literal(profession), btn -> {})
                                .bounds(profX, rowY, profWidth, btnHeight)
                                .build()
                );

                // Balance Display Box
                Screens.getWidgets(screen).add(
                        Button.builder(Component.literal("$ " + currentBalance), btn -> {})
                                .bounds(balX, rowY, balWidth, btnHeight)
                                .build()
                );

                // Economics Screen Button
                Screens.getWidgets(screen).add(
                        Button.builder(Component.literal("Economics"), btn ->
                                client.gui.setScreen(new EconomicsBoxScreen())
                        ).bounds(ecoX, rowY, ecoWidth, btnHeight).build()
                );
            });

            AdvancedEconomicsCommon.LOGGER.info("Ready. Inventory header widgets active.");

        } catch (Throwable t) {
            AdvancedEconomicsCommon.LOGGER.error("[AE] Client init failed: {}", t.getMessage(), t);
        }
    }
}
