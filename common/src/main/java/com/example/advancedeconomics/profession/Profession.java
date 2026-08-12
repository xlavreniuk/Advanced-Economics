package com.example.advancedeconomics.profession;

/**
 * Profession Enum defining career paths with specializations and sell bonuses.
 */
public enum Profession {
    NONE("Unemployed", "No active profession selected.", "none"),
    LUMBERJACK("Lumberjack", "Wood specialist (+5%/level sell bonus on wood).", "wood"),
    MINER("Miner", "Mining specialist (+5%/level sell bonus on ores/stone).", "miner"),
    FARMER("Farmer", "Crops specialist (+5%/level sell bonus on crops/food).", "farmer"),
    HUNTER("Hunter", "Hunter specialist (+5%/level sell bonus on mob drops).", "hunter"),
    WEAPONSMITH("Weaponsmith", "Forge specialist (+5%/level sell bonus on gear).", "gear");

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
