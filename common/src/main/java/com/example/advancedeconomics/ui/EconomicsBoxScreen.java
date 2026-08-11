package com.example.advancedeconomics.ui;

import com.example.advancedeconomics.AdvancedEconomicsCommon;

/**
 * Centered Minecraft UI Box Screen (Version 0.2)
 * Framework: UI Lib (https://modrinth.com/mod/ui-lib)
 */
public class EconomicsBoxScreen {
    public static final String UI_LIB_SOURCE = "https://modrinth.com/mod/ui-lib";
    private static boolean isOpen = false;

    public static boolean toggle() {
        isOpen = !isOpen;
        AdvancedEconomicsCommon.LOGGER.info("[UI Lib] Economics UI Box toggled: {}", isOpen ? "OPEN" : "CLOSED");
        return isOpen;
    }

    public static boolean isOpen() {
        return isOpen;
    }

    public static void setOpen(boolean open) {
        isOpen = open;
    }

    public static String getTitle() {
        return "Advanced Economics v0.2 (UI Lib Powered)";
    }

    public static String getInstruction() {
        return "Press 'N' or ESC to close UI Box";
    }
}
