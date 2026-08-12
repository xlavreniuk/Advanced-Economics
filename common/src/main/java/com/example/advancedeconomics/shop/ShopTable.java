package com.example.advancedeconomics.shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Extended Item Base Price Table for Advanced Economics (v0.29).
 * All prices scaled 100x starting from $0.01 precision.
 */
public class ShopTable {

    private static final List<ShopItem> ITEMS = new ArrayList<>();

    static {
        // --- 1. Basic Common Items & Blocks ($0.01 - $0.03) ---
        ITEMS.add(new ShopItem("stick", "Stick", 0.01));
        ITEMS.add(new ShopItem("dirt", "Dirt", 0.01));
        ITEMS.add(new ShopItem("coarse_dirt", "Coarse Dirt", 0.01));
        ITEMS.add(new ShopItem("wheat_seeds", "Wheat Seeds", 0.01));
        ITEMS.add(new ShopItem("rotten_flesh", "Rotten Flesh", 0.02));
        ITEMS.add(new ShopItem("cobblestone", "Cobblestone", 0.02));
        ITEMS.add(new ShopItem("sand", "Sand", 0.02));
        ITEMS.add(new ShopItem("gravel", "Gravel", 0.02));
        ITEMS.add(new ShopItem("grass_block", "Grass Block", 0.02));
        ITEMS.add(new ShopItem("granite", "Granite", 0.02));
        ITEMS.add(new ShopItem("diorite", "Diorite", 0.02));
        ITEMS.add(new ShopItem("andesite", "Andesite", 0.02));
        ITEMS.add(new ShopItem("sugar_cane", "Sugar Cane", 0.02));
        ITEMS.add(new ShopItem("bamboo", "Bamboo", 0.02));
        ITEMS.add(new ShopItem("torch", "Torch", 0.02));
        ITEMS.add(new ShopItem("feather", "Feather", 0.03));
        ITEMS.add(new ShopItem("stone", "Stone", 0.03));
        ITEMS.add(new ShopItem("mossy_cobblestone", "Mossy Cobblestone", 0.03));
        ITEMS.add(new ShopItem("cactus", "Cactus", 0.03));
        ITEMS.add(new ShopItem("arrow", "Arrow", 0.03));

        // --- 2. Wood & Building Materials ($0.02 - $0.10) ---
        ITEMS.add(new ShopItem("oak_log", "Oak Log", 0.04));
        ITEMS.add(new ShopItem("birch_log", "Birch Log", 0.04));
        ITEMS.add(new ShopItem("spruce_log", "Spruce Log", 0.04));
        ITEMS.add(new ShopItem("jungle_log", "Jungle Log", 0.04));
        ITEMS.add(new ShopItem("acacia_log", "Acacia Log", 0.04));
        ITEMS.add(new ShopItem("dark_oak_log", "Dark Oak Log", 0.04));
        ITEMS.add(new ShopItem("mangrove_log", "Mangrove Log", 0.05));
        ITEMS.add(new ShopItem("cherry_log", "Cherry Log", 0.05));
        ITEMS.add(new ShopItem("oak_planks", "Oak Planks", 0.02));
        ITEMS.add(new ShopItem("stone_bricks", "Stone Bricks", 0.04));
        ITEMS.add(new ShopItem("glass", "Glass", 0.05));
        ITEMS.add(new ShopItem("bone", "Bone", 0.05));
        ITEMS.add(new ShopItem("string", "String", 0.04));
        ITEMS.add(new ShopItem("crafting_table", "Crafting Table", 0.06));
        ITEMS.add(new ShopItem("chest", "Chest", 0.08));
        ITEMS.add(new ShopItem("bricks", "Bricks", 0.08));
        ITEMS.add(new ShopItem("furnace", "Furnace", 0.10));

        // --- 3. Crops & Food ($0.04 - $1.50) ---
        ITEMS.add(new ShopItem("wheat", "Wheat", 0.04));
        ITEMS.add(new ShopItem("carrot", "Carrot", 0.04));
        ITEMS.add(new ShopItem("potato", "Potato", 0.04));
        ITEMS.add(new ShopItem("baked_potato", "Baked Potato", 0.07));
        ITEMS.add(new ShopItem("bread", "Bread", 0.08));
        ITEMS.add(new ShopItem("apple", "Apple", 0.10));
        ITEMS.add(new ShopItem("raw_beef", "Raw Beef", 0.06));
        ITEMS.add(new ShopItem("cooked_beef", "Cooked Beef", 0.12));
        ITEMS.add(new ShopItem("golden_carrot", "Golden Carrot", 0.40));
        ITEMS.add(new ShopItem("golden_apple", "Golden Apple", 1.50));

        // --- 4. Minerals & Drops ($0.08 - $3.00) ---
        ITEMS.add(new ShopItem("charcoal", "Charcoal", 0.08));
        ITEMS.add(new ShopItem("coal", "Coal", 0.10));
        ITEMS.add(new ShopItem("copper_ingot", "Copper Ingot", 0.12));
        ITEMS.add(new ShopItem("leather", "Leather", 0.12));
        ITEMS.add(new ShopItem("gunpowder", "Gunpowder", 0.15));
        ITEMS.add(new ShopItem("redstone", "Redstone", 0.15));
        ITEMS.add(new ShopItem("lapis_lazuli", "Lapis Lazuli", 0.15));
        ITEMS.add(new ShopItem("amethyst_shard", "Amethyst Shard", 0.20));
        ITEMS.add(new ShopItem("slime_ball", "Slimeball", 0.20));
        ITEMS.add(new ShopItem("iron_ingot", "Iron Ingot", 0.25));
        ITEMS.add(new ShopItem("ender_pearl", "Ender Pearl", 0.40));
        ITEMS.add(new ShopItem("blaze_rod", "Blaze Rod", 0.45));
        ITEMS.add(new ShopItem("gold_ingot", "Gold Ingot", 0.50));
        ITEMS.add(new ShopItem("ghast_tear", "Ghast Tear", 0.80));
        ITEMS.add(new ShopItem("diamond", "Diamond", 1.50));
        ITEMS.add(new ShopItem("emerald", "Emerald", 2.00));
        ITEMS.add(new ShopItem("netherite_scrap", "Netherite Scrap", 3.00));

        // --- 5. Weapons, Tools & Armor ($0.15 - $18.00) ---
        ITEMS.add(new ShopItem("stone_axe", "Stone Axe", 0.15));
        ITEMS.add(new ShopItem("stone_pickaxe", "Stone Pickaxe", 0.15));
        ITEMS.add(new ShopItem("bow", "Bow", 0.30));
        ITEMS.add(new ShopItem("iron_shovel", "Iron Shovel", 0.35));
        ITEMS.add(new ShopItem("shield", "Shield", 0.45));
        ITEMS.add(new ShopItem("iron_sword", "Iron Sword", 0.60));
        ITEMS.add(new ShopItem("iron_pickaxe", "Iron Pickaxe", 0.80));
        ITEMS.add(new ShopItem("iron_axe", "Iron Axe", 0.80));
        ITEMS.add(new ShopItem("iron_chestplate", "Iron Chestplate", 2.00));
        ITEMS.add(new ShopItem("diamond_sword", "Diamond Sword", 3.20));
        ITEMS.add(new ShopItem("diamond_pickaxe", "Diamond Pickaxe", 4.80));
        ITEMS.add(new ShopItem("diamond_axe", "Diamond Axe", 4.80));
        ITEMS.add(new ShopItem("diamond_chestplate", "Diamond Chestplate", 12.00));
        ITEMS.add(new ShopItem("netherite_ingot", "Netherite Ingot", 12.00));
        ITEMS.add(new ShopItem("netherite_sword", "Netherite Sword", 16.00));
        ITEMS.add(new ShopItem("netherite_pickaxe", "Netherite Pickaxe", 18.00));

        // --- 6. High-Tier & Rare Artifacts ($2.25 - $500.00) ---
        ITEMS.add(new ShopItem("iron_block", "Iron Block", 2.25));
        ITEMS.add(new ShopItem("gold_block", "Gold Block", 4.50));
        ITEMS.add(new ShopItem("diamond_block", "Diamond Block", 13.50));
        ITEMS.add(new ShopItem("emerald_block", "Emerald Block", 18.00));
        ITEMS.add(new ShopItem("enchanted_golden_apple", "Enchanted Golden Apple", 25.00));
        ITEMS.add(new ShopItem("totem_of_undying", "Totem of Undying", 35.00));
        ITEMS.add(new ShopItem("netherite_block", "Netherite Block", 108.00));
        ITEMS.add(new ShopItem("beacon", "Beacon", 120.00));
        ITEMS.add(new ShopItem("elytra", "Elytra", 150.00));
        ITEMS.add(new ShopItem("nether_star", "Nether Star", 250.00));
        ITEMS.add(new ShopItem("dragon_egg", "Dragon Egg", 500.00));
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
