package com.example.advancedeconomics.fabric.client;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Fabric Client Mod Initializer for Advanced Economics.
 * Registers 'N' keybinding via Fabric API KeyMappingHelper (MC 26.2 updated API)
 * and opens EconomicsBoxScreen via Minecraft.gui.setScreen().
 */
public class EconomicsFabricClient implements ClientModInitializer {

    private static KeyMapping openBoxKey;

    @Override
    public void onInitializeClient() {
        try {
            AdvancedEconomicsCommon.LOGGER.info("=============================================");
            AdvancedEconomicsCommon.LOGGER.info("Advanced Economics Fabric Client v0.3");
            AdvancedEconomicsCommon.LOGGER.info("Registering 'N' keybinding via KeyMappingHelper...");
            AdvancedEconomicsCommon.LOGGER.info("=============================================");

            // MC 26.2: KeyMappingHelper (not KeyBindingHelper), Category.MISC (not a String)
            openBoxKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.advanced_economics.open_box",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_N,
                    KeyMapping.Category.MISC
            ));

            AdvancedEconomicsCommon.LOGGER.info("'N' keybinding registered successfully.");

            // Listen on each client tick for key presses
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                try {
                    while (openBoxKey.consumeClick()) {
                        // MC 26.2: screen is on client.gui, not client directly
                        if (client.gui.screen() instanceof EconomicsBoxScreen) {
                            // Close if already open
                            client.gui.setScreen(null);
                            AdvancedEconomicsCommon.LOGGER.info("Closed Advanced Economics UI Box.");
                        } else if (client.gui.screen() == null) {
                            // Open the screen
                            client.gui.setScreen(new EconomicsBoxScreen());
                            AdvancedEconomicsCommon.LOGGER.info("Opened Advanced Economics UI Box.");
                        }
                    }
                } catch (Throwable t) {
                    AdvancedEconomicsCommon.LOGGER.error("[AE Error] Key tick handler failed: {}", t.getMessage(), t);
                }
            });

            AdvancedEconomicsCommon.LOGGER.info("ClientTickEvents.END_CLIENT_TICK listener registered.");
            AdvancedEconomicsCommon.LOGGER.info("Ready! Press 'N' in-game to open the UI box.");

        } catch (Throwable t) {
            AdvancedEconomicsCommon.LOGGER.error("[AE Error] Client init failed: {}", t.getMessage(), t);
        }
    }
}
