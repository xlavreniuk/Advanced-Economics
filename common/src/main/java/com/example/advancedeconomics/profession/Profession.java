package com.example.advancedeconomics.profession;

/**
 * Player Professions in Advanced Economics.
 */
public enum Profession {
    NONE("No Profession"),
    FARMER("Farmer"),
    MINER("Miner"),
    MERCHANT("Merchant"),
    SMITH("Smith"),
    BUILDER("Builder");

    private final String displayName;

    Profession(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Profession fromString(String name) {
        if (name == null || name.isEmpty()) return NONE;
        for (Profession p : values()) {
            if (p.name().equalsIgnoreCase(name) || p.displayName.equalsIgnoreCase(name)) {
                return p;
            }
        }
        return NONE;
    }
}
