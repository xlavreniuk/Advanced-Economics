package com.example.advancedeconomics.shop;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which items each player has unlocked.
 * An item is unlocked automatically when the player has ever owned it,
 * or when unlocked via paying the 10x unlock price in the shop.
 */
public class PlayerUnlockManager {

    private static final Map<UUID, Set<String>> UNLOCKED_ITEMS = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean isUnlocked(UUID playerUuid, String itemId) {
        if (itemId == null) return false;
        Set<String> set = UNLOCKED_ITEMS.get(playerUuid);
        return set != null && set.contains(itemId.toLowerCase());
    }

    public static boolean unlock(UUID playerUuid, String itemId) {
        if (itemId == null) return false;
        Set<String> set = UNLOCKED_ITEMS.computeIfAbsent(playerUuid, k -> ConcurrentHashMap.newKeySet());
        boolean added = set.add(itemId.toLowerCase());
        if (added) {
            AdvancedEconomicsCommon.LOGGER.info("[PlayerUnlockManager] Unlocked item '{}' for player {}", itemId, playerUuid);
        }
        return added;
    }

    public static Set<String> getUnlockedItems(UUID playerUuid) {
        Set<String> set = UNLOCKED_ITEMS.get(playerUuid);
        return set != null ? new HashSet<>(set) : Collections.emptySet();
    }

    public static synchronized void load(File worldDataDir) {
        UNLOCKED_ITEMS.clear();
        File saveFile = new File(worldDataDir, "player_unlocks.json");
        if (!saveFile.exists()) return;

        try (FileReader reader = new FileReader(saveFile, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject unlocksObj = root.has("unlocks") ? root.getAsJsonObject("unlocks") : new JsonObject();

            for (String uuidStr : unlocksObj.keySet()) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    JsonArray arr = unlocksObj.getAsJsonArray(uuidStr);
                    Set<String> set = ConcurrentHashMap.newKeySet();
                    arr.forEach(element -> set.add(element.getAsString().toLowerCase()));
                    UNLOCKED_ITEMS.put(uuid, set);
                } catch (Exception e) {
                    AdvancedEconomicsCommon.LOGGER.error("[PlayerUnlockManager] Error loading unlocks for {}", uuidStr, e);
                }
            }
        } catch (Exception e) {
            AdvancedEconomicsCommon.LOGGER.error("[PlayerUnlockManager] Error loading unlocks file: {}", e.getMessage(), e);
        }
    }

    public static synchronized void save(File worldDataDir) {
        try {
            if (!worldDataDir.exists()) worldDataDir.mkdirs();
            File saveFile = new File(worldDataDir, "player_unlocks.json");

            JsonObject root = new JsonObject();
            JsonObject unlocksObj = new JsonObject();
            for (Map.Entry<UUID, Set<String>> entry : UNLOCKED_ITEMS.entrySet()) {
                JsonArray arr = new JsonArray();
                entry.getValue().forEach(arr::add);
                unlocksObj.add(entry.getKey().toString(), arr);
            }
            root.add("unlocks", unlocksObj);

            try (FileWriter writer = new FileWriter(saveFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            AdvancedEconomicsCommon.LOGGER.error("[PlayerUnlockManager] Error saving unlocks: {}", e.getMessage(), e);
        }
    }
}
