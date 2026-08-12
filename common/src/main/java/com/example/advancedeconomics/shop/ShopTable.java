package com.example.advancedeconomics.shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Comprehensive Item Base Price Table for Advanced Economics (v0.40).
 * Over 120+ Minecraft items balanced from Tier 1 ($0.01) to Tier 6 ($500.00).
 */
public class ShopTable {

    private static final List<ShopItem> ITEMS = new ArrayList<>();

    static {
        // --- Tier 1: Abundant & Basic Resources ($0.01 - $0.03) ---
        ITEMS.add(new ShopItem("stick", "Stick", 0.01));
        ITEMS.add(new ShopItem("dirt", "Dirt", 0.01));
        ITEMS.add(new ShopItem("coarse_dirt", "Coarse Dirt", 0.01));
        ITEMS.add(new ShopItem("sand", "Sand", 0.01));
        ITEMS.add(new ShopItem("red_sand", "Red Sand", 0.01));
        ITEMS.add(new ShopItem("gravel", "Gravel", 0.01));
        ITEMS.add(new ShopItem("wheat_seeds", "Wheat Seeds", 0.01));
        ITEMS.add(new ShopItem("pumpkin_seeds", "Pumpkin Seeds", 0.01));
        ITEMS.add(new ShopItem("melon_seeds", "Melon Seeds", 0.01));
        ITEMS.add(new ShopItem("beetroot_seeds", "Beetroot Seeds", 0.01));
        ITEMS.add(new ShopItem("cobblestone", "Cobblestone", 0.01));
        ITEMS.add(new ShopItem("cobbled_deepslate", "Cobbled Deepslate", 0.01));
        ITEMS.add(new ShopItem("stone", "Stone", 0.01));
        ITEMS.add(new ShopItem("granite", "Granite", 0.01));
        ITEMS.add(new ShopItem("diorite", "Diorite", 0.01));
        ITEMS.add(new ShopItem("andesite", "Andesite", 0.01));
        ITEMS.add(new ShopItem("tuff", "Tuff", 0.01));
        ITEMS.add(new ShopItem("calcite", "Calcite", 0.01));
        ITEMS.add(new ShopItem("basalt", "Basalt", 0.01));
        ITEMS.add(new ShopItem("blackstone", "Blackstone", 0.01));
        ITEMS.add(new ShopItem("grass_block", "Grass Block", 0.02));
        ITEMS.add(new ShopItem("moss_block", "Moss Block", 0.02));
        ITEMS.add(new ShopItem("sugar_cane", "Sugar Cane", 0.02));
        ITEMS.add(new ShopItem("bamboo", "Bamboo", 0.02));
        ITEMS.add(new ShopItem("cactus", "Cactus", 0.02));
        ITEMS.add(new ShopItem("sweet_berries", "Sweet Berries", 0.02));
        ITEMS.add(new ShopItem("glow_berries", "Glow Berries", 0.02));
        ITEMS.add(new ShopItem("torch", "Torch", 0.02));
        ITEMS.add(new ShopItem("soul_torch", "Soul Torch", 0.02));
        ITEMS.add(new ShopItem("rotten_flesh", "Rotten Flesh", 0.02));
        ITEMS.add(new ShopItem("feather", "Feather", 0.03));
        ITEMS.add(new ShopItem("arrow", "Arrow", 0.03));

        // --- Tier 2: Wood, Construction & Crafted Blocks ($0.02 - $0.15) ---
        ITEMS.add(new ShopItem("oak_log", "Oak Log", 0.04));
        ITEMS.add(new ShopItem("spruce_log", "Spruce Log", 0.04));
        ITEMS.add(new ShopItem("birch_log", "Birch Log", 0.04));
        ITEMS.add(new ShopItem("jungle_log", "Jungle Log", 0.04));
        ITEMS.add(new ShopItem("acacia_log", "Acacia Log", 0.04));
        ITEMS.add(new ShopItem("dark_oak_log", "Dark Oak Log", 0.04));
        ITEMS.add(new ShopItem("mangrove_log", "Mangrove Log", 0.04));
        ITEMS.add(new ShopItem("cherry_log", "Cherry Log", 0.04));
        ITEMS.add(new ShopItem("crimson_stem", "Crimson Stem", 0.05));
        ITEMS.add(new ShopItem("warped_stem", "Warped Stem", 0.05));
        ITEMS.add(new ShopItem("oak_planks", "Oak Planks", 0.01));
        ITEMS.add(new ShopItem("spruce_planks", "Spruce Planks", 0.01));
        ITEMS.add(new ShopItem("birch_planks", "Birch Planks", 0.01));
        ITEMS.add(new ShopItem("stone_bricks", "Stone Bricks", 0.03));
        ITEMS.add(new ShopItem("deepslate_bricks", "Deepslate Bricks", 0.03));
        ITEMS.add(new ShopItem("glass", "Glass", 0.04));
        ITEMS.add(new ShopItem("tinted_glass", "Tinted Glass", 0.08));
        ITEMS.add(new ShopItem("terracotta", "Terracotta", 0.04));
        ITEMS.add(new ShopItem("white_wool", "White Wool", 0.04));
        ITEMS.add(new ShopItem("bone", "Bone", 0.04));
        ITEMS.add(new ShopItem("string", "String", 0.04));
        ITEMS.add(new ShopItem("crafting_table", "Crafting Table", 0.05));
        ITEMS.add(new ShopItem("chest", "Chest", 0.06));
        ITEMS.add(new ShopItem("barrel", "Barrel", 0.06));
        ITEMS.add(new ShopItem("furnace", "Furnace", 0.08));
        ITEMS.add(new ShopItem("blast_furnace", "Blast Furnace", 0.25));
        ITEMS.add(new ShopItem("smoker", "Smoker", 0.12));
        ITEMS.add(new ShopItem("anvil", "Anvil", 1.80));

        // --- Tier 3: Crops, Produce & Food ($0.03 - $1.50) ---
        ITEMS.add(new ShopItem("wheat", "Wheat", 0.03));
        ITEMS.add(new ShopItem("carrot", "Carrot", 0.03));
        ITEMS.add(new ShopItem("potato", "Potato", 0.03));
        ITEMS.add(new ShopItem("beetroot", "Beetroot", 0.03));
        ITEMS.add(new ShopItem("melon_slice", "Melon Slice", 0.02));
        ITEMS.add(new ShopItem("pumpkin", "Pumpkin", 0.05));
        ITEMS.add(new ShopItem("apple", "Apple", 0.06));
        ITEMS.add(new ShopItem("baked_potato", "Baked Potato", 0.06));
        ITEMS.add(new ShopItem("bread", "Bread", 0.07));
        ITEMS.add(new ShopItem("cookie", "Cookie", 0.04));
        ITEMS.add(new ShopItem("pumpkin_pie", "Pumpkin Pie", 0.12));
        ITEMS.add(new ShopItem("raw_beef", "Raw Beef", 0.05));
        ITEMS.add(new ShopItem("cooked_beef", "Cooked Beef", 0.10));
        ITEMS.add(new ShopItem("raw_porkchop", "Raw Porkchop", 0.05));
        ITEMS.add(new ShopItem("cooked_porkchop", "Cooked Porkchop", 0.10));
        ITEMS.add(new ShopItem("raw_chicken", "Raw Chicken", 0.04));
        ITEMS.add(new ShopItem("cooked_chicken", "Cooked Chicken", 0.08));
        ITEMS.add(new ShopItem("raw_mutton", "Raw Mutton", 0.04));
        ITEMS.add(new ShopItem("cooked_mutton", "Cooked Mutton", 0.08));
        ITEMS.add(new ShopItem("raw_cod", "Raw Cod", 0.05));
        ITEMS.add(new ShopItem("cooked_cod", "Cooked Cod", 0.09));
        ITEMS.add(new ShopItem("raw_salmon", "Raw Salmon", 0.06));
        ITEMS.add(new ShopItem("cooked_salmon", "Cooked Salmon", 0.11));
        ITEMS.add(new ShopItem("golden_carrot", "Golden Carrot", 0.35));
        ITEMS.add(new ShopItem("golden_apple", "Golden Apple", 1.20));

        // --- Tier 4: Ores, Gems & Monster Drops ($0.08 - $4.00) ---
        ITEMS.add(new ShopItem("charcoal", "Charcoal", 0.08));
        ITEMS.add(new ShopItem("coal", "Coal", 0.10));
        ITEMS.add(new ShopItem("raw_copper", "Raw Copper", 0.12));
        ITEMS.add(new ShopItem("copper_ingot", "Copper Ingot", 0.15));
        ITEMS.add(new ShopItem("raw_iron", "Raw Iron", 0.20));
        ITEMS.add(new ShopItem("iron_ingot", "Iron Ingot", 0.25));
        ITEMS.add(new ShopItem("raw_gold", "Raw Gold", 0.40));
        ITEMS.add(new ShopItem("gold_ingot", "Gold Ingot", 0.50));
        ITEMS.add(new ShopItem("leather", "Leather", 0.10));
        ITEMS.add(new ShopItem("spider_eye", "Spider Eye", 0.10));
        ITEMS.add(new ShopItem("gunpowder", "Gunpowder", 0.12));
        ITEMS.add(new ShopItem("redstone", "Redstone", 0.15));
        ITEMS.add(new ShopItem("lapis_lazuli", "Lapis Lazuli", 0.15));
        ITEMS.add(new ShopItem("amethyst_shard", "Amethyst Shard", 0.18));
        ITEMS.add(new ShopItem("quartz", "Nether Quartz", 0.20));
        ITEMS.add(new ShopItem("slime_ball", "Slimeball", 0.20));
        ITEMS.add(new ShopItem("magma_cream", "Magma Cream", 0.30));
        ITEMS.add(new ShopItem("blaze_rod", "Blaze Rod", 0.40));
        ITEMS.add(new ShopItem("ender_pearl", "Ender Pearl", 0.35));
        ITEMS.add(new ShopItem("ghast_tear", "Ghast Tear", 0.75));
        ITEMS.add(new ShopItem("diamond", "Diamond", 1.50));
        ITEMS.add(new ShopItem("emerald", "Emerald", 2.00));
        ITEMS.add(new ShopItem("ancient_debris", "Ancient Debris", 3.50));
        ITEMS.add(new ShopItem("netherite_scrap", "Netherite Scrap", 4.00));

        // --- Tier 5: Weapons, Tools & Armor ($0.15 - $25.00) ---
        ITEMS.add(new ShopItem("stone_sword", "Stone Sword", 0.12));
        ITEMS.add(new ShopItem("stone_pickaxe", "Stone Pickaxe", 0.14));
        ITEMS.add(new ShopItem("stone_axe", "Stone Axe", 0.14));
        ITEMS.add(new ShopItem("bow", "Bow", 0.25));
        ITEMS.add(new ShopItem("crossbow", "Crossbow", 0.40));
        ITEMS.add(new ShopItem("shield", "Shield", 0.40));
        ITEMS.add(new ShopItem("iron_shovel", "Iron Shovel", 0.30));
        ITEMS.add(new ShopItem("iron_sword", "Iron Sword", 0.55));
        ITEMS.add(new ShopItem("iron_pickaxe", "Iron Pickaxe", 0.80));
        ITEMS.add(new ShopItem("iron_axe", "Iron Axe", 0.80));
        ITEMS.add(new ShopItem("iron_helmet", "Iron Helmet", 1.30));
        ITEMS.add(new ShopItem("iron_chestplate", "Iron Chestplate", 2.00));
        ITEMS.add(new ShopItem("iron_leggings", "Iron Leggings", 1.80));
        ITEMS.add(new ShopItem("iron_boots", "Iron Boots", 1.00));
        ITEMS.add(new ShopItem("diamond_sword", "Diamond Sword", 3.20));
        ITEMS.add(new ShopItem("diamond_pickaxe", "Diamond Pickaxe", 4.80));
        ITEMS.add(new ShopItem("diamond_axe", "Diamond Axe", 4.80));
        ITEMS.add(new ShopItem("diamond_helmet", "Diamond Helmet", 7.50));
        ITEMS.add(new ShopItem("diamond_chestplate", "Diamond Chestplate", 12.00));
        ITEMS.add(new ShopItem("diamond_leggings", "Diamond Leggings", 10.50));
        ITEMS.add(new ShopItem("diamond_boots", "Diamond Boots", 6.00));
        ITEMS.add(new ShopItem("netherite_ingot", "Netherite Ingot", 12.00));
        ITEMS.add(new ShopItem("netherite_sword", "Netherite Sword", 16.00));
        ITEMS.add(new ShopItem("netherite_pickaxe", "Netherite Pickaxe", 18.00));
        ITEMS.add(new ShopItem("netherite_chestplate", "Netherite Chestplate", 25.00));

        // --- Tier 6: High-Value Blocks, Relics & Boss Artifacts ($1.35 - $500.00) ---
        ITEMS.add(new ShopItem("copper_block", "Copper Block", 1.35));
        ITEMS.add(new ShopItem("iron_block", "Iron Block", 2.25));
        ITEMS.add(new ShopItem("gold_block", "Gold Block", 4.50));
        ITEMS.add(new ShopItem("diamond_block", "Diamond Block", 13.50));
        ITEMS.add(new ShopItem("emerald_block", "Emerald Block", 18.00));
        ITEMS.add(new ShopItem("shulker_shell", "Shulker Shell", 15.00));
        ITEMS.add(new ShopItem("heart_of_the_sea", "Heart of the Sea", 20.00));
        ITEMS.add(new ShopItem("enchanted_golden_apple", "Enchanted Golden Apple", 30.00));
        ITEMS.add(new ShopItem("totem_of_undying", "Totem of Undying", 35.00));
        ITEMS.add(new ShopItem("netherite_block", "Netherite Block", 108.00));
        ITEMS.add(new ShopItem("beacon", "Beacon", 60.00));
        ITEMS.add(new ShopItem("elytra", "Elytra", 150.00));
        ITEMS.add(new ShopItem("nether_star", "Nether Star", 80.00));
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
