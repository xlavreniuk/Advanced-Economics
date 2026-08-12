package com.example.advancedeconomics.shop;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Global Economy Settings & Feature Toggles Config (v0.38).
 */
public class ShopSettings {

    private static int sellMultiplier     = 1;
    private static int buyMultiplier      = 5;
    private static int unlockMultiplier   = 10;

    private static boolean allowSelling     = true;
    private static boolean allowBuying      = true;
    private static boolean allowUnlocking   = true;
    private static boolean enableProfessions= true;
    private static boolean enableXpLeveling = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class SettingsData {
        public int sellMultiplier     = 1;
        public int buyMultiplier      = 5;
        public int unlockMultiplier   = 10;

        public boolean allowSelling     = true;
        public boolean allowBuying      = true;
        public boolean allowUnlocking   = true;
        public boolean enableProfessions= true;
        public boolean enableXpLeveling = true;
    }

    public static int getSellMultiplier() {
        return sellMultiplier;
    }

    public static int getBuyMultiplier() {
        return buyMultiplier;
    }

    public static int getUnlockMultiplier() {
        return unlockMultiplier;
    }

    public static boolean isAllowSelling() {
        return allowSelling;
    }

    public static boolean isAllowBuying() {
        return allowBuying;
    }

    public static boolean isAllowUnlocking() {
        return allowUnlocking;
    }

    public static boolean isEnableProfessions() {
        return enableProfessions;
    }

    public static boolean isEnableXpLeveling() {
        return enableXpLeveling;
    }

    public static void setSellMultiplier(int mult) {
        sellMultiplier = Math.max(1, mult);
    }

    public static void setBuyMultiplier(int mult) {
        buyMultiplier = Math.max(1, mult);
    }

    public static void setUnlockMultiplier(int mult) {
        unlockMultiplier = Math.max(1, mult);
    }

    public static void setAllowSelling(boolean allow) {
        allowSelling = allow;
    }

    public static void setAllowBuying(boolean allow) {
        allowBuying = allow;
    }

    public static void setAllowUnlocking(boolean allow) {
        allowUnlocking = allow;
    }

    public static void setEnableProfessions(boolean enable) {
        enableProfessions = enable;
    }

    public static void setEnableXpLeveling(boolean enable) {
        enableXpLeveling = enable;
    }

    public static double calculateSellPrice(double basePrice) {
        return Math.max(0.01, Math.round(basePrice * sellMultiplier * 100.0) / 100.0);
    }

    public static double calculateBuyPrice(double basePrice) {
        return Math.max(0.01, Math.round(basePrice * buyMultiplier * 100.0) / 100.0);
    }

    public static double calculateUnlockPrice(double basePrice) {
        return Math.max(0.01, Math.round(basePrice * unlockMultiplier * 100.0) / 100.0);
    }

    public static void load(File dataDir) {
        File file = new File(dataDir, "settings.json");
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file)) {
            SettingsData data = GSON.fromJson(reader, SettingsData.class);
            if (data != null) {
                sellMultiplier     = Math.max(1, data.sellMultiplier);
                buyMultiplier      = Math.max(1, data.buyMultiplier);
                unlockMultiplier   = Math.max(1, data.unlockMultiplier);

                allowSelling       = data.allowSelling;
                allowBuying        = data.allowBuying;
                allowUnlocking     = data.allowUnlocking;
                enableProfessions  = data.enableProfessions;
                enableXpLeveling   = data.enableXpLeveling;
            }
        } catch (Exception e) {
            AdvancedEconomicsCommon.LOGGER.error("[AE] Failed to load settings: {}", e.getMessage());
        }
    }

    public static void save(File dataDir) {
        if (!dataDir.exists()) dataDir.mkdirs();
        File file = new File(dataDir, "settings.json");
        try (FileWriter writer = new FileWriter(file)) {
            SettingsData data = new SettingsData();
            data.sellMultiplier     = sellMultiplier;
            data.buyMultiplier      = buyMultiplier;
            data.unlockMultiplier   = unlockMultiplier;

            data.allowSelling       = allowSelling;
            data.allowBuying        = allowBuying;
            data.allowUnlocking     = allowUnlocking;
            data.enableProfessions  = enableProfessions;
            data.enableXpLeveling   = enableXpLeveling;
            GSON.toJson(data, writer);
        } catch (Exception e) {
            AdvancedEconomicsCommon.LOGGER.error("[AE] Failed to save settings: {}", e.getMessage());
        }
    }
}
