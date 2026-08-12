package com.example.advancedeconomics.shop;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Global Economy Price Multipliers Config (v0.29).
 */
public class ShopSettings {

    private static int sellMultiplier   = 1;
    private static int buyMultiplier    = 5;
    private static int unlockMultiplier = 10;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class SettingsData {
        public int sellMultiplier   = 1;
        public int buyMultiplier    = 5;
        public int unlockMultiplier = 10;
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

    public static void setSellMultiplier(int mult) {
        sellMultiplier = Math.max(1, mult);
    }

    public static void setBuyMultiplier(int mult) {
        buyMultiplier = Math.max(1, mult);
    }

    public static void setUnlockMultiplier(int mult) {
        unlockMultiplier = Math.max(1, mult);
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
                sellMultiplier   = Math.max(1, data.sellMultiplier);
                buyMultiplier    = Math.max(1, data.buyMultiplier);
                unlockMultiplier = Math.max(1, data.unlockMultiplier);
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
            data.sellMultiplier   = sellMultiplier;
            data.buyMultiplier    = buyMultiplier;
            data.unlockMultiplier = unlockMultiplier;
            GSON.toJson(data, writer);
        } catch (Exception e) {
            AdvancedEconomicsCommon.LOGGER.error("[AE] Failed to save settings: {}", e.getMessage());
        }
    }
}
