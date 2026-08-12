package com.example.advancedeconomics.fabric.client;

import java.util.HashSet;
import java.util.Set;

/**
 * Client-side cached read-only economy & profession state (v0.38).
 */
public class ClientEconomyState {

    private static long balance = 0;
    private static String profession = "Unemployed";
    private static int level = 1;
    private static long xp = 0;
    private static long maxXp = 100;

    private static int sellMultiplier = 1;
    private static int buyMultiplier = 5;
    private static int unlockMultiplier = 10;

    private static boolean allowSelling = true;
    private static boolean allowBuying = true;
    private static boolean allowUnlocking = true;
    private static boolean enableProfessions = true;
    private static boolean enableXpLeveling = true;

    private static final Set<String> UNLOCKED_ITEMS = new HashSet<>();

    public static long getBalance() {
        return balance;
    }

    public static void setBalance(long newBalance) {
        balance = newBalance;
    }

    public static String getProfession() {
        return profession;
    }

    public static int getLevel() {
        return level;
    }

    public static long getXp() {
        return xp;
    }

    public static long getMaxXp() {
        return maxXp;
    }

    public static void setProfessionData(String newProfession, int newLevel, long newXp, long newMaxXp) {
        profession = newProfession;
        level = newLevel;
        xp = newXp;
        maxXp = Math.max(1, newMaxXp);
    }

    public static void setProfession(String newProfession) {
        profession = newProfession;
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

    public static void setSettings(int sell, int buy, int unlock, boolean sellOn, boolean buyOn, boolean unlockOn, boolean profOn, boolean xpOn) {
        sellMultiplier = Math.max(1, sell);
        buyMultiplier = Math.max(1, buy);
        unlockMultiplier = Math.max(1, unlock);

        allowSelling = sellOn;
        allowBuying = buyOn;
        allowUnlocking = unlockOn;
        enableProfessions = profOn;
        enableXpLeveling = xpOn;
    }

    public static boolean isUnlocked(String itemId) {
        if (itemId == null) return false;
        return UNLOCKED_ITEMS.contains(itemId.trim().toLowerCase());
    }

    public static void setUnlockedItems(Set<String> items) {
        UNLOCKED_ITEMS.clear();
        if (items != null) {
            for (String id : items) {
                if (id != null && !id.isBlank()) {
                    UNLOCKED_ITEMS.add(id.trim().toLowerCase());
                }
            }
        }
    }
}
