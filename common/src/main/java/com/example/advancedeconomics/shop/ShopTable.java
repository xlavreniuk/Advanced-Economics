package com.example.advancedeconomics.shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Default Item Base Price Data Table for 20 common Minecraft items.
 */
public class ShopTable {

    private static final List<ShopItem> ITEMS = new ArrayList<>();

    static {
        ITEMS.add(new ShopItem("dirt", "Dirt", 1));
        ITEMS.add(new ShopItem("cobblestone", "Cobblestone", 2));
        ITEMS.add(new ShopItem("stone", "Stone", 3));
        ITEMS.add(new ShopItem("oak_log", "Oak Log", 4));
        ITEMS.add(new ShopItem("oak_planks", "Oak Planks", 2));
        ITEMS.add(new ShopItem("torch", "Torch", 2));
        ITEMS.add(new ShopItem("glass", "Glass", 5));
        ITEMS.add(new ShopItem("coal", "Coal", 10));
        ITEMS.add(new ShopItem("iron_ingot", "Iron Ingot", 25));
        ITEMS.add(new ShopItem("gold_ingot", "Gold Ingot", 50));
        ITEMS.add(new ShopItem("diamond", "Diamond", 150));
        ITEMS.add(new ShopItem("emerald", "Emerald", 200));
        ITEMS.add(new ShopItem("redstone", "Redstone", 15));
        ITEMS.add(new ShopItem("wheat", "Wheat", 4));
        ITEMS.add(new ShopItem("bread", "Bread", 8));
        ITEMS.add(new ShopItem("apple", "Apple", 10));
        ITEMS.add(new ShopItem("cooked_beef", "Cooked Beef", 12));
        ITEMS.add(new ShopItem("stone_axe", "Stone Axe", 15));
        ITEMS.add(new ShopItem("iron_pickaxe", "Iron Pickaxe", 80));
        ITEMS.add(new ShopItem("iron_sword", "Iron Sword", 60));
    }

    public static List<ShopItem> getItems() {
        return Collections.unmodifiableList(ITEMS);
    }

    public static ShopItem findById(String id) {
        if (id == null) return null;
        for (ShopItem item : ITEMS) {
            if (item.id().equalsIgnoreCase(id)) return item;
        }
        return null;
    }
}
