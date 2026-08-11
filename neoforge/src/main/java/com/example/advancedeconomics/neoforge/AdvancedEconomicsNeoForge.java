package com.example.advancedeconomics.neoforge;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import net.neoforged.fml.common.Mod;

@Mod(AdvancedEconomicsCommon.MOD_ID)
public class AdvancedEconomicsNeoForge {
    public AdvancedEconomicsNeoForge() {
        AdvancedEconomicsCommon.init();
        AdvancedEconomicsCommon.LOGGER.info("Advanced Economics NeoForge initializer completed.");
    }
}
