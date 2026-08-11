package com.example.advancedeconomics.ui;

import com.example.advancedeconomics.AdvancedEconomicsCommon;

/**
 * Minecraft UI Box screen for Advanced Economics (Version 0.1)
 * Displays a classic centered Minecraft container panel when "N" is pressed.
 */
public class EconomicsBoxScreen {
    private static boolean isOpen = false;

    public static boolean toggle() {
        isOpen = !isOpen;
        AdvancedEconomicsCommon.LOGGER.info("Economics UI Box toggled: {}", isOpen ? "OPEN" : "CLOSED");
        return isOpen;
    }

    public static boolean isOpen() {
        return isOpen;
    }

    public static void setOpen(boolean open) {
        isOpen = open;
    }

    public static String getTitle() {
        return "Advanced Economics v0.1";
    }

    public static String getInstruction() {
        return "Press 'N' or ESC to close";
    }
}
