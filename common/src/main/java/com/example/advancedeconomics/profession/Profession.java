package com.example.advancedeconomics.profession;

/**
 * Profession Enum with concise descriptions.
 */
public enum Profession {
    NONE("Unemployed", "No active profession.", "none"),
    LUMBERJACK("Lumberjack", "Wood & forestry", "wood"),
    MINER("Miner", "Stones & ores", "miner"),
    FARMER("Farmer", "Crops & food", "farmer"),
    HUNTER("Hunter", "Mob drops & meats", "hunter"),
    WEAPONSMITH("Weaponsmith", "Weapons & armor", "gear");

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
