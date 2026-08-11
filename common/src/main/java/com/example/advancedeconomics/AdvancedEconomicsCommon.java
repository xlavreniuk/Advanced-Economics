package com.example.advancedeconomics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvancedEconomicsCommon {
    public static final String MOD_ID = "advanced_economics";
    public static final String MOD_NAME = "Advanced Economics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static void init() {
        LOGGER.info("Initializing {} Common (Version Dev 0.1)...", MOD_NAME);
        LOGGER.info("UI Lib dependency configured from https://modrinth.com/mod/ui-lib");
    }
}
