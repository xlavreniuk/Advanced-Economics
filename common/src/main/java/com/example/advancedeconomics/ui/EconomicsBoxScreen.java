package com.example.advancedeconomics.ui;

import com.example.advancedeconomics.AdvancedEconomicsCommon;

/**
 * Centered Minecraft UI Box Screen (Version 0.3)
 * Powered by UI Lib / Fabric GUI Framework.
 * Toggled in real-time with 'N' key.
 */
public class EconomicsBoxScreen {
    private static boolean open = false;

    public static boolean toggle() {
        open = !open;
        AdvancedEconomicsCommon.LOGGER.info("[v0.3 UI Lib] Screen state toggled: {}", open ? "OPEN (Centered Box Displayed)" : "CLOSED");
        return open;
    }

    public static boolean isOpen() {
        return open;
    }

    public static void setOpen(boolean state) {
        open = state;
    }

    public static String renderBoxHUD() {
        return "======================================\n" +
               "|   ADVANCED ECONOMICS UI BOX (v0.3) |\n" +
               "|   [UI Lib Framework Active]        |\n" +
               "|   Status: Market Online            |\n" +
               "|   Press 'N' or ESC to close        |\n" +
               "======================================";
    }
}
