package com.example.advancedeconomics.fabric.client;

/**
 * Read-Only Client Economy State.
 * Displays official balance synced from the Server.
 * Cannot modify server economy state.
 */
public class ClientEconomyState {

    private static long playerBalance = 100L;

    public static long getBalance() {
        return playerBalance;
    }

    public static void setBalance(long balance) {
        playerBalance = Math.max(0, balance);
    }
}
