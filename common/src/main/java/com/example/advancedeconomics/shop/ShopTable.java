package com.example.advancedeconomics.shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Extended Item Base Price Table for Advanced Economics (v0.21).
 * Features over 80 items covering:
 * - Cheapest common blocks & materials ($1 - $5)
 * - Building blocks & woods ($2 - $15)
 * - Minerals, Ores & Mob Drops ($10 - $300)
 * - Food & Farming ($3 - $150)
 * - Weapons, Tools & Armor ($15 - $1,800)
 * - High-tier & Ultra-expensive artifacts ($450 - $50,000)
 */
public class ShopTable {

    private static final List<ShopItem> ITEMS = new ArrayList<>();

    static {
        // --- 1. Basic & Cheapest Blocks ($1 - $5) ---
        ITEMS.add(new ShopItem("stick", "Stick", 1));
        ITEMS.add(new ShopItem("dirt", "Dirt", 1));
        ITEMS.add(new ShopItem("coarse_dirt", "Coarse Dirt", 1));
        ITEMS.add(new ShopItem("wheat_seeds", "Wheat Seeds", 1));
        ITEMS.add(new ShopItem("rotten_flesh", "Rotten Flesh", 2));
        ITEMS.add(new ShopItem("cobblestone", "Cobblestone", 2));
        ITEMS.add(new ShopItem("sand", "Sand", 2));
        ITEMS.add(new ShopItem("gravel", "Gravel", 2));
        ITEMS.add(new ShopItem("grass_block", "Grass Block", 2));
        ITEMS.add(new ShopItem("granite", "Granite", 2));
        ITEMS.add(new ShopItem("diorite", "Diorite", 2));
        ITEMS.add(new ShopItem("andesite", "Andesite", 2));
        ITEMS.add(new ShopItem("sugar_cane", "Sugar Cane", 2));
        ITEMS.add(new ShopItem("bamboo", "Bamboo", 2));
        ITEMS.add(new ShopItem("torch", "Torch", 2));
        ITEMS.add(new ShopItem("feather", "Feather", 3));
        ITEMS.add(new ShopItem("stone", "Stone", 3));
        ITEMS.add(new ShopItem("mossy_cobblestone", "Mossy Cobblestone", 3));
        ITEMS.add(new ShopItem("cactus", "Cactus", 3));
        ITEMS.add(new ShopItem("arrow", "Arrow", 3));

        // --- 2. Common Wood & Building Materials ($4 - $15) ---
        ITEMS.add(new ShopItem("oak_log", "Oak Log", 4));
        ITEMS.add(new ShopItem("birch_log", "Birch Log", 4));
        ITEMS.add(new ShopItem("spruce_log", "Spruce Log", 4));
        ITEMS.add(new ShopItem("jungle_log", "Jungle Log", 4));
        ITEMS.add(new ShopItem("acacia_log", "Acacia Log", 4));
        ITEMS.add(new ShopItem("dark_oak_log", "Dark Oak Log", 4));
        ITEMS.add(new ShopItem("mangrove_log", "Mangrove Log", 5));
        ITEMS.add(new ShopItem("cherry_log", "Cherry Log", 5));
        ITEMS.add(new ShopItem("oak_planks", "Oak Planks", 2));
        ITEMS.add(new ShopItem("stone_bricks", "Stone Bricks", 4));
        ITEMS.add(new ShopItem("glass", "Glass", 5));
        ITEMS.add(new ShopItem("bone", "Bone", 5));
        ITEMS.add(new ShopItem("string", "String", 4));
        ITEMS.add(new ShopItem("crafting_table", "Crafting Table", 6));
        ITEMS.add(new ShopItem("chest", "Chest", 8));
        ITEMS.add(new ShopItem("bricks", "Bricks", 8));
        ITEMS.add(new ShopItem("furnace", "Furnace", 10));

        // --- 3. Food & Crops ($4 - $150) ---
        ITEMS.add(new ShopItem("wheat", "Wheat", 4));
        ITEMS.add(new ShopItem("carrot", "Carrot", 4));
        ITEMS.add(new ShopItem("potato", "Potato", 4));
        ITEMS.add(new ShopItem("baked_potato", "Baked Potato", 7));
        ITEMS.add(new ShopItem("bread", "Bread", 8));
        ITEMS.add(new ShopItem("apple", "Apple", 10));
        ITEMS.add(new ShopItem("raw_beef", "Raw Beef", 6));
        ITEMS.add(new ShopItem("cooked_beef", "Cooked Beef", 12));
        ITEMS.add(new ShopItem("golden_carrot", "Golden Carrot", 40));
        ITEMS.add(new ShopItem("golden_apple", "Golden Apple", 150));

        // --- 4. Minerals, Ores & Drops ($10 - $300) ---
        ITEMS.add(new ShopItem("charcoal", "Charcoal", 8));
        ITEMS.add(new ShopItem("coal", "Coal", 10));
        ITEMS.add(new ShopItem("copper_ingot", "Copper Ingot", 12));
        ITEMS.add(new ShopItem("leather", "Leather", 12));
        ITEMS.add(new ShopItem("gunpowder", "Gunpowder", 15));
        ITEMS.add(new ShopItem("redstone", "Redstone", 15));
        ITEMS.add(new ShopItem("lapis_lazuli", "Lapis Lazuli", 15));
        ITEMS.add(new ShopItem("amethyst_shard", "Amethyst Shard", 20));
        ITEMS.add(new ShopItem("slime_ball", "Slimeball", 20));
        ITEMS.add(new ShopItem("iron_ingot", "Iron Ingot", 25));
        ITEMS.add(new ShopItem("ender_pearl", "Ender Pearl", 40));
        ITEMS.add(new ShopItem("blaze_rod", "Blaze Rod", 45));
        ITEMS.add(new ShopItem("gold_ingot", "Gold Ingot", 50));
        ITEMS.add(new ShopItem("ghast_tear", "Ghast Tear", 80));
        ITEMS.add(new ShopItem("diamond", "Diamond", 150));
        ITEMS.add(new ShopItem("emerald", "Emerald", 200));
        ITEMS.add(new ShopItem("netherite_scrap", "Netherite Scrap", 300));

        // --- 5. Weapons, Tools & Armor ($15 - $1,800) ---
        ITEMS.add(new ShopItem("stone_axe", "Stone Axe", 15));
        ITEMS.add(new ShopItem("stone_pickaxe", "Stone Pickaxe", 15));
        ITEMS.add(new ShopItem("bow", "Bow", 30));
        ITEMS.add(new ShopItem("iron_shovel", "Iron Shovel", 35));
        ITEMS.add(new ShopItem("shield", "Shield", 45));
        ITEMS.add(new ShopItem("iron_sword", "Iron Sword", 60));
        ITEMS.add(new ShopItem("iron_pickaxe", "Iron Pickaxe", 80));
        ITEMS.add(new ShopItem("iron_axe", "Iron Axe", 80));
        ITEMS.add(new ShopItem("iron_chestplate", "Iron Chestplate", 200));
        ITEMS.add(new ShopItem("diamond_sword", "Diamond Sword", 320));
        ITEMS.add(new ShopItem("diamond_pickaxe", "Diamond Pickaxe", 480));
        ITEMS.add(new ShopItem("diamond_axe", "Diamond Axe", 480));
        ITEMS.add(new ShopItem("diamond_chestplate", "Diamond Chestplate", 1200));
        ITEMS.add(new ShopItem("netherite_ingot", "Netherite Ingot", 1200));
        ITEMS.add(new ShopItem("netherite_sword", "Netherite Sword", 1600));
        ITEMS.add(new ShopItem("netherite_pickaxe", "Netherite Pickaxe", 1800));

        // --- 6. High-Tier & Ultra Expensive Artifacts ($225 - $50,000) ---
        ITEMS.add(new ShopItem("iron_block", "Iron Block", 225));
        ITEMS.add(new ShopItem("gold_block", "Gold Block", 450));
        ITEMS.add(new ShopItem("diamond_block", "Diamond Block", 1350));
        ITEMS.add(new ShopItem("emerald_block", "Emerald Block", 1800));
        ITEMS.add(new ShopItem("enchanted_golden_apple", "Enchanted Golden Apple", 2500));
        ITEMS.add(new ShopItem("totem_of_undying", "Totem of Undying", 3500));
        ITEMS.add(new ShopItem("netherite_block", "Netherite Block", 10800));
        ITEMS.add(new ShopItem("beacon", "Beacon", 12000));
        ITEMS.add(new ShopItem("elytra", "Elytra", 15000));
        ITEMS.add(new ShopItem("nether_star", "Nether Star", 25000));
        ITEMS.add(new ShopItem("dragon_egg", "Dragon Egg", 50000));
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
