package com.example.advancedeconomics.fabric.client;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import com.example.advancedeconomics.ui.EconomicsBoxScreen;
import net.fabricmc.api.ClientModInitializer;

public class EconomicsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AdvancedEconomicsCommon.LOGGER.info("Advanced Economics Fabric Client Initialized (v0.3)...");
        AdvancedEconomicsCommon.LOGGER.info("Live Reload Mode: Active. Registered 'N' Key for Centered UI Box.");
    }

    public static void toggleScreen() {
        boolean state = EconomicsBoxScreen.toggle();
        if (state) {
            AdvancedEconomicsCommon.LOGGER.info("\n{}", EconomicsBoxScreen.renderBoxHUD());
        }
    }
}
