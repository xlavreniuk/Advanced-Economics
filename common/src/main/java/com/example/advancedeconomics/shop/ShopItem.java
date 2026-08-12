package com.example.advancedeconomics.shop;

/**
 * Shop Item record holding ID, Display Name, and decimal Base Price ($0.01 minimum).
 */
public record ShopItem(String id, String displayName, double basePrice) {
}
