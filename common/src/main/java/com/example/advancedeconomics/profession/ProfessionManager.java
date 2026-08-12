package com.example.advancedeconomics.profession;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-Authoritative Profession Manager.
 * Stores and manages player professions with persistence.
 */
public class ProfessionManager {

    private static final Map<UUID, Profession> PROFESSIONS = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Profession getProfession(UUID playerUuid) {
        return PROFESSIONS.getOrDefault(playerUuid, Profession.NONE);
    }

    public static void setProfession(UUID playerUuid, Profession profession) {
        Profession p = (profession != null) ? profession : Profession.NONE;
        PROFESSIONS.put(playerUuid, p);
        AdvancedEconomicsCommon.LOGGER.info("[ProfessionManager] Profession updated for {}: {}", playerUuid, p.getDisplayName());
    }

    public static synchronized void load(File worldDataDir) {
        PROFESSIONS.clear();
        File saveFile = new File(worldDataDir, "advanced_economics_professions.json");

        if (!saveFile.exists()) return;

        try (FileReader reader = new FileReader(saveFile, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject profsObj = root.has("professions") ? root.getAsJsonObject("professions") : new JsonObject();

            for (String uuidStr : profsObj.keySet()) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String profStr = profsObj.get(uuidStr).getAsString();
                    PROFESSIONS.put(uuid, Profession.fromString(profStr));
                } catch (Exception e) {
                    AdvancedEconomicsCommon.LOGGER.error("[ProfessionManager] Failed to parse profession for {}", uuidStr, e);
                }
            }
        } catch (Exception e) {
            AdvancedEconomicsCommon.LOGGER.error("[ProfessionManager] Error loading professions: {}", e.getMessage(), e);
        }
    }

    public static synchronized void save(File worldDataDir) {
        try {
            if (!worldDataDir.exists()) worldDataDir.mkdirs();
            File saveFile = new File(worldDataDir, "advanced_economics_professions.json");

            JsonObject root = new JsonObject();
            JsonObject profsObj = new JsonObject();
            for (Map.Entry<UUID, Profession> entry : PROFESSIONS.entrySet()) {
                profsObj.addProperty(entry.getKey().toString(), entry.getValue().name());
            }
            root.add("professions", profsObj);

            try (FileWriter writer = new FileWriter(saveFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            AdvancedEconomicsCommon.LOGGER.error("[ProfessionManager] Error saving professions: {}", e.getMessage(), e);
        }
    }
}
