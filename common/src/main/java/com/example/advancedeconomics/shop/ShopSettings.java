package com.example.advancedeconomics.shop;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

/**
 * Global Economy Settings & Price Multipliers Manager.
 * Default Multipliers:
 *   Sell:   1x (Sell Price   = Base * 1)
 *   Buy:    5x (Buy Price    = Base * 5)
 *   Unlock: 10x (Unlock Cost = Base * 10)
 */
public class ShopSettings {

    private static int sellMultiplier = 1;
    private static int buyMultiplier = 5;
    private static int unlockMultiplier = 10;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static int getSellMultiplier() {
        return sellMultiplier;
    }

    public static void setSellMultiplier(int mult) {
        sellMultiplier = Math.max(1, mult);
    }

    public static int getBuyMultiplier() {
        return buyMultiplier;
    }

    public static void setBuyMultiplier(int mult) {
        buyMultiplier = Math.max(1, mult);
    }

    public static int getUnlockMultiplier() {
        return unlockMultiplier;
    }

    public static void setUnlockMultiplier(int mult) {
        unlockMultiplier = Math.max(1, mult);
    }

    public static long calculateSellPrice(long basePrice) {
        return basePrice * sellMultiplier;
    }

    public static long calculateBuyPrice(long basePrice) {
        return basePrice * buyMultiplier;
    }

    public static long calculateUnlockPrice(long basePrice) {
        return basePrice * unlockMultiplier;
    }

    public static synchronized void load(File worldDataDir) {
        File saveFile = new File(worldDataDir, "shop_settings.json");
        if (!saveFile.exists()) return;

        try (FileReader reader = new FileReader(saveFile, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("sellMultiplier")) sellMultiplier = root.get("sellMultiplier").getAsInt();
            if (root.has("buyMultiplier")) buyMultiplier = root.get("buyMultiplier").getAsInt();
            if (root.has("unlockMultiplier")) unlockMultiplier = root.get("unlockMultiplier").getAsInt();
        } catch (Exception e) {
            AdvancedEconomicsCommon.LOGGER.error("[ShopSettings] Failed to load settings: {}", e.getMessage(), e);
        }
    }

    public static synchronized void save(File worldDataDir) {
        try {
            if (!worldDataDir.exists()) worldDataDir.mkdirs();
            File saveFile = new File(worldDataDir, "shop_settings.json");

            JsonObject root = new JsonObject();
            root.addProperty("sellMultiplier", sellMultiplier);
            root.addProperty("buyMultiplier", buyMultiplier);
            root.addProperty("unlockMultiplier", unlockMultiplier);

            try (FileWriter writer = new FileWriter(saveFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            AdvancedEconomicsCommon.LOGGER.error("[ShopSettings] Failed to save settings: {}", e.getMessage(), e);
        }
    }
}
