package com.example.advancedeconomics.shop;

/**
 * Shop Item Entry holding item identifier, display name, and base price.
 */
public record ShopItem(String id, String displayName, long basePrice) {
}
