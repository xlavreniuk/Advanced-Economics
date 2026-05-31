package com.example.advancedeconomics;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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

public class AdvancedEconomicsMod implements ModInitializer {
    private static final Path PRICE_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("advanced-economics-prices.txt");
    private static final int LUDKA_CASHBACK_PERCENT = 5;
    private static final int LUDKA_UNPRICED_CASHBACK_CENTS = 1;
    private static final int LUDKA_WIN_CHANCE_PERCENT = 40;
    private static final BigDecimal INVESTMENT_DAILY_MULTIPLIER = new BigDecimal("1.01");
    private static final Map<UUID, Integer> BALANCES = new HashMap<>();
    private static final Map<UUID, Integer> INVESTMENTS = new HashMap<>();
    private static final Map<String, Integer> SELL_PRICES = new HashMap<>();
    private static final Map<Item, ShopItemDefinition> SHOP_ITEMS = new HashMap<>();
    private static final Map<String, String> ITEM_ALIASES = new HashMap<>();
    private static final List<ShopItemDefinition> SHOP_ITEM_DEFINITIONS = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static long lastInvestmentDay = -1L;

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
                            .executes(context -> help(context.getSource())))
                    .then(Commands.literal("send")
                            .then(Commands.argument("player", EntityArgument.player())
                                    .then(Commands.argument("amount", StringArgumentType.word())
                                            .executes(context -> sendMoney(
                                                    context.getSource(),
                                                    EntityArgument.getPlayer(context, "player"),
                                                    StringArgumentType.getString(context, "amount")
                                            ))))));

            dispatcher.register(Commands.literal("price")
                    .executes(context -> price(context.getSource())));

            dispatcher.register(Commands.literal("prices")
                    .executes(context -> prices(context.getSource())));

            dispatcher.register(Commands.literal("invest")
                    .then(Commands.literal("balance")
                            .executes(context -> investmentBalance(context.getSource())))
                    .then(Commands.literal("withdraw")
                            .executes(context -> withdrawInvestment(context.getSource())))
                    .then(Commands.argument("amount", StringArgumentType.word())
                            .executes(context -> invest(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "amount")
                            ))));

            dispatcher.register(Commands.literal("ludka")
                    .executes(context -> ludka(context.getSource())));
        });

        ServerTickEvents.END_SERVER_TICK.register(AdvancedEconomicsMod::growInvestments);
    }

    private static int sell(CommandSourceStack source, String requestedAmount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (requestedAmount.equalsIgnoreCase("inventory")) {
            return sellInventory(source, player);
        }

        ItemStack held = player.getMainHandItem();
        ShopItem option = getShopItem(held);

        if (option == null) {
            source.sendFailure(Component.literal("This item is not in the Advanced Economics price list."));
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

    private static int sellInventory(CommandSourceStack source, ServerPlayer player) {
        int soldCount = 0;
        int earned = 0;

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            ShopItem option = getShopItem(stack);
            if (option == null) {
                continue;
            }

            int count = stack.getCount();
            soldCount += count;
            earned += count * option.sellPrice();
            stack.setCount(0);
        }

        if (soldCount == 0) {
            source.sendFailure(Component.literal("No sellable items found in your inventory."));
            return 0;
        }

        int balance = addMoney(player, earned);
        int finalSoldCount = soldCount;
        int finalEarned = earned;
        source.sendSuccess(() -> Component.literal("Sold " + finalSoldCount + " inventory items for " + formatMoney(finalEarned) + ". Balance: " + formatMoney(balance)), false);
        return soldCount;
    }

    private static int buy(CommandSourceStack source, String requestedItem, String requestedAmount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ShopItem option = getShopItem(requestedItem);

        if (option == null) {
            source.sendFailure(Component.literal("This item is not in the Advanced Economics price list."));
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

    private static int sendMoney(CommandSourceStack source, ServerPlayer target, String requestedAmount) throws CommandSyntaxException {
        ServerPlayer sender = source.getPlayerOrException();
        if (sender.getUUID().equals(target.getUUID())) {
            source.sendFailure(Component.literal("You cannot send money to yourself."));
            return 0;
        }

        int senderBalance = getMoney(sender);
        int amount = parseMoneyAmount(requestedAmount, senderBalance);
        if (amount <= 0) {
            source.sendFailure(Component.literal("Use /money send <player> <amount> or /money send <player> all."));
            return 0;
        }

        if (amount > senderBalance) {
            source.sendFailure(Component.literal("Not enough money. Balance: " + formatMoney(senderBalance)));
            return 0;
        }

        setMoney(sender, senderBalance - amount);
        int targetBalance = addMoney(target, amount);
        source.sendSuccess(() -> Component.literal("Sent " + formatMoney(amount) + " to " + target.getName().getString() + ". Balance: " + formatMoney(getMoney(sender))), false);
        target.sendSystemMessage(Component.literal(sender.getName().getString() + " sent you " + formatMoney(amount) + ". Balance: " + formatMoney(targetBalance)));
        return amount;
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Commands: /money, /money send <player> <amount|all>, /price, /prices, /sell <count|all|inventory>, /buy <item> <count|all>, /invest <amount|all>, /invest balance, /invest withdraw, /ludka"), false);
        return 1;
    }

    private static int price(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ShopItem option = getShopItem(player.getMainHandItem());

        if (option == null) {
            source.sendFailure(Component.literal("Hold an item from the Advanced Economics price list to see its price."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(option.displayName() + " price: buy " + formatMoney(option.buyPrice()) + ", sell " + formatMoney(option.sellPrice())), false);
        return 1;
    }

    private static int prices(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Advanced Economics prices: sell / buy"), false);
        StringBuilder line = new StringBuilder();
        for (ShopItemDefinition definition : SHOP_ITEM_DEFINITIONS) {
            ShopItem item = definition.toShopItem();
            String entry = item.primaryName() + " " + formatMoney(item.sellPrice()) + "/" + formatMoney(item.buyPrice());
            if (line.length() + entry.length() + 2 > 220) {
                String message = line.toString();
                source.sendSuccess(() -> Component.literal(message), false);
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(", ");
            }
            line.append(entry);
        }

        if (!line.isEmpty()) {
            String message = line.toString();
            source.sendSuccess(() -> Component.literal(message), false);
        }
        return SHOP_ITEM_DEFINITIONS.size();
    }

    private static int invest(CommandSourceStack source, String requestedAmount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int balance = getMoney(player);
        int amount = parseMoneyAmount(requestedAmount, balance);
        if (amount <= 0) {
            source.sendFailure(Component.literal("Use /invest <amount> or /invest all."));
            return 0;
        }

        if (amount > balance) {
            source.sendFailure(Component.literal("Not enough money. Balance: " + formatMoney(balance)));
            return 0;
        }

        setMoney(player, balance - amount);
        int invested = addInvestment(player, amount);
        source.sendSuccess(() -> Component.literal("Invested " + formatMoney(amount) + ". Investment balance: " + formatMoney(invested) + ". Money balance: " + formatMoney(getMoney(player))), false);
        return amount;
    }

    private static int investmentBalance(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int invested = getInvestment(player);
        source.sendSuccess(() -> Component.literal("Investment balance: " + formatMoney(invested)), false);
        return invested;
    }

    private static int withdrawInvestment(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int invested = getInvestment(player);
        if (invested <= 0) {
            source.sendFailure(Component.literal("You have no invested money."));
            return 0;
        }

        setInvestment(player, 0);
        int balance = addMoney(player, invested);
        source.sendSuccess(() -> Component.literal("Withdrew " + formatMoney(invested) + ". Balance: " + formatMoney(balance)), false);
        return invested;
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
        if (RANDOM.nextInt(100) < LUDKA_WIN_CHANCE_PERCENT) {
            giveItems(player, held.copy(), originalCount);
            source.sendSuccess(() -> Component.literal("You win. Your item count doubled."), false);
            return 1;
        }

        held.setCount(0);
        int cashback = getLudkaCashback(option, originalCount);
        int balance = addMoney(player, cashback);
        source.sendSuccess(() -> Component.literal("You lose, but got " + formatMoney(cashback) + " cashback. Balance: " + formatMoney(balance)), false);
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

    private static int getInvestment(ServerPlayer player) {
        return INVESTMENTS.getOrDefault(player.getUUID(), 0);
    }

    private static int addMoney(ServerPlayer player, int amount) {
        int balance = getMoney(player) + amount;
        setMoney(player, balance);
        return balance;
    }

    private static int addInvestment(ServerPlayer player, int amount) {
        int invested = getInvestment(player) + amount;
        setInvestment(player, invested);
        return invested;
    }

    private static void setMoney(ServerPlayer player, int balance) {
        BALANCES.put(player.getUUID(), balance);
    }

    private static void setInvestment(ServerPlayer player, int balance) {
        INVESTMENTS.put(player.getUUID(), balance);
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

    private static int parseMoneyAmount(String requestedAmount, int allAmount) {
        String normalized = requestedAmount.toLowerCase(Locale.ROOT);
        if (normalized.equals("all")) {
            return allAmount;
        }

        try {
            return parseMoney(normalized);
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    private static String formatMoney(int units) {
        return "$" + (units / 100) + "." + String.format(Locale.ROOT, "%02d", Math.abs(units % 100));
    }

    private static int getLudkaCashback(ShopItem option, int lostCount) {
        if (option == null || option.sellPrice() <= 0) {
            return LUDKA_UNPRICED_CASHBACK_CENTS;
        }

        return Math.max(1, lostCount * option.sellPrice() * LUDKA_CASHBACK_PERCENT / 100);
    }

    private static void giveItems(ServerPlayer player, ItemStack template, int amount) {
        int maxStackSize = Math.max(1, template.getMaxStackSize());
        int remaining = amount;
        while (remaining > 0) {
            int count = Math.min(remaining, maxStackSize);
            ItemStack extraStack = template.copy();
            extraStack.setCount(count);
            if (!player.getInventory().add(extraStack)) {
                player.drop(extraStack, false);
            }
            remaining -= count;
        }
    }

    private static void growInvestments(MinecraftServer server) {
        long day = server.overworld().getOverworldClockTime() / 24000L;
        if (lastInvestmentDay == -1L) {
            lastInvestmentDay = day;
            return;
        }

        if (day <= lastInvestmentDay) {
            return;
        }

        long passedDays = day - lastInvestmentDay;
        lastInvestmentDay = day;
        for (Map.Entry<UUID, Integer> entry : INVESTMENTS.entrySet()) {
            int balance = entry.getValue();
            for (long i = 0; i < passedDays; i++) {
                balance = BigDecimal.valueOf(balance)
                        .multiply(INVESTMENT_DAILY_MULTIPLIER)
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValueExact();
            }
            entry.setValue(balance);
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
            System.err.println("Advanced Economics could not read " + PRICE_CONFIG_PATH + ": " + exception.getMessage());
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
            System.err.println("Advanced Economics could not create " + PRICE_CONFIG_PATH + ": " + exception.getMessage());
        }
    }

    private static List<String> defaultPriceConfigLines() {
        List<String> lines = new ArrayList<>();
        lines.add("# Advanced Economics sell prices.");
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
            updatedLines.add("# Added by Advanced Economics after an update.");
            updatedLines.addAll(missingLines);
            Files.write(PRICE_CONFIG_PATH, updatedLines);
        } catch (IOException exception) {
            System.err.println("Advanced Economics could not update " + PRICE_CONFIG_PATH + ": " + exception.getMessage());
        }
    }

    private static void loadPriceConfigLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }

        String[] parts = trimmed.split("=", 2);
        if (parts.length != 2) {
            System.err.println("Advanced Economics ignored invalid price line: " + line);
            return;
        }

        String itemName = ITEM_ALIASES.get(parts[0].trim().toLowerCase(Locale.ROOT));
        if (itemName == null) {
            itemName = parts[0].trim().toLowerCase(Locale.ROOT);
        }

        if (!SELL_PRICES.containsKey(itemName)) {
            System.err.println("Advanced Economics ignored unknown price item: " + parts[0].trim());
            return;
        }

        try {
            SELL_PRICES.put(itemName, parseMoney(parts[1].trim()));
        } catch (RuntimeException exception) {
            System.err.println("Advanced Economics ignored invalid price for " + itemName + ": " + parts[1].trim());
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
