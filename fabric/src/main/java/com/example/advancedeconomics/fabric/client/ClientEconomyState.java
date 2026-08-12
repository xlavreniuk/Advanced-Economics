package com.example.advancedeconomics.fabric.client;

import java.util.HashSet;
import java.util.Set;

/**
 * Read-Only Client Economy, Profession, and Shop State.
 * Synced from Server.
 */
public class ClientEconomyState {

    private static long playerBalance = 100L;
    private static String playerProfession = "No Profession";

    private static int sellMultiplier = 1;
    private static int buyMultiplier = 5;
    private static int unlockMultiplier = 10;

    private static final Set<String> UNLOCKED_ITEMS = new HashSet<>();

    public static long getBalance() {
        return playerBalance;
    }

    public static void setBalance(long balance) {
        playerBalance = Math.max(0, balance);
    }

    public static String getProfession() {
        return playerProfession;
    }

    public static void setProfession(String profession) {
        playerProfession = (profession != null && !profession.isEmpty()) ? profession : "No Profession";
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

    public static void setMultipliers(int sell, int buy, int unlock) {
        sellMultiplier = Math.max(1, sell);
        buyMultiplier = Math.max(1, buy);
        unlockMultiplier = Math.max(1, unlock);
    }

    public static boolean isUnlocked(String itemId) {
        return itemId != null && UNLOCKED_ITEMS.contains(itemId.toLowerCase());
    }

    public static void setUnlockedItems(Set<String> items) {
        UNLOCKED_ITEMS.clear();
        if (items != null) {
            items.forEach(i -> UNLOCKED_ITEMS.add(i.toLowerCase()));
        }
    }
}
