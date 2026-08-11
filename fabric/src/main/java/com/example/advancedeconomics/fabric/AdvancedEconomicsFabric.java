package com.example.advancedeconomics.fabric;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import net.fabricmc.api.ModInitializer;

public class AdvancedEconomicsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        AdvancedEconomicsCommon.init();
        AdvancedEconomicsCommon.LOGGER.info("Advanced Economics Fabric initializer completed.");
    }
}
