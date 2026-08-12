package com.example.advancedeconomics.fabric.client;

/**
 * Read-Only Client Economy & Profession State.
 * Displays official balance and profession synced from the Server.
 */
public class ClientEconomyState {

    private static long playerBalance = 100L;
    private static String playerProfession = "None";

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
        playerProfession = (profession != null && !profession.isEmpty()) ? profession : "None";
    }
}
