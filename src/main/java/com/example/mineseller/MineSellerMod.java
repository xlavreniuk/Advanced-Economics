package com.example.mineseller;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class MineSellerMod implements ModInitializer {
    private static final Path PRICE_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("mineseller-prices.txt");
    private static final int LUDKA_CASHBACK_PERCENT = 5;
    private static final int LUDKA_UNPRICED_CASHBACK_CENTS = 1;
    private static final Map<UUID, Integer> BALANCES = new HashMap<>();
    private static final Map<String, Integer> SELL_PRICES = new HashMap<>();
    private static final Map<Item, ShopItemDefinition> SHOP_ITEMS = new HashMap<>();
    private static final Map<String, String> ITEM_ALIASES = new HashMap<>();
    private static final List<ShopItemDefinition> SHOP_ITEM_DEFINITIONS = new ArrayList<>();
    private static final Random RANDOM = new Random();

    static {
        registerShopItems();
    }

    @Override
    public void onInitialize() {
        loadPriceConfig();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("sell")
                    .executes(context -> sell(context.getSource(), "1"))
                    .then(Commands.argument("amount", StringArgumentType.word())
                            .executes(context -> sell(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "amount")
                            ))));

            dispatcher.register(Commands.literal("sellall")
                    .executes(context -> sell(context.getSource(), "all")));

            dispatcher.register(Commands.literal("buy")
                    .then(Commands.argument("item", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                for (String itemName : ITEM_ALIASES.keySet()) {
                                    builder.suggest(itemName);
                                }
                                return builder.buildFuture();
                            })
                            .executes(context -> buy(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "item"),
                                    "1"
                            ))
                            .then(Commands.argument("amount", StringArgumentType.word())
                                    .executes(context -> buy(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "item"),
                                            StringArgumentType.getString(context, "amount")
                                    )))));

            dispatcher.register(Commands.literal("money")
                    .executes(context -> money(context.getSource()))
                    .then(Commands.literal("help")
                            .executes(context -> help(context.getSource()))));

            dispatcher.register(Commands.literal("price")
                    .executes(context -> price(context.getSource())));

            dispatcher.register(Commands.literal("ludka")
                    .executes(context -> ludka(context.getSource())));
        });
    }

    private static int sell(CommandSourceStack source, String requestedAmount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        ShopItem option = getShopItem(held);

        if (option == null) {
            source.sendFailure(Component.literal("This item is not in the MineSeller price list."));
            return 0;
        }

        int soldCount = Math.min(parseAmount(requestedAmount, held.getCount()), held.getCount());
        if (soldCount <= 0) {
            source.sendFailure(Component.literal("Use /sell <count> or /sell all."));
            return 0;
        }

        int earned = soldCount * option.sellPrice();
        held.shrink(soldCount);
        int balance = addMoney(player, earned);
        source.sendSuccess(() -> Component.literal("Sold " + soldCount + " " + option.displayName() + " for " + formatMoney(earned) + ". Balance: " + formatMoney(balance)), false);
        return soldCount;
    }

    private static int buy(CommandSourceStack source, String requestedItem, String requestedAmount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ShopItem option = getShopItem(requestedItem);

        if (option == null) {
            source.sendFailure(Component.literal("This item is not in the MineSeller price list."));
            return 0;
        }

        int balance = getMoney(player);
        int requestedCount = parseAmount(requestedAmount, option.buyPrice() == 0 ? 0 : balance / option.buyPrice());
        if (requestedCount <= 0) {
            source.sendFailure(Component.literal("Use /buy " + option.primaryName() + " <count> or /buy " + option.primaryName() + " all."));
            return 0;
        }

        int maxAffordable = option.buyPrice() == 0 ? requestedCount : balance / option.buyPrice();
        int targetCount = Math.min(requestedCount, maxAffordable);
        if (targetCount <= 0) {
            source.sendFailure(Component.literal("Not enough money. " + option.displayName() + " costs " + formatMoney(option.buyPrice()) + ". Balance: " + formatMoney(balance)));
            return 0;
        }

        int boughtCount = 0;
        for (int i = 0; i < targetCount; i++) {
            if (!player.getInventory().add(new ItemStack(option.item()))) {
                break;
            }
            boughtCount++;
        }

        if (boughtCount == 0) {
            source.sendFailure(Component.literal("Your inventory is full."));
            return 0;
        }

        int spent = boughtCount * option.buyPrice();
        setMoney(player, balance - spent);
        int finalBoughtCount = boughtCount;
        source.sendSuccess(() -> Component.literal("Bought " + finalBoughtCount + " " + option.displayName() + " for " + formatMoney(spent) + ". Balance: " + formatMoney(getMoney(player))), false);
        return boughtCount;
    }

    private static int money(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.literal("Balance: " + formatMoney(getMoney(player))), false);
        return getMoney(player);
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Commands: /money, /price, /sell <count|all>, /buy <item> <count|all>, /ludka"), false);
        return 1;
    }

    private static int price(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ShopItem option = getShopItem(player.getMainHandItem());

        if (option == null) {
            source.sendFailure(Component.literal("Hold an item from the MineSeller price list to see its price."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(option.displayName() + " price: buy " + formatMoney(option.buyPrice()) + ", sell " + formatMoney(option.sellPrice())), false);
        return 1;
    }

    private static int ludka(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        ShopItem option = getShopItem(held);

        if (held.isEmpty()) {
            source.sendFailure(Component.literal("Hold an item in your main hand before using /ludka."));
            return 0;
        }

        int originalCount = held.getCount();
        Item originalItem = held.getItem();
        int roll = RANDOM.nextInt(100);
        source.sendSuccess(() -> Component.literal("Ludka rolling... under 50 loses."), false);
        runLudkaAnimation(source, player.getUUID(), originalItem, originalCount, option, roll);
        return 1;
    }

    private static ShopItem getShopItem(ItemStack stack) {
        ShopItemDefinition definition = SHOP_ITEMS.get(stack.getItem());
        return definition == null ? null : definition.toShopItem();
    }

    private static ShopItem getShopItem(String requestedItem) {
        String normalized = requestedItem.toLowerCase(Locale.ROOT);
        String itemName = ITEM_ALIASES.get(normalized);
        ShopItemDefinition definition = itemName == null ? null : getDefinition(itemName);
        return definition == null ? null : definition.toShopItem();
    }

    private static int getMoney(ServerPlayer player) {
        return BALANCES.getOrDefault(player.getUUID(), 0);
    }

    private static int addMoney(ServerPlayer player, int amount) {
        int balance = getMoney(player) + amount;
        setMoney(player, balance);
        return balance;
    }

    private static void setMoney(ServerPlayer player, int balance) {
        BALANCES.put(player.getUUID(), balance);
    }

    private static int parseAmount(String requestedAmount, int allAmount) {
        String normalized = requestedAmount.toLowerCase(Locale.ROOT);
        if (normalized.equals("all")) {
            return allAmount;
        }

        try {
            return Math.max(0, Integer.parseInt(normalized));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static String formatMoney(int units) {
        return "$" + (units / 100) + "." + String.format(Locale.ROOT, "%02d", Math.abs(units % 100));
    }

    private static void runLudkaAnimation(CommandSourceStack source, UUID playerId, Item originalItem, int originalCount, ShopItem option, int roll) {
        Thread animationThread = new Thread(() -> {
            for (int i = 0; i < 6; i++) {
                int shownRoll = i == 5 ? roll : RANDOM.nextInt(100);
                sleep(350L);
                source.getServer().execute(() -> source.sendSuccess(() -> Component.literal("Ludka roll: " + shownRoll), false));
            }

            sleep(350L);
            source.getServer().execute(() -> finishLudka(source, playerId, originalItem, originalCount, option, roll));
        }, "MineSeller Ludka Roll");
        animationThread.setDaemon(true);
        animationThread.start();
    }

    private static void finishLudka(CommandSourceStack source, UUID playerId, Item originalItem, int originalCount, ShopItem option, int roll) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayer(playerId);
        if (player == null) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || held.getItem() != originalItem || held.getCount() != originalCount) {
            source.sendFailure(Component.literal("Ludka cancelled because the held item changed."));
            return;
        }

        if (roll < 50) {
            int lostCount = held.getCount();
            held.setCount(0);
            int cashback = getLudkaCashback(option, lostCount);
            int balance = addMoney(player, cashback);
            source.sendSuccess(() -> Component.literal("Ludka result: " + roll + ". You lose, but got " + formatMoney(cashback) + " cashback. Balance: " + formatMoney(balance)), false);
            return;
        }

        int maxCount = held.getMaxStackSize();
        if (maxCount == 1) {
            ItemStack extraItem = held.copy();
            extraItem.setCount(1);
            if (player.getInventory().add(extraItem)) {
                source.sendSuccess(() -> Component.literal("Ludka result: " + roll + ". You win. You got one more " + held.getDisplayName().getString() + "."), false);
            } else {
                source.sendSuccess(() -> Component.literal("Ludka result: " + roll + ". You win, but your inventory is full."), false);
            }
            return;
        }

        int doubledCount = Math.min(originalCount * 2, maxCount);
        held.setCount(doubledCount);

        if (doubledCount == originalCount) {
            source.sendSuccess(() -> Component.literal("Ludka result: " + roll + ". You win, but this stack is already full."), false);
        } else {
            source.sendSuccess(() -> Component.literal("Ludka result: " + roll + ". You win. Your stack doubled to " + doubledCount + "."), false);
        }
    }

    private static int getLudkaCashback(ShopItem option, int lostCount) {
        if (option == null || option.sellPrice() <= 0) {
            return LUDKA_UNPRICED_CASHBACK_CENTS;
        }

        return Math.max(1, lostCount * option.sellPrice() * LUDKA_CASHBACK_PERCENT / 100);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static void loadPriceConfig() {
        resetDefaultSellPrices();
        createDefaultPriceConfigIfMissing();

        try {
            List<String> lines = Files.readAllLines(PRICE_CONFIG_PATH);
            for (String line : lines) {
                loadPriceConfigLine(line);
            }
            appendMissingPriceConfigLines(lines);
        } catch (IOException exception) {
            System.err.println("MineSeller could not read " + PRICE_CONFIG_PATH + ": " + exception.getMessage());
        }
    }

    private static void resetDefaultSellPrices() {
        SELL_PRICES.clear();
        for (ShopItemDefinition definition : SHOP_ITEM_DEFINITIONS) {
            SELL_PRICES.put(definition.primaryName(), definition.defaultSellPrice());
        }
    }

    private static void createDefaultPriceConfigIfMissing() {
        if (Files.exists(PRICE_CONFIG_PATH)) {
            return;
        }

        try {
            Files.createDirectories(PRICE_CONFIG_PATH.getParent());
            Files.write(PRICE_CONFIG_PATH, defaultPriceConfigLines());
        } catch (IOException exception) {
            System.err.println("MineSeller could not create " + PRICE_CONFIG_PATH + ": " + exception.getMessage());
        }
    }

    private static List<String> defaultPriceConfigLines() {
        List<String> lines = new ArrayList<>();
        lines.add("# MineSeller sell prices.");
        lines.add("# Edit these values, then restart the server.");
        lines.add("# Buy prices are always 3x the sell price.");
        lines.add("# Values are dollars, so 0.01 means one cent.");
        for (ShopItemDefinition definition : SHOP_ITEM_DEFINITIONS) {
            lines.add(definition.primaryName() + "=" + formatMoneyValue(definition.defaultSellPrice()));
        }
        return lines;
    }

    private static void appendMissingPriceConfigLines(List<String> lines) {
        List<String> existingItems = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            String[] parts = trimmed.split("=", 2);
            if (parts.length != 2) {
                continue;
            }

            String itemName = ITEM_ALIASES.get(parts[0].trim().toLowerCase(Locale.ROOT));
            existingItems.add(itemName == null ? parts[0].trim().toLowerCase(Locale.ROOT) : itemName);
        }

        List<String> missingLines = new ArrayList<>();
        for (ShopItemDefinition definition : SHOP_ITEM_DEFINITIONS) {
            if (!existingItems.contains(definition.primaryName())) {
                missingLines.add(definition.primaryName() + "=" + formatMoneyValue(definition.defaultSellPrice()));
            }
        }

        if (missingLines.isEmpty()) {
            return;
        }

        try {
            List<String> updatedLines = new ArrayList<>(lines);
            updatedLines.add("");
            updatedLines.add("# Added by MineSeller after an update.");
            updatedLines.addAll(missingLines);
            Files.write(PRICE_CONFIG_PATH, updatedLines);
        } catch (IOException exception) {
            System.err.println("MineSeller could not update " + PRICE_CONFIG_PATH + ": " + exception.getMessage());
        }
    }

    private static void loadPriceConfigLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }

        String[] parts = trimmed.split("=", 2);
        if (parts.length != 2) {
            System.err.println("MineSeller ignored invalid price line: " + line);
            return;
        }

        String itemName = ITEM_ALIASES.get(parts[0].trim().toLowerCase(Locale.ROOT));
        if (itemName == null) {
            itemName = parts[0].trim().toLowerCase(Locale.ROOT);
        }

        if (!SELL_PRICES.containsKey(itemName)) {
            System.err.println("MineSeller ignored unknown price item: " + parts[0].trim());
            return;
        }

        try {
            SELL_PRICES.put(itemName, parseMoney(parts[1].trim()));
        } catch (RuntimeException exception) {
            System.err.println("MineSeller ignored invalid price for " + itemName + ": " + parts[1].trim());
        }
    }

    private static int parseMoney(String value) {
        BigDecimal dollars = new BigDecimal(value);
        int cents = dollars.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
        if (cents < 0) {
            throw new NumberFormatException("Price cannot be negative");
        }
        return cents;
    }

    private static int sellPrice(String itemName) {
        return SELL_PRICES.getOrDefault(itemName, 0);
    }

    private static String formatMoneyValue(int cents) {
        return (cents / 100) + "." + String.format(Locale.ROOT, "%02d", Math.abs(cents % 100));
    }

    private static ShopItemDefinition getDefinition(String primaryName) {
        for (ShopItemDefinition definition : SHOP_ITEM_DEFINITIONS) {
            if (definition.primaryName().equals(primaryName)) {
                return definition;
            }
        }
        return null;
    }

    private static void registerShopItems() {
        register(Items.DIRT, "dirt", "dirt", 1, "dirst", "minecraft:dirt");
        register(Items.GRASS_BLOCK, "grass block", "grass_block", 2, "minecraft:grass_block");
        register(Items.STONE, "stone", "stone", 50, "minecraft:stone");
        register(Items.COBBLESTONE, "cobblestone", "cobblestone", 3, "cobble", "minecraft:cobblestone");
        register(Items.DEEPSLATE, "deepslate", "deepslate", 4, "minecraft:deepslate");
        register(Items.COBBLED_DEEPSLATE, "cobbled deepslate", "cobbled_deepslate", 4, "minecraft:cobbled_deepslate");
        register(Items.GRANITE, "granite", "granite", 3, "minecraft:granite");
        register(Items.DIORITE, "diorite", "diorite", 3, "minecraft:diorite");
        register(Items.ANDESITE, "andesite", "andesite", 3, "minecraft:andesite");
        register(Items.SAND, "sand", "sand", 3, "minecraft:sand");
        register(Items.RED_SAND, "red sand", "red_sand", 3, "minecraft:red_sand");
        register(Items.GRAVEL, "gravel", "gravel", 3, "minecraft:gravel");
        register(Items.CLAY_BALL, "clay ball", "clay_ball", 5, "clay", "minecraft:clay_ball");
        register(Items.FLINT, "flint", "flint", 6, "minecraft:flint");

        register(Items.OAK_LOG, "oak log", "oak_log", 10, "log", "logs", "wood", "wood_log", "minecraft:oak_log");
        register(Items.SPRUCE_LOG, "spruce log", "spruce_log", 10, "minecraft:spruce_log");
        register(Items.BIRCH_LOG, "birch log", "birch_log", 10, "minecraft:birch_log");
        register(Items.JUNGLE_LOG, "jungle log", "jungle_log", 10, "minecraft:jungle_log");
        register(Items.ACACIA_LOG, "acacia log", "acacia_log", 10, "minecraft:acacia_log");
        register(Items.DARK_OAK_LOG, "dark oak log", "dark_oak_log", 10, "minecraft:dark_oak_log");
        register(Items.MANGROVE_LOG, "mangrove log", "mangrove_log", 10, "minecraft:mangrove_log");
        register(Items.CHERRY_LOG, "cherry log", "cherry_log", 10, "minecraft:cherry_log");
        register(Items.OAK_PLANKS, "oak planks", "oak_planks", 3, "planks", "plank", "wood_planks", "wood_panel", "wood_panels", "minecraft:oak_planks");
        register(Items.SPRUCE_PLANKS, "spruce planks", "spruce_planks", 3, "minecraft:spruce_planks");
        register(Items.BIRCH_PLANKS, "birch planks", "birch_planks", 3, "minecraft:birch_planks");
        register(Items.JUNGLE_PLANKS, "jungle planks", "jungle_planks", 3, "minecraft:jungle_planks");
        register(Items.ACACIA_PLANKS, "acacia planks", "acacia_planks", 3, "minecraft:acacia_planks");
        register(Items.DARK_OAK_PLANKS, "dark oak planks", "dark_oak_planks", 3, "minecraft:dark_oak_planks");
        register(Items.MANGROVE_PLANKS, "mangrove planks", "mangrove_planks", 3, "minecraft:mangrove_planks");
        register(Items.CHERRY_PLANKS, "cherry planks", "cherry_planks", 3, "minecraft:cherry_planks");
        register(Items.STICK, "stick", "stick", 1, "sticks", "minecraft:stick");

        register(Items.COAL, "coal", "coal", 20, "minecraft:coal");
        register(Items.CHARCOAL, "charcoal", "charcoal", 15, "minecraft:charcoal");
        register(Items.RAW_COPPER, "raw copper", "raw_copper", 20, "minecraft:raw_copper");
        register(Items.COPPER_INGOT, "copper ingot", "copper_ingot", 30, "copper", "minecraft:copper_ingot");
        register(Items.RAW_IRON, "raw iron", "raw_iron", 60, "minecraft:raw_iron");
        register(Items.IRON_NUGGET, "iron nugget", "iron_nugget", 10, "minecraft:iron_nugget");
        register(Items.IRON_INGOT, "iron ingot", "iron_ingot", 90, "iron", "iron_igon", "minecraft:iron_ingot");
        register(Items.RAW_GOLD, "raw gold", "raw_gold", 60, "minecraft:raw_gold");
        register(Items.GOLD_NUGGET, "gold nugget", "gold_nugget", 10, "minecraft:gold_nugget");
        register(Items.GOLD_INGOT, "gold ingot", "gold_ingot", 90, "gold", "minecraft:gold_ingot");
        register(Items.REDSTONE, "redstone", "redstone", 15, "redstone_dust", "minecraft:redstone");
        register(Items.LAPIS_LAZULI, "lapis lazuli", "lapis_lazuli", 20, "lapis", "minecraft:lapis_lazuli");
        register(Items.QUARTZ, "quartz", "quartz", 25, "minecraft:quartz");
        register(Items.AMETHYST_SHARD, "amethyst shard", "amethyst_shard", 40, "amethyst", "minecraft:amethyst_shard");
        register(Items.DIAMOND, "diamond", "diamond", 500, "minecraft:diamond");
        register(Items.EMERALD, "emerald", "emerald", 400, "minecraft:emerald");

        register(Items.COAL_ORE, "coal ore", "coal_ore", 25, "minecraft:coal_ore");
        register(Items.COPPER_ORE, "copper ore", "copper_ore", 25, "minecraft:copper_ore");
        register(Items.IRON_ORE, "iron ore", "iron_ore", 70, "minecraft:iron_ore");
        register(Items.GOLD_ORE, "gold ore", "gold_ore", 70, "minecraft:gold_ore");
        register(Items.REDSTONE_ORE, "redstone ore", "redstone_ore", 50, "minecraft:redstone_ore");
        register(Items.LAPIS_ORE, "lapis ore", "lapis_ore", 60, "minecraft:lapis_ore");
        register(Items.DIAMOND_ORE, "diamond ore", "diamond_ore", 600, "minecraft:diamond_ore");
        register(Items.EMERALD_ORE, "emerald ore", "emerald_ore", 500, "minecraft:emerald_ore");
        register(Items.NETHER_QUARTZ_ORE, "nether quartz ore", "nether_quartz_ore", 40, "minecraft:nether_quartz_ore");
        register(Items.ANCIENT_DEBRIS, "ancient debris", "ancient_debris", 1200, "minecraft:ancient_debris");

        register(Items.GLASS, "glass", "glass", 5, "minecraft:glass");
        register(Items.TORCH, "torch", "torch", 3, "torches", "minecraft:torch");
        register(Items.CRAFTING_TABLE, "crafting table", "crafting_table", 8, "minecraft:crafting_table");
        register(Items.FURNACE, "furnace", "furnace", 12, "minecraft:furnace");
        register(Items.CHEST, "chest", "chest", 15, "minecraft:chest");

        register(Items.WHEAT_SEEDS, "wheat seeds", "wheat_seeds", 1, "seeds", "minecraft:wheat_seeds");
        register(Items.WHEAT, "wheat", "wheat", 5, "minecraft:wheat");
        register(Items.BREAD, "bread", "bread", 15, "minecraft:bread");
        register(Items.APPLE, "apple", "apple", 10, "minecraft:apple");
        register(Items.CARROT, "carrot", "carrot", 7, "minecraft:carrot");
        register(Items.POTATO, "potato", "potato", 7, "minecraft:potato");
        register(Items.BEEF, "raw beef", "beef", 15, "raw_beef", "minecraft:beef");
        register(Items.COOKED_BEEF, "steak", "cooked_beef", 25, "steak", "minecraft:cooked_beef");

        register(Items.LEATHER, "leather", "leather", 20, "minecraft:leather");
        register(Items.FEATHER, "feather", "feather", 8, "minecraft:feather");
        register(Items.STRING, "string", "string", 8, "minecraft:string");
        register(Items.BONE, "bone", "bone", 10, "minecraft:bone");
        register(Items.GUNPOWDER, "gunpowder", "gunpowder", 25, "minecraft:gunpowder");
        register(Items.ROTTEN_FLESH, "rotten flesh", "rotten_flesh", 3, "minecraft:rotten_flesh");
        register(Items.SPIDER_EYE, "spider eye", "spider_eye", 15, "minecraft:spider_eye");
        register(Items.ENDER_PEARL, "ender pearl", "ender_pearl", 100, "minecraft:ender_pearl");
        register(Items.BLAZE_ROD, "blaze rod", "blaze_rod", 120, "minecraft:blaze_rod");
        register(Items.SLIME_BALL, "slimeball", "slime_ball", 30, "slimeball", "minecraft:slime_ball");

        register(Items.WOODEN_PICKAXE, "wooden pickaxe", "wooden_pickaxe", 15, "minecraft:wooden_pickaxe");
        register(Items.STONE_PICKAXE, "stone pickaxe", "stone_pickaxe", 25, "minecraft:stone_pickaxe");
        register(Items.IRON_PICKAXE, "iron pickaxe", "iron_pickaxe", 300, "minecraft:iron_pickaxe");
        register(Items.GOLDEN_PICKAXE, "golden pickaxe", "golden_pickaxe", 300, "gold_pickaxe", "minecraft:golden_pickaxe");
        register(Items.DIAMOND_PICKAXE, "diamond pickaxe", "diamond_pickaxe", 1600, "minecraft:diamond_pickaxe");
    }

    private static void register(Item item, String displayName, String primaryName, int defaultSellPrice, String... aliases) {
        ShopItemDefinition definition = new ShopItemDefinition(item, displayName, primaryName, defaultSellPrice);
        SHOP_ITEMS.put(item, definition);
        SHOP_ITEM_DEFINITIONS.add(definition);
        ITEM_ALIASES.put(primaryName, primaryName);
        ITEM_ALIASES.put("minecraft:" + primaryName, primaryName);
        for (String alias : aliases) {
            ITEM_ALIASES.put(alias, primaryName);
        }
    }

    private record ShopItemDefinition(Item item, String displayName, String primaryName, int defaultSellPrice) {
        private ShopItem toShopItem() {
            return new ShopItem(item, displayName, primaryName, sellPrice(primaryName));
        }
    }

    private record ShopItem(Item item, String displayName, String primaryName, int sellPrice) {
        private int buyPrice() {
            return sellPrice * 3;
        }
    }
}
