package com.example.advancedeconomics.profession;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative Profession Manager (v0.23).
 * Tracks Player Profession, Level, and XP progression with save/load persistence.
 */
public class ProfessionManager {

    public static class PlayerProfessionState {
        public String profession = Profession.NONE.name();
        public int level = 1;
        public long xp = 0;

        public PlayerProfessionState() {}

        public PlayerProfessionState(String profession, int level, long xp) {
            this.profession = profession;
            this.level = level;
            this.xp = xp;
        }

        public long getMaxXp() {
            return level * 100L;
        }
    }

    private static final Map<UUID, PlayerProfessionState> PLAYER_PROFESSIONS = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static PlayerProfessionState getPlayerState(UUID playerUuid) {
        return PLAYER_PROFESSIONS.computeIfAbsent(playerUuid, k -> new PlayerProfessionState());
    }

    public static Profession getProfession(UUID playerUuid) {
        PlayerProfessionState state = getPlayerState(playerUuid);
        return Profession.fromId(state.profession);
    }

    public static void setProfession(UUID playerUuid, Profession profession) {
        PlayerProfessionState state = getPlayerState(playerUuid);
        if (!state.profession.equalsIgnoreCase(profession.name())) {
            state.profession = profession.name();
            // Retain level or reset if changing
            AdvancedEconomicsCommon.LOGGER.info("[AE] Player {} selected profession {}", playerUuid, profession.getDisplayName());
        }
    }

    public static boolean addXp(UUID playerUuid, long amount) {
        PlayerProfessionState state = getPlayerState(playerUuid);
        if (Profession.fromId(state.profession) == Profession.NONE) return false;

        state.xp += amount;
        boolean leveledUp = false;
        while (state.xp >= state.getMaxXp()) {
            state.xp -= state.getMaxXp();
            state.level++;
            leveledUp = true;
            AdvancedEconomicsCommon.LOGGER.info("[AE] Player {} leveled up in {} to Level {}", playerUuid, state.profession, state.level);
        }
        return leveledUp;
    }

    public static double getProfessionSellBonusRatio(UUID playerUuid, String itemId) {
        PlayerProfessionState state = getPlayerState(playerUuid);
        Profession prof = Profession.fromId(state.profession);
        if (prof == Profession.NONE) return 1.0;

        // Check if item belongs to player's profession category
        boolean matches = matchItemToProfession(prof, itemId);
        if (matches) {
            // 5% bonus per level
            return 1.0 + (state.level * 0.05);
        }
        return 1.0;
    }

    private static boolean matchItemToProfession(Profession prof, String itemId) {
        if (itemId == null) return false;
        String id = itemId.toLowerCase();
        switch (prof) {
            case LUMBERJACK:
                return id.contains("log") || id.contains("planks") || id.contains("wood") || id.equals("stick");
            case MINER:
                return id.contains("stone") || id.contains("ore") || id.contains("cobble") || id.contains("ingot") || id.equals("diamond") || id.equals("emerald") || id.equals("coal") || id.equals("redstone") || id.equals("quartz") || id.equals("amethyst_shard");
            case FARMER:
                return id.equals("wheat") || id.equals("carrot") || id.equals("potato") || id.equals("baked_potato") || id.equals("bread") || id.equals("apple") || id.contains("seeds") || id.equals("sugar_cane") || id.equals("bamboo") || id.equals("cactus");
            case HUNTER:
                return id.equals("leather") || id.contains("beef") || id.contains("porkchop") || id.contains("chicken") || id.equals("bone") || id.equals("feather") || id.equals("string") || id.equals("ender_pearl") || id.equals("blaze_rod") || id.equals("ghast_tear") || id.equals("rotten_flesh") || id.equals("slime_ball");
            case WEAPONSMITH:
                return id.contains("sword") || id.contains("axe") || id.contains("pickaxe") || id.contains("shovel") || id.contains("chestplate") || id.equals("bow") || id.equals("shield") || id.equals("arrow");
            default:
                return false;
        }
    }

    public static void load(File dataDir) {
        File file = new File(dataDir, "professions.json");
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<UUID, PlayerProfessionState>>() {}.getType();
            Map<UUID, PlayerProfessionState> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                PLAYER_PROFESSIONS.clear();
                PLAYER_PROFESSIONS.putAll(loaded);
            }
        } catch (Exception e) {
            AdvancedEconomicsCommon.LOGGER.error("[AE] Failed to load professions: {}", e.getMessage());
        }
    }

    public static void save(File dataDir) {
        if (!dataDir.exists()) dataDir.mkdirs();
        File file = new File(dataDir, "professions.json");
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(PLAYER_PROFESSIONS, writer);
        } catch (Exception e) {
            AdvancedEconomicsCommon.LOGGER.error("[AE] Failed to save professions: {}", e.getMessage());
        }
    }
}
