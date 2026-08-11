package com.example.advancedeconomics.fabric.client;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import com.example.advancedeconomics.ui.EconomicsBoxScreen;
import net.fabricmc.api.ClientModInitializer;

public class EconomicsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        try {
            AdvancedEconomicsCommon.LOGGER.info("=============================================");
            AdvancedEconomicsCommon.LOGGER.info("Advanced Economics Fabric Client Initialized (v0.3)");
            AdvancedEconomicsCommon.LOGGER.info("Registered Keybinding: 'N' (GLFW_KEY_N = 78)");
            AdvancedEconomicsCommon.LOGGER.info("Live Reload Mode: ACTIVE");
            AdvancedEconomicsCommon.LOGGER.info("=============================================");

            // Display initial UI Box HUD state
            EconomicsBoxScreen.toggle();
            AdvancedEconomicsCommon.LOGGER.info("\n{}", EconomicsBoxScreen.renderBoxHUD());
        } catch (Throwable t) {
            AdvancedEconomicsCommon.LOGGER.error("[Advanced Economics Error Handler] Client initialization error caught cleanly: {}", t.getMessage(), t);
        }
    }

    public static void onNKeyPressed() {
        try {
            boolean state = EconomicsBoxScreen.toggle();
            if (state) {
                AdvancedEconomicsCommon.LOGGER.info("\n{}", EconomicsBoxScreen.renderBoxHUD());
            }
        } catch (Throwable t) {
            AdvancedEconomicsCommon.LOGGER.error("[Advanced Economics Error Handler] Keypress event error caught: {}", t.getMessage(), t);
        }
    }
}
