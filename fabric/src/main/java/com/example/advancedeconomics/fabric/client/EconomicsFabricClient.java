package com.example.advancedeconomics.fabric.client;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import com.example.advancedeconomics.ui.EconomicsBoxScreen;
import net.fabricmc.api.ClientModInitializer;

public class EconomicsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AdvancedEconomicsCommon.LOGGER.info("Advanced Economics Fabric Client Initialized for 1.20.4 / Fabric 26.2.");
        AdvancedEconomicsCommon.LOGGER.info("Keybinding 'N' registered to toggle centered Minecraft UI box.");
    }

    public static void onNKeyPressed() {
        boolean open = EconomicsBoxScreen.toggle();
        AdvancedEconomicsCommon.LOGGER.info("Key 'N' pressed: Screen box state is now {}", open ? "OPEN" : "CLOSED");
    }
}
