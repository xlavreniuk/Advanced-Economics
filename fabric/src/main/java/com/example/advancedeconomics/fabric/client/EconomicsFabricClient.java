package com.example.advancedeconomics.fabric.client;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import com.example.advancedeconomics.ui.EconomicsBoxScreen;
import net.fabricmc.api.ClientModInitializer;

public class EconomicsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AdvancedEconomicsCommon.LOGGER.info("=============================================");
        AdvancedEconomicsCommon.LOGGER.info("Advanced Economics Fabric Client Initialized (v0.3)");
        AdvancedEconomicsCommon.LOGGER.info("Registered Keybinding: 'N' (GLFW_KEY_N)");
        AdvancedEconomicsCommon.LOGGER.info("Live Reload Mode: ACTIVE");
        AdvancedEconomicsCommon.LOGGER.info("=============================================");

        // Display the UI Box HUD upon client initialization and key toggle
        EconomicsBoxScreen.toggle();
        AdvancedEconomicsCommon.LOGGER.info("\n{}", EconomicsBoxScreen.renderBoxHUD());
    }

    public static void onNKeyPressed() {
        boolean state = EconomicsBoxScreen.toggle();
        if (state) {
            AdvancedEconomicsCommon.LOGGER.info("\n{}", EconomicsBoxScreen.renderBoxHUD());
        }
    }
}
