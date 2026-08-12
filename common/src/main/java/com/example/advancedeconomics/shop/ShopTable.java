package com.example.advancedeconomics.shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scaled Base Price Table for Advanced Economics (v0.22).
 * All prices scaled down 100x ($1 minimum base unit).
 */
public class ShopTable {

    private static final List<ShopItem> ITEMS = new ArrayList<>();

    static {
        // --- 1. Basic Common Items & Blocks ($1) ---
        ITEMS.add(new ShopItem("stick", "Stick", 1));
        ITEMS.add(new ShopItem("dirt", "Dirt", 1));
        ITEMS.add(new ShopItem("coarse_dirt", "Coarse Dirt", 1));
        ITEMS.add(new ShopItem("wheat_seeds", "Wheat Seeds", 1));
        ITEMS.add(new ShopItem("rotten_flesh", "Rotten Flesh", 1));
        ITEMS.add(new ShopItem("cobblestone", "Cobblestone", 1));
        ITEMS.add(new ShopItem("sand", "Sand", 1));
        ITEMS.add(new ShopItem("gravel", "Gravel", 1));
        ITEMS.add(new ShopItem("grass_block", "Grass Block", 1));
        ITEMS.add(new ShopItem("granite", "Granite", 1));
        ITEMS.add(new ShopItem("diorite", "Diorite", 1));
        ITEMS.add(new ShopItem("andesite", "Andesite", 1));
        ITEMS.add(new ShopItem("sugar_cane", "Sugar Cane", 1));
        ITEMS.add(new ShopItem("bamboo", "Bamboo", 1));
        ITEMS.add(new ShopItem("torch", "Torch", 1));
        ITEMS.add(new ShopItem("feather", "Feather", 1));
        ITEMS.add(new ShopItem("stone", "Stone", 1));
        ITEMS.add(new ShopItem("mossy_cobblestone", "Mossy Cobblestone", 1));
        ITEMS.add(new ShopItem("cactus", "Cactus", 1));
        ITEMS.add(new ShopItem("arrow", "Arrow", 1));

        // --- 2. Wood & Building Materials ($1) ---
        ITEMS.add(new ShopItem("oak_log", "Oak Log", 1));
        ITEMS.add(new ShopItem("birch_log", "Birch Log", 1));
        ITEMS.add(new ShopItem("spruce_log", "Spruce Log", 1));
        ITEMS.add(new ShopItem("jungle_log", "Jungle Log", 1));
        ITEMS.add(new ShopItem("acacia_log", "Acacia Log", 1));
        ITEMS.add(new ShopItem("dark_oak_log", "Dark Oak Log", 1));
        ITEMS.add(new ShopItem("mangrove_log", "Mangrove Log", 1));
        ITEMS.add(new ShopItem("cherry_log", "Cherry Log", 1));
        ITEMS.add(new ShopItem("oak_planks", "Oak Planks", 1));
        ITEMS.add(new ShopItem("stone_bricks", "Stone Bricks", 1));
        ITEMS.add(new ShopItem("glass", "Glass", 1));
        ITEMS.add(new ShopItem("bone", "Bone", 1));
        ITEMS.add(new ShopItem("string", "String", 1));
        ITEMS.add(new ShopItem("crafting_table", "Crafting Table", 1));
        ITEMS.add(new ShopItem("chest", "Chest", 1));
        ITEMS.add(new ShopItem("bricks", "Bricks", 1));
        ITEMS.add(new ShopItem("furnace", "Furnace", 1));

        // --- 3. Crops & Food ($1 - $2) ---
        ITEMS.add(new ShopItem("wheat", "Wheat", 1));
        ITEMS.add(new ShopItem("carrot", "Carrot", 1));
        ITEMS.add(new ShopItem("potato", "Potato", 1));
        ITEMS.add(new ShopItem("baked_potato", "Baked Potato", 1));
        ITEMS.add(new ShopItem("bread", "Bread", 1));
        ITEMS.add(new ShopItem("apple", "Apple", 1));
        ITEMS.add(new ShopItem("raw_beef", "Raw Beef", 1));
        ITEMS.add(new ShopItem("cooked_beef", "Cooked Beef", 1));
        ITEMS.add(new ShopItem("golden_carrot", "Golden Carrot", 1));
        ITEMS.add(new ShopItem("golden_apple", "Golden Apple", 2));

        // --- 4. Minerals & Drops ($1 - $3) ---
        ITEMS.add(new ShopItem("charcoal", "Charcoal", 1));
        ITEMS.add(new ShopItem("coal", "Coal", 1));
        ITEMS.add(new ShopItem("copper_ingot", "Copper Ingot", 1));
        ITEMS.add(new ShopItem("leather", "Leather", 1));
        ITEMS.add(new ShopItem("gunpowder", "Gunpowder", 1));
        ITEMS.add(new ShopItem("redstone", "Redstone", 1));
        ITEMS.add(new ShopItem("lapis_lazuli", "Lapis Lazuli", 1));
        ITEMS.add(new ShopItem("amethyst_shard", "Amethyst Shard", 1));
        ITEMS.add(new ShopItem("slime_ball", "Slimeball", 1));
        ITEMS.add(new ShopItem("iron_ingot", "Iron Ingot", 1));
        ITEMS.add(new ShopItem("ender_pearl", "Ender Pearl", 1));
        ITEMS.add(new ShopItem("blaze_rod", "Blaze Rod", 1));
        ITEMS.add(new ShopItem("gold_ingot", "Gold Ingot", 1));
        ITEMS.add(new ShopItem("ghast_tear", "Ghast Tear", 1));
        ITEMS.add(new ShopItem("diamond", "Diamond", 2));
        ITEMS.add(new ShopItem("emerald", "Emerald", 2));
        ITEMS.add(new ShopItem("netherite_scrap", "Netherite Scrap", 3));

        // --- 5. Weapons, Tools & Armor ($1 - $18) ---
        ITEMS.add(new ShopItem("stone_axe", "Stone Axe", 1));
        ITEMS.add(new ShopItem("stone_pickaxe", "Stone Pickaxe", 1));
        ITEMS.add(new ShopItem("bow", "Bow", 1));
        ITEMS.add(new ShopItem("iron_shovel", "Iron Shovel", 1));
        ITEMS.add(new ShopItem("shield", "Shield", 1));
        ITEMS.add(new ShopItem("iron_sword", "Iron Sword", 1));
        ITEMS.add(new ShopItem("iron_pickaxe", "Iron Pickaxe", 1));
        ITEMS.add(new ShopItem("iron_axe", "Iron Axe", 1));
        ITEMS.add(new ShopItem("iron_chestplate", "Iron Chestplate", 2));
        ITEMS.add(new ShopItem("diamond_sword", "Diamond Sword", 3));
        ITEMS.add(new ShopItem("diamond_pickaxe", "Diamond Pickaxe", 5));
        ITEMS.add(new ShopItem("diamond_axe", "Diamond Axe", 5));
        ITEMS.add(new ShopItem("diamond_chestplate", "Diamond Chestplate", 12));
        ITEMS.add(new ShopItem("netherite_ingot", "Netherite Ingot", 12));
        ITEMS.add(new ShopItem("netherite_sword", "Netherite Sword", 16));
        ITEMS.add(new ShopItem("netherite_pickaxe", "Netherite Pickaxe", 18));

        // --- 6. High-Tier & Rare Artifacts ($2 - $500) ---
        ITEMS.add(new ShopItem("iron_block", "Iron Block", 2));
        ITEMS.add(new ShopItem("gold_block", "Gold Block", 5));
        ITEMS.add(new ShopItem("diamond_block", "Diamond Block", 14));
        ITEMS.add(new ShopItem("emerald_block", "Emerald Block", 18));
        ITEMS.add(new ShopItem("enchanted_golden_apple", "Enchanted Golden Apple", 25));
        ITEMS.add(new ShopItem("totem_of_undying", "Totem of Undying", 35));
        ITEMS.add(new ShopItem("netherite_block", "Netherite Block", 108));
        ITEMS.add(new ShopItem("beacon", "Beacon", 120));
        ITEMS.add(new ShopItem("elytra", "Elytra", 150));
        ITEMS.add(new ShopItem("nether_star", "Nether Star", 250));
        ITEMS.add(new ShopItem("dragon_egg", "Dragon Egg", 500));
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
