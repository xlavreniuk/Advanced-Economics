package com.example.advancedeconomics.fabric.client;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Fabric Client Mod Initializer for Advanced Economics (v0.7).
 * - Registers 'N' keybinding to open the economics screen.
 * - Injects an "Economics" button into the player inventory (top-right).
 */
public class EconomicsFabricClient implements ClientModInitializer {

    private static KeyMapping openBoxKey;

    @Override
    public void onInitializeClient() {
        try {
            AdvancedEconomicsCommon.LOGGER.info("=============================================");
            AdvancedEconomicsCommon.LOGGER.info("Advanced Economics Fabric Client v0.7");
            AdvancedEconomicsCommon.LOGGER.info("=============================================");

            // Register 'N' keybinding
            openBoxKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.advanced_economics.open_box",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_N,
                    KeyMapping.Category.MISC
            ));

            // Tick listener: open/close screen with N key (in-game only)
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                try {
                    if (client.level == null) return;
                    while (openBoxKey.consumeClick()) {
                        if (client.gui.screen() instanceof EconomicsBoxScreen) {
                            client.gui.setScreen(null);
                            AdvancedEconomicsCommon.LOGGER.info("Closed Advanced Economics UI Box.");
                        } else if (client.gui.screen() == null) {
                            client.gui.setScreen(new EconomicsBoxScreen());
                            AdvancedEconomicsCommon.LOGGER.info("Opened Advanced Economics UI Box.");
                        }
                    }
                } catch (Throwable t) {
                    AdvancedEconomicsCommon.LOGGER.error("[AE Error] Key tick handler failed: {}", t.getMessage(), t);
                }
            });

            // Inject "Economics" button into the player inventory screen (top-right)
            ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
                if (!(screen instanceof InventoryScreen inv)) return;

                // Inventory panel is 176×166 px, centred on screen.
                // leftPos/topPos are protected — compute them from screen dimensions.
                int invW  = 176;
                int invH  = 166;
                int leftPos = (scaledWidth  - invW) / 2;
                int topPos  = (scaledHeight - invH) / 2;

                int btnWidth  = 60;
                int btnHeight = 14;
                // Top-right corner of the inventory panel
                int btnX = leftPos + invW - btnWidth;
                int btnY = topPos - btnHeight - 2;

                Screens.getWidgets(screen).add(
                        Button.builder(Component.literal("Economics"), btn -> {
                            client.gui.setScreen(new EconomicsBoxScreen());
                        }).bounds(btnX, btnY, btnWidth, btnHeight).build()
                );
            });

            AdvancedEconomicsCommon.LOGGER.info("Initialization complete. Press 'N' or use inventory button.");

        } catch (Throwable t) {
            AdvancedEconomicsCommon.LOGGER.error("[AE Error] Client init failed: {}", t.getMessage(), t);
        }
    }
}
