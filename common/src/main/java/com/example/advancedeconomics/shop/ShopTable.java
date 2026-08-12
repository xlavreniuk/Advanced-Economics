package com.example.advancedeconomics.shop;

import java.util.*;

/**
 * Universal Marketplace Table for Advanced Economics (v0.41).
 * Stores loader-agnostic explicit prices and accepts dynamically registered items from registry scanning.
 */
public class ShopTable {

    private static final List<ShopItem> ITEMS = new ArrayList<>();
    private static final Map<String, ShopItem> ITEM_MAP = new HashMap<>();
    private static final Set<String> PROCESSED_IDS = new HashSet<>();

    static {
        // --- Tier 1: Abundant & Basic Resources ($0.01 - $0.03) ---
        addExplicit("stick", "Stick", 0.01);
        addExplicit("dirt", "Dirt", 0.01);
        addExplicit("coarse_dirt", "Coarse Dirt", 0.01);
        addExplicit("sand", "Sand", 0.01);
        addExplicit("red_sand", "Red Sand", 0.01);
        addExplicit("gravel", "Gravel", 0.01);
        addExplicit("wheat_seeds", "Wheat Seeds", 0.01);
        addExplicit("pumpkin_seeds", "Pumpkin Seeds", 0.01);
        addExplicit("melon_seeds", "Melon Seeds", 0.01);
        addExplicit("beetroot_seeds", "Beetroot Seeds", 0.01);
        addExplicit("cobblestone", "Cobblestone", 0.01);
        addExplicit("cobbled_deepslate", "Cobbled Deepslate", 0.01);
        addExplicit("stone", "Stone", 0.01);
        addExplicit("granite", "Granite", 0.01);
        addExplicit("diorite", "Diorite", 0.01);
        addExplicit("andesite", "Andesite", 0.01);
        addExplicit("tuff", "Tuff", 0.01);
        addExplicit("calcite", "Calcite", 0.01);
        addExplicit("basalt", "Basalt", 0.01);
        addExplicit("blackstone", "Blackstone", 0.01);
        addExplicit("grass_block", "Grass Block", 0.02);
        addExplicit("moss_block", "Moss Block", 0.02);
        addExplicit("sugar_cane", "Sugar Cane", 0.02);
        addExplicit("bamboo", "Bamboo", 0.02);
        addExplicit("cactus", "Cactus", 0.02);
        addExplicit("sweet_berries", "Sweet Berries", 0.02);
        addExplicit("glow_berries", "Glow Berries", 0.02);
        addExplicit("torch", "Torch", 0.02);
        addExplicit("soul_torch", "Soul Torch", 0.02);
        addExplicit("rotten_flesh", "Rotten Flesh", 0.02);
        addExplicit("feather", "Feather", 0.03);
        addExplicit("arrow", "Arrow", 0.03);

        // --- Tier 2: Wood, Construction & Crafted Blocks ($0.02 - $0.15) ---
        addExplicit("oak_log", "Oak Log", 0.04);
        addExplicit("spruce_log", "Spruce Log", 0.04);
        addExplicit("birch_log", "Birch Log", 0.04);
        addExplicit("jungle_log", "Jungle Log", 0.04);
        addExplicit("acacia_log", "Acacia Log", 0.04);
        addExplicit("dark_oak_log", "Dark Oak Log", 0.04);
        addExplicit("mangrove_log", "Mangrove Log", 0.04);
        addExplicit("cherry_log", "Cherry Log", 0.04);
        addExplicit("crimson_stem", "Crimson Stem", 0.05);
        addExplicit("warped_stem", "Warped Stem", 0.05);
        addExplicit("oak_planks", "Oak Planks", 0.01);
        addExplicit("spruce_planks", "Spruce Planks", 0.01);
        addExplicit("birch_planks", "Birch Planks", 0.01);
        addExplicit("stone_bricks", "Stone Bricks", 0.03);
        addExplicit("deepslate_bricks", "Deepslate Bricks", 0.03);
        addExplicit("glass", "Glass", 0.04);
        addExplicit("tinted_glass", "Tinted Glass", 0.08);
        addExplicit("terracotta", "Terracotta", 0.04);
        addExplicit("white_wool", "White Wool", 0.04);
        addExplicit("bone", "Bone", 0.04);
        addExplicit("string", "String", 0.04);
        addExplicit("crafting_table", "Crafting Table", 0.05);
        addExplicit("chest", "Chest", 0.06);
        addExplicit("barrel", "Barrel", 0.06);
        addExplicit("furnace", "Furnace", 0.08);
        addExplicit("blast_furnace", "Blast Furnace", 0.25);
        addExplicit("smoker", "Smoker", 0.12);
        addExplicit("anvil", "Anvil", 1.80);

        // --- Tier 3: Crops, Produce & Food ($0.03 - $1.50) ---
        addExplicit("wheat", "Wheat", 0.03);
        addExplicit("carrot", "Carrot", 0.03);
        addExplicit("potato", "Potato", 0.03);
        addExplicit("beetroot", "Beetroot", 0.03);
        addExplicit("melon_slice", "Melon Slice", 0.02);
        addExplicit("pumpkin", "Pumpkin", 0.05);
        addExplicit("apple", "Apple", 0.06);
        addExplicit("baked_potato", "Baked Potato", 0.06);
        addExplicit("bread", "Bread", 0.07);
        addExplicit("cookie", "Cookie", 0.04);
        addExplicit("pumpkin_pie", "Pumpkin Pie", 0.12);
        addExplicit("raw_beef", "Raw Beef", 0.05);
        addExplicit("cooked_beef", "Cooked Beef", 0.10);
        addExplicit("raw_porkchop", "Raw Porkchop", 0.05);
        addExplicit("cooked_porkchop", "Cooked Porkchop", 0.10);
        addExplicit("raw_chicken", "Raw Chicken", 0.04);
        addExplicit("cooked_chicken", "Cooked Chicken", 0.08);
        addExplicit("raw_mutton", "Raw Mutton", 0.04);
        addExplicit("cooked_mutton", "Cooked Mutton", 0.08);
        addExplicit("raw_cod", "Raw Cod", 0.05);
        addExplicit("cooked_cod", "Cooked Cod", 0.09);
        addExplicit("raw_salmon", "Raw Salmon", 0.06);
        addExplicit("cooked_salmon", "Cooked Salmon", 0.11);
        addExplicit("golden_carrot", "Golden Carrot", 0.35);
        addExplicit("golden_apple", "Golden Apple", 1.20);

        // --- Tier 4: Ores, Gems & Monster Drops ($0.08 - $4.00) ---
        addExplicit("charcoal", "Charcoal", 0.08);
        addExplicit("coal", "Coal", 0.10);
        addExplicit("raw_copper", "Raw Copper", 0.12);
        addExplicit("copper_ingot", "Copper Ingot", 0.15);
        addExplicit("raw_iron", "Raw Iron", 0.20);
        addExplicit("iron_ingot", "Iron Ingot", 0.25);
        addExplicit("raw_gold", "Raw Gold", 0.40);
        addExplicit("gold_ingot", "Gold Ingot", 0.50);
        addExplicit("leather", "Leather", 0.10);
        addExplicit("spider_eye", "Spider Eye", 0.10);
        addExplicit("gunpowder", "Gunpowder", 0.12);
        addExplicit("redstone", "Redstone", 0.15);
        addExplicit("lapis_lazuli", "Lapis Lazuli", 0.15);
        addExplicit("amethyst_shard", "Amethyst Shard", 0.18);
        addExplicit("quartz", "Nether Quartz", 0.20);
        addExplicit("slime_ball", "Slimeball", 0.20);
        addExplicit("magma_cream", "Magma Cream", 0.30);
        addExplicit("blaze_rod", "Blaze Rod", 0.40);
        addExplicit("ender_pearl", "Ender Pearl", 0.35);
        addExplicit("ghast_tear", "Ghast Tear", 0.75);
        addExplicit("diamond", "Diamond", 1.50);
        addExplicit("emerald", "Emerald", 2.00);
        addExplicit("ancient_debris", "Ancient Debris", 3.50);
        addExplicit("netherite_scrap", "Netherite Scrap", 4.00);

        // --- Tier 5: Weapons, Tools & Armor ($0.15 - $25.00) ---
        addExplicit("stone_sword", "Stone Sword", 0.12);
        addExplicit("stone_pickaxe", "Stone Pickaxe", 0.14);
        addExplicit("stone_axe", "Stone Axe", 0.14);
        addExplicit("bow", "Bow", 0.25);
        addExplicit("crossbow", "Crossbow", 0.40);
        addExplicit("shield", "Shield", 0.40);
        addExplicit("iron_shovel", "Iron Shovel", 0.30);
        addExplicit("iron_sword", "Iron Sword", 0.55);
        addExplicit("iron_pickaxe", "Iron Pickaxe", 0.80);
        addExplicit("iron_axe", "Iron Axe", 0.80);
        addExplicit("iron_helmet", "Iron Helmet", 1.30);
        addExplicit("iron_chestplate", "Iron Chestplate", 2.00);
        addExplicit("iron_leggings", "Iron Leggings", 1.80);
        addExplicit("iron_boots", "Iron Boots", 1.00);
        addExplicit("diamond_sword", "Diamond Sword", 3.20);
        addExplicit("diamond_pickaxe", "Diamond Pickaxe", 4.80);
        addExplicit("diamond_axe", "Diamond Axe", 4.80);
        addExplicit("diamond_helmet", "Diamond Helmet", 7.50);
        addExplicit("diamond_chestplate", "Diamond Chestplate", 12.00);
        addExplicit("diamond_leggings", "Diamond Leggings", 10.50);
        addExplicit("diamond_boots", "Diamond Boots", 6.00);
        addExplicit("netherite_ingot", "Netherite Ingot", 12.00);
        addExplicit("netherite_sword", "Netherite Sword", 16.00);
        addExplicit("netherite_pickaxe", "Netherite Pickaxe", 18.00);
        addExplicit("netherite_chestplate", "Netherite Chestplate", 25.00);

        // --- Tier 6: High-Value Blocks, Relics & Boss Artifacts ($1.35 - $500.00) ---
        addExplicit("copper_block", "Copper Block", 1.35);
        addExplicit("iron_block", "Iron Block", 2.25);
        addExplicit("gold_block", "Gold Block", 4.50);
        addExplicit("diamond_block", "Diamond Block", 13.50);
        addExplicit("emerald_block", "Emerald Block", 18.00);
        addExplicit("shulker_shell", "Shulker Shell", 15.00);
        addExplicit("heart_of_the_sea", "Heart of the Sea", 20.00);
        addExplicit("enchanted_golden_apple", "Enchanted Golden Apple", 30.00);
        addExplicit("totem_of_undying", "Totem of Undying", 35.00);
        addExplicit("netherite_block", "Netherite Block", 108.00);
        addExplicit("beacon", "Beacon", 60.00);
        addExplicit("elytra", "Elytra", 150.00);
        addExplicit("nether_star", "Nether Star", 80.00);
        addExplicit("dragon_egg", "Dragon Egg", 500.00);
    }

    private static void addExplicit(String id, String displayName, double price) {
        ShopItem item = new ShopItem(id, displayName, price);
        ITEMS.add(item);
        ITEM_MAP.put(id, item);
        PROCESSED_IDS.add(id);
    }

    public static synchronized void registerDynamicItem(String id, String displayName, double price) {
        if (id == null || PROCESSED_IDS.contains(id.trim().toLowerCase())) return;
        String cleanId = id.trim().toLowerCase();
        ShopItem item = new ShopItem(cleanId, displayName, price);
        ITEMS.add(item);
        ITEM_MAP.put(cleanId, item);
        PROCESSED_IDS.add(cleanId);
    }

    public static boolean isProcessed(String id) {
        return id != null && PROCESSED_IDS.contains(id.trim().toLowerCase());
    }

    public static List<ShopItem> getItems() {
        return Collections.unmodifiableList(ITEMS);
    }

    public static ShopItem findById(String id) {
        if (id == null) return null;
        return ITEM_MAP.get(id.trim().toLowerCase());
    }
}
