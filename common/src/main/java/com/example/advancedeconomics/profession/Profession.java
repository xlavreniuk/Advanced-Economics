package com.example.advancedeconomics.profession;

/**
 * Profession Enum with concise descriptions and 2%/level sell price bonuses.
 */
public enum Profession {
    NONE("Unemployed", "No active profession.", "none"),
    LUMBERJACK("Lumberjack", "Wood & forestry (+2%/lvl)", "wood"),
    MINER("Miner", "Stones & ores (+2%/lvl)", "miner"),
    FARMER("Farmer", "Crops & food (+2%/lvl)", "farmer"),
    HUNTER("Hunter", "Mob drops & meats (+2%/lvl)", "hunter"),
    WEAPONSMITH("Weaponsmith", "Weapons & armor (+2%/lvl)", "gear");

    private final String displayName;
    private final String description;
    private final String category;

    Profession(String displayName, String description, String category) {
        this.displayName = displayName;
        this.description = description;
        this.category = category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public static Profession fromId(String id) {
        if (id == null) return NONE;
        for (Profession p : values()) {
            if (p.name().equalsIgnoreCase(id) || p.getDisplayName().equalsIgnoreCase(id)) {
                return p;
            }
        }
        return NONE;
    }
}
