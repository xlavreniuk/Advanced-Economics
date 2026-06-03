package com.example.advancedeconomics;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class AdvancedEconomicsMod implements ModInitializer {
    private static final Path PRICE_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("advanced-economics-prices.txt");
    private static final Path SETTINGS_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("advanced-economics-settings.json");
    private static final Path FALLBACK_PLAYER_DATA_PATH = FabricLoader.getInstance().getConfigDir().resolve("advanced-economics-player-data.json");
    private static final Identifier DATAPACK_PRICE_OVERRIDES = Identifier.fromNamespaceAndPath("advanced-economics", "economy/prices.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int GAMBLE_CASHBACK_PERCENT = 5;
    private static final int GAMBLE_UNPRICED_CASHBACK_CENTS = 1;
    private static final int GAMBLE_WIN_CHANCE_PERCENT = 40;
    private static final int MONEY_GAMBLE_WIN_CHANCE_PERCENT = 45;
    private static final int GAMBLE_HISTORY_LIMIT = 10;
    private static final BigDecimal INVESTMENT_DAILY_MULTIPLIER = new BigDecimal("1.01");
    private static final int SEND_FEE_PERCENT = 5;
    private static final int INVESTMENT_FEE_PERCENT = 1;
    private static final int BUY_PRICE_MULTIPLIER = 4;
    private static final int TELEPORT_PRICE = 150000;
    private static final int PROFESSION_CHANGE_PRICE = 50000;
    private static final int PET_SALE_PRICE = 50000;
    private static final int PET_INSURANCE_PRICE = 30000;
    private static final int PET_INSURANCE_PAYOUT = 75000;
    private static final int DEATH_LOSS_PERCENT = 10;
    private static final int INSURED_DEATH_LOSS_PERCENT = 2;
    private static final int PLAYER_INSURANCE_PRICE = 100000;
    private static final int REPAIR_PRICE_PER_DAMAGE = 100;
    private static final int REMOVE_ENCHANT_PRICE = 25000;
    private static final int RENAME_PRICE = 5000;
    private static final int EFFECT_PRICE = 30000;
    private static final int TERM_DEPOSIT_FEE_PERCENT = 1;
    private static final Map<String, Profession> PROFESSIONS = new HashMap<>();
    private static final Map<String, Holder<MobEffect>> BUYABLE_EFFECTS = new HashMap<>();
    private static final Map<UUID, Integer> BALANCES = new HashMap<>();
    private static final Map<UUID, Integer> INVESTMENTS = new HashMap<>();
    private static final Map<UUID, Integer> LOANS = new HashMap<>();
    private static final Map<UUID, TermDeposit> TERM_DEPOSITS = new HashMap<>();
    private static final Map<UUID, String> PLAYER_PROFESSIONS = new HashMap<>();
    private static final Map<UUID, Integer> PROFESSION_SALES = new HashMap<>();
    private static final Map<UUID, Long> DAILY_QUEST_DAYS = new HashMap<>();
    private static final Set<UUID> HIDDEN_DAILY_QUEST_WIDGETS = new HashSet<>();
    private static final Map<UUID, Set<String>> COMPLETED_PROFESSION_QUESTS = new HashMap<>();
    private static final Map<UUID, Boolean> PLAYER_INSURANCE = new HashMap<>();
    private static final Map<UUID, UUID> PET_INSURANCE_OWNERS = new HashMap<>();
    private static final Map<UUID, Set<String>> UNLOCKED_ITEMS = new HashMap<>();
    private static final Map<UUID, Map<String, String>> UNLOCK_SOURCES = new HashMap<>();
    private static final Map<UUID, List<String>> GAMBLE_HISTORY = new HashMap<>();
    private static final Map<UUID, DuelRequest> DUEL_REQUESTS = new HashMap<>();
    private static final Set<UUID> WELCOME_HINT_SHOWN = new HashSet<>();
    private static final Map<String, Integer> SELL_PRICES = new HashMap<>();
    private static final Map<Item, ShopItemDefinition> SHOP_ITEMS = new HashMap<>();
    private static final Map<String, String> ITEM_ALIASES = new HashMap<>();
    private static final List<ShopItemDefinition> SHOP_ITEM_DEFINITIONS = new ArrayList<>();
    public static final List<CommandHelp> COMMAND_HELP = List.of(
            new CommandHelp("/money", "Example: /money", "Shows your wallet balance."),
            new CommandHelp("/money send <player> <amount|all>", "Example: /money send Steve 25.00", "Sends money to another player with a 5% sender fee."),
            new CommandHelp("/sell <count|all>", "Example: /sell all", "Sells the held supported item."),
            new CommandHelp("/sell <item> <count|all>", "Example: /sell diamond 3", "Sells a supported item from your inventory."),
            new CommandHelp("/sell inventory", "Example: /sell inventory", "Sells every supported item in your inventory."),
            new CommandHelp("/buy <item> <count|all>", "Example: /buy bread 16", "Buys unlocked shop items."),
            new CommandHelp("/unlock <item>", "Example: /unlock diamond", "Pays to unlock buying for an item."),
            new CommandHelp("/unlocks", "Example: /unlocks", "Lists item unlock status."),
            new CommandHelp("/price", "Example: /price", "Shows the held item price."),
            new CommandHelp("/invest <amount|all>", "Example: /invest 100.00", "Moves money into investment with a 1% fee."),
            new CommandHelp("/invest balance", "Example: /invest balance", "Shows investment balance."),
            new CommandHelp("/invest withdraw", "Example: /invest withdraw", "Withdraws investment balance with a 1% fee."),
            new CommandHelp("/termdeposit <7|14|30> <amount>", "Example: /termdeposit 7 250.00", "Starts a fixed-term deposit."),
            new CommandHelp("/termdeposit claim", "Example: /termdeposit claim", "Claims a matured fixed-term deposit."),
            new CommandHelp("/gamble", "Example: /gamble", "Risks the held stack for a chance to double it."),
            new CommandHelp("/gamble money <amount>", "Example: /gamble money 50.00", "Risks money for a double-stake payout chance."),
            new CommandHelp("/duel <player> <amount>", "Example: /duel Steve 100.00", "Starts a money duel; winner takes both stakes."),
            new CommandHelp("/duel accept", "Example: /duel accept", "Accepts a pending money duel."),
            new CommandHelp("/duel deny", "Example: /duel deny", "Rejects a pending money duel."),
            new CommandHelp("/paidtp <player>", "Example: /paidtp Steve", "Teleports to a player for money."),
            new CommandHelp("/dailyquest", "Example: /dailyquest", "Shows the daily quest."),
            new CommandHelp("/dailyquest claim", "Example: /dailyquest claim", "Turns in the daily quest items for money."),
            new CommandHelp("/dailyquest hide|show", "Example: /dailyquest hide", "Hides or shows the daily quest toast."),
            new CommandHelp("/profession", "Example: /profession", "Shows current profession status."),
            new CommandHelp("/profession choose <miner|lumberjack|farmer|fisherman|hunter>", "Example: /profession choose miner", "Selects or changes your profession."),
            new CommandHelp("/profession quest", "Example: /profession quest", "Shows your profession quest."),
            new CommandHelp("/profession quest claim", "Example: /profession quest claim", "Claims a completed profession quest."),
            new CommandHelp("/effectbuy <speed|haste>", "Example: /effectbuy speed", "Buys a short potion effect."),
            new CommandHelp("/xpmoney buy <levels>", "Example: /xpmoney buy 5", "Buys XP levels with money."),
            new CommandHelp("/xpmoney sell <levels|all>", "Example: /xpmoney sell all", "Sells XP levels for money."),
            new CommandHelp("/itemservice repair", "Example: /itemservice repair", "Repairs the held item for money."),
            new CommandHelp("/itemservice disenchant", "Example: /itemservice disenchant", "Removes enchantments from the held item."),
            new CommandHelp("/itemservice rename <name>", "Example: /itemservice rename Lucky Pick", "Renames the held item."),
            new CommandHelp("/loan take <amount>", "Example: /loan take 500.00", "Takes an emergency loan."),
            new CommandHelp("/loan repay <amount|all>", "Example: /loan repay all", "Repays an active loan."),
            new CommandHelp("/pet sell", "Example: /pet sell", "Sells a nearby owned tamed pet."),
            new CommandHelp("/pet insure", "Example: /pet insure", "Insures a nearby owned tamed pet."),
            new CommandHelp("/insurance buy", "Example: /insurance buy", "Reduces the next death fee."),
            new CommandHelp("/aesettings", "Example: /aesettings", "Shows server feature toggles."),
            new CommandHelp("/aesettings toggle <feature>", "Example: /aesettings toggle wallet", "Toggles a server feature."),
            new CommandHelp("/aegivemoney <amount>", "Example: /aegivemoney 1000.00", "Creative-only command that gives yourself money."),
            new CommandHelp("/aegivemoney <player> <amount>", "Example: /aegivemoney Steve 1000.00", "Creative-only command that gives another player money."),
            new CommandHelp("/aehelp", "Example: /aehelp", "Shows this full command list.")
    );
    private static final Random RANDOM = new Random();
    private static FeatureSettings featureSettings = new FeatureSettings();
    private static Path playerDataPath = FALLBACK_PLAYER_DATA_PATH;
    private static long lastInvestmentDay = -1L;
    private static int worldQuestSalt = 0;

    static {
        registerProfessions();
        registerBuyableEffects();
        registerShopItems();
    }

    @Override
    public void onInitialize() {
        loadPriceConfig();
        loadFeatureSettings();
        reportDependencyState();
        PayloadTypeRegistry.clientboundPlay().register(EconomyStatePayload.TYPE, EconomyStatePayload.CODEC);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("sell")
                    .executes(context -> sell(context.getSource(), "1"))
                    .then(Commands.argument("target", StringArgumentType.word())
                            .suggests((context, builder) -> suggestShopItems(builder))
                            .executes(context -> sellFlexible(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "target")
                            ))
                            .then(Commands.argument("amount", StringArgumentType.word())
                                    .executes(context -> sellItem(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "target"),
                                            StringArgumentType.getString(context, "amount")
                                    )))));

            dispatcher.register(Commands.literal("buy")
                    .then(Commands.argument("item", StringArgumentType.word())
                            .suggests((context, builder) -> suggestShopItems(builder))
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

            dispatcher.register(Commands.literal("aehelp")
                    .executes(context -> help(context.getSource())));

            dispatcher.register(Commands.literal("aegivemoney")
                    .then(Commands.argument("amount", StringArgumentType.word())
                            .executes(context -> giveMoneyCommand(
                                    context.getSource(),
                                    context.getSource().getPlayerOrException(),
                                    StringArgumentType.getString(context, "amount")
                            )))
                    .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("amount", StringArgumentType.word())
                                    .executes(context -> giveMoneyCommand(
                                            context.getSource(),
                                            EntityArgument.getPlayer(context, "player"),
                                            StringArgumentType.getString(context, "amount")
                                    )))));

            dispatcher.register(Commands.literal("price")
                    .executes(context -> price(context.getSource())));

            dispatcher.register(Commands.literal("unlock")
                    .then(Commands.argument("item", StringArgumentType.word())
                            .suggests((context, builder) -> suggestShopItems(builder))
                            .executes(context -> unlock(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "item")
                            ))));

            dispatcher.register(Commands.literal("unlocks")
                    .executes(context -> unlocks(context.getSource())));

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

            dispatcher.register(Commands.literal("gamble")
                    .executes(context -> gamble(context.getSource()))
                    .then(Commands.literal("money")
                            .then(Commands.argument("amount", StringArgumentType.word())
                                    .executes(context -> gambleMoney(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "amount")
                                    )))));

            dispatcher.register(Commands.literal("duel")
                    .then(Commands.literal("accept")
                            .executes(context -> acceptDuel(context.getSource())))
                    .then(Commands.literal("deny")
                            .executes(context -> denyDuel(context.getSource())))
                    .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("amount", StringArgumentType.word())
                                    .executes(context -> requestDuel(
                                            context.getSource(),
                                            EntityArgument.getPlayer(context, "player"),
                                            StringArgumentType.getString(context, "amount")
                                    )))));

            dispatcher.register(Commands.literal("effectbuy")
                    .then(Commands.argument("effect", StringArgumentType.word())
                            .suggests((context, builder) -> suggestStrings(builder, BUYABLE_EFFECTS.keySet()))
                            .executes(context -> buyEffect(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "effect")
                            ))));

            dispatcher.register(Commands.literal("dailyquest")
                    .executes(context -> showDailyQuest(context.getSource()))
                    .then(Commands.literal("hide")
                            .executes(context -> setDailyQuestWidget(context.getSource(), true)))
                    .then(Commands.literal("show")
                            .executes(context -> setDailyQuestWidget(context.getSource(), false)))
                    .then(Commands.literal("claim")
                            .executes(context -> claimDailyQuest(context.getSource()))));

            dispatcher.register(Commands.literal("aesettings")
                    .executes(context -> showFeatureSettings(context.getSource()))
                    .then(Commands.literal("toggle")
                            .then(Commands.argument("feature", StringArgumentType.word())
                                    .suggests((context, builder) -> suggestStrings(builder, featureSettings.featureNames()))
                                    .executes(context -> toggleFeatureSetting(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "feature")
                                    )))));

            dispatcher.register(Commands.literal("profession")
                    .executes(context -> professionStatus(context.getSource()))
                    .then(Commands.literal("choose")
                            .then(Commands.argument("profession", StringArgumentType.word())
                                    .suggests((context, builder) -> suggestStrings(builder, PROFESSIONS.keySet()))
                                    .executes(context -> chooseProfession(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "profession")
                                    ))))
                    .then(Commands.literal("quest")
                            .executes(context -> professionQuest(context.getSource()))
                            .then(Commands.literal("claim")
                                    .executes(context -> claimProfessionQuest(context.getSource())))));

            dispatcher.register(Commands.literal("paidtp")
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> paidTeleport(
                                    context.getSource(),
                                    EntityArgument.getPlayer(context, "player")
                            ))));

            dispatcher.register(Commands.literal("xpmoney")
                    .then(Commands.literal("buy")
                            .executes(context -> buyXpLevel(context.getSource(), "1"))
                            .then(Commands.argument("levels", StringArgumentType.word())
                                    .executes(context -> buyXpLevel(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "levels")
                                    ))))
                    .then(Commands.literal("sell")
                            .executes(context -> sellXpLevel(context.getSource(), "1"))
                            .then(Commands.argument("levels", StringArgumentType.word())
                                    .executes(context -> sellXpLevel(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "levels")
                                    )))));

            dispatcher.register(Commands.literal("itemservice")
                    .then(Commands.literal("repair")
                            .executes(context -> repairHeldItem(context.getSource())))
                    .then(Commands.literal("disenchant")
                            .executes(context -> removeHeldEnchantments(context.getSource())))
                    .then(Commands.literal("rename")
                            .then(Commands.argument("name", StringArgumentType.greedyString())
                                    .executes(context -> renameHeldItem(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "name")
                                    )))));

            dispatcher.register(Commands.literal("termdeposit")
                    .then(Commands.literal("claim")
                            .executes(context -> claimTermDeposit(context.getSource())))
                    .then(Commands.argument("days", IntegerArgumentType.integer(7, 30))
                            .then(Commands.argument("amount", StringArgumentType.word())
                                    .executes(context -> startTermDeposit(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "days"),
                                            StringArgumentType.getString(context, "amount")
                                    )))));

            dispatcher.register(Commands.literal("loan")
                    .then(Commands.literal("take")
                            .then(Commands.argument("amount", StringArgumentType.word())
                                    .executes(context -> takeLoan(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "amount")
                                    ))))
                    .then(Commands.literal("repay")
                            .then(Commands.argument("amount", StringArgumentType.word())
                                    .executes(context -> repayLoan(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "amount")
                                    )))));

            dispatcher.register(Commands.literal("pet")
                    .then(Commands.literal("sell")
                            .executes(context -> sellNearestPet(context.getSource())))
                    .then(Commands.literal("insure")
                            .executes(context -> insureNearestPet(context.getSource()))));

            dispatcher.register(Commands.literal("insurance")
                    .then(Commands.literal("buy")
                            .executes(context -> buyPlayerInsurance(context.getSource()))));
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            rememberPlayerInventoryItems(server);
            syncPlayerEconomyStates(server);
            savePlayerDataPeriodically(server);
            growInvestments(server);
        });
        ServerLifecycleEvents.SERVER_STARTING.register(AdvancedEconomicsMod::loadWorldPlayerData);
        ServerLifecycleEvents.SERVER_STARTED.register(AdvancedEconomicsMod::loadDatapackPriceOverrides);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                loadPriceConfig();
                loadDatapackPriceOverrides(server);
                server.getPlayerList().getPlayers().forEach(AdvancedEconomicsMod::sendEconomyState);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> savePlayerData());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> resetLoadedWorldPlayerData());
        ServerLivingEntityEvents.AFTER_DEATH.register(AdvancedEconomicsMod::afterEntityDeath);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            sendEconomyState(player);
            sendWelcomeHint(player);
        });
    }

    private static void sendWelcomeHint(ServerPlayer player) {
        if (!WELCOME_HINT_SHOWN.add(player.getUUID())) {
            return;
        }

        player.sendSystemMessage(Component.translatable("message.advanced-economics.open_hint").withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.literal("Use /aehelp to view Advanced Economics commands and explanations.").withStyle(ChatFormatting.GRAY));
        savePlayerData();
    }

    private static int sell(CommandSourceStack source, String requestedAmount) throws CommandSyntaxException {
        if (!ensureFeature(source, "shop_sell")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        rememberUnlockedItems(player);
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

        int earned = saleValue(player, option, soldCount);
        held.shrink(soldCount);
        int balance = addMoney(player, earned);
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Sold " + soldCount + " " + option.displayName() + " for " + formatMoney(earned) + ". Balance: " + formatMoney(balance)), false);
        return soldCount;
    }

    private static int sellFlexible(CommandSourceStack source, String target) throws CommandSyntaxException {
        String normalized = target.toLowerCase(Locale.ROOT);
        if (normalized.equals("all") || normalized.equals("inventory") || isInteger(normalized)) {
            return sell(source, target);
        }
        return sellItem(source, target, "1");
    }

    private static int sellItem(CommandSourceStack source, String requestedItem, String requestedAmount) throws CommandSyntaxException {
        if (!ensureFeature(source, "shop_sell")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        rememberUnlockedItems(player);
        ShopItem option = getShopItem(requestedItem);

        if (option == null) {
            source.sendFailure(Component.literal("This item is not in the Advanced Economics price list."));
            return 0;
        }

        int available = countInventoryItems(player, option.item());
        int soldCount = Math.min(parseAmount(requestedAmount, available), available);
        if (soldCount <= 0) {
            source.sendFailure(Component.literal("Use /sell <item> <count|all>."));
            return 0;
        }

        removeInventoryItems(player, option.item(), soldCount);
        int earned = saleValue(player, option, soldCount);
        int balance = addMoney(player, earned);
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Sold " + soldCount + " " + option.displayName() + " for " + formatMoney(earned) + ". Balance: " + formatMoney(balance)), false);
        return soldCount;
    }

    private static int sellInventory(CommandSourceStack source, ServerPlayer player) {
        rememberUnlockedItems(player);
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
            earned += saleValue(player, option, count);
            stack.setCount(0);
        }

        if (soldCount == 0) {
            source.sendFailure(Component.literal("No sellable items found in your inventory."));
            return 0;
        }

        int balance = addMoney(player, earned);
        int finalSoldCount = soldCount;
        int finalEarned = earned;
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Sold " + finalSoldCount + " inventory items for " + formatMoney(finalEarned) + ". Balance: " + formatMoney(balance)), false);
        return soldCount;
    }

    private static int buy(CommandSourceStack source, String requestedItem, String requestedAmount) throws CommandSyntaxException {
        if (!ensureFeature(source, "shop_sell")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        rememberUnlockedItems(player);
        ShopItem option = getShopItem(requestedItem);

        if (option == null) {
            source.sendFailure(Component.literal("This item is not in the Advanced Economics price list."));
            return 0;
        }

        if (!isUnlocked(player, option)) {
            source.sendFailure(Component.literal(option.displayName() + " is locked. Find it once, or use /unlock " + option.primaryName() + " for " + formatMoney(unlockPrice(option)) + "."));
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
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Bought " + finalBoughtCount + " " + option.displayName() + " for " + formatMoney(spent) + ". Balance: " + formatMoney(getMoney(player))), false);
        return boughtCount;
    }

    private static int money(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.literal("Balance: " + formatMoney(getMoney(player))), false);
        return getMoney(player);
    }

    private static int giveMoneyCommand(CommandSourceStack source, ServerPlayer target, String requestedAmount) throws CommandSyntaxException {
        if (!canUseCreativeMoneyCommand(source)) {
            source.sendFailure(Component.literal("Only creative-mode players can use /aegivemoney."));
            return 0;
        }

        int amount = parseMoneyAmount(requestedAmount, 0);
        if (amount <= 0) {
            source.sendFailure(Component.literal("Use /aegivemoney <amount> or /aegivemoney <player> <amount>."));
            return 0;
        }

        int balance = addMoney(target, amount);
        savePlayerData();
        sendEconomyState(target);
        source.sendSuccess(() -> Component.literal("Gave " + formatMoney(amount) + " to " + target.getName().getString() + ". Balance: " + formatMoney(balance)), true);
        Entity executor = source.getEntity();
        if (executor == null || !executor.getUUID().equals(target.getUUID())) {
            target.sendSystemMessage(Component.literal("Advanced Economics added " + formatMoney(amount) + " to your balance."));
        }
        return amount;
    }

    private static boolean canUseCreativeMoneyCommand(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return player.isCreative();
        } catch (CommandSyntaxException exception) {
            return false;
        }
    }

    private static int sendMoney(CommandSourceStack source, ServerPlayer target, String requestedAmount) throws CommandSyntaxException {
        if (!ensureFeature(source, "wallet")) {
            return 0;
        }
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

        int fee = percentage(amount, SEND_FEE_PERCENT);
        int totalCost = amount + fee;
        if (totalCost > senderBalance) {
            source.sendFailure(Component.literal("Not enough money. Sending " + formatMoney(amount) + " has a 5% fee (" + formatMoney(fee) + "). Balance: " + formatMoney(senderBalance)));
            return 0;
        }

        setMoney(sender, senderBalance - totalCost);
        int targetBalance = addMoney(target, amount);
        savePlayerData();
        sendEconomyState(sender);
        sendEconomyState(target);
        source.sendSuccess(() -> Component.literal("Sent " + formatMoney(amount) + " to " + target.getName().getString() + ". Fee: " + formatMoney(fee) + ". Balance: " + formatMoney(getMoney(sender))), false);
        target.sendSystemMessage(Component.literal(sender.getName().getString() + " sent you " + formatMoney(amount) + ". Balance: " + formatMoney(targetBalance)));
        return amount;
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Advanced Economics commands:"), false);
        for (CommandHelp command : COMMAND_HELP) {
            source.sendSuccess(() -> Component.literal(command.syntax() + " - " + command.description() + " " + command.example()), false);
        }
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

    private static int unlock(CommandSourceStack source, String requestedItem) throws CommandSyntaxException {
        if (!ensureFeature(source, "shop_sell")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        rememberUnlockedItems(player);
        ShopItem option = getShopItem(requestedItem);

        if (option == null) {
            source.sendFailure(Component.literal("This item is not in the Advanced Economics price list."));
            return 0;
        }

        if (isUnlocked(player, option)) {
            source.sendSuccess(() -> Component.literal(option.displayName() + " is already unlocked."), false);
            return 1;
        }

        int price = unlockPrice(option);
        int balance = getMoney(player);
        if (balance < price) {
            source.sendFailure(Component.literal("Not enough money. Unlocking " + option.displayName() + " costs " + formatMoney(price) + ". Balance: " + formatMoney(balance)));
            return 0;
        }

        setMoney(player, balance - price);
        unlockItem(player, option, "Paid");
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Unlocked " + option.displayName() + " for " + formatMoney(price) + ". Balance: " + formatMoney(getMoney(player))), false);
        return 1;
    }

    private static int unlocks(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        rememberUnlockedItems(player);
        Set<String> unlocked = UNLOCKED_ITEMS.getOrDefault(player.getUUID(), Set.of());

        if (unlocked.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No shop items unlocked yet. Find an item once, or use /unlock <item>."), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Unlocked shop items: " + String.join(", ", unlocked.stream().sorted().toList())), false);
        return unlocked.size();
    }

    private static int invest(CommandSourceStack source, String requestedAmount) throws CommandSyntaxException {
        if (!ensureFeature(source, "investments")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        int balance = getMoney(player);
        int amount = parseFeeInclusiveMoneyAmount(requestedAmount, balance, INVESTMENT_FEE_PERCENT);
        if (amount <= 0) {
            source.sendFailure(Component.literal("Use /invest <amount> or /invest all."));
            return 0;
        }

        int fee = percentage(amount, INVESTMENT_FEE_PERCENT);
        if (amount + fee > balance) {
            source.sendFailure(Component.literal("Not enough money. Investing has a 1% fee (" + formatMoney(fee) + "). Balance: " + formatMoney(balance)));
            return 0;
        }

        setMoney(player, balance - amount - fee);
        int invested = addInvestment(player, amount);
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Invested " + formatMoney(amount) + " with " + formatMoney(fee) + " fee. Investment balance: " + formatMoney(invested) + ". Money balance: " + formatMoney(getMoney(player))), false);
        return amount;
    }

    private static int investmentBalance(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int invested = getInvestment(player);
        source.sendSuccess(() -> Component.literal("Investment balance: " + formatMoney(invested)), false);
        return invested;
    }

    private static int withdrawInvestment(CommandSourceStack source) throws CommandSyntaxException {
        if (!ensureFeature(source, "investments")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        int invested = getInvestment(player);
        if (invested <= 0) {
            source.sendFailure(Component.literal("You have no invested money."));
            return 0;
        }

        int fee = percentage(invested, INVESTMENT_FEE_PERCENT);
        int payout = Math.max(0, invested - fee);
        setInvestment(player, 0);
        int balance = addMoney(player, payout);
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Withdrew " + formatMoney(payout) + " after " + formatMoney(fee) + " fee. Balance: " + formatMoney(balance)), false);
        return payout;
    }

    private static int gamble(CommandSourceStack source) throws CommandSyntaxException {
        if (!ensureFeature(source, "gambling")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        ShopItem option = getShopItem(held);

        if (held.isEmpty()) {
            source.sendFailure(Component.literal("Hold an item in your main hand before using /gamble."));
            return 0;
        }

        int originalCount = held.getCount();
        String itemName = held.getHoverName().getString();
        if (RANDOM.nextInt(100) < GAMBLE_WIN_CHANCE_PERCENT) {
            giveItems(player, held.copy(), originalCount);
            addGambleHistory(player, "Win: doubled " + originalCount + " " + itemName);
            savePlayerData();
            sendEconomyState(player);
            source.sendSuccess(() -> Component.literal("You win. Your item count doubled."), false);
            return 1;
        }

        held.setCount(0);
        int cashback = getGambleCashback(option, originalCount);
        int balance = addMoney(player, cashback);
        addGambleHistory(player, "Loss: lost " + originalCount + " " + itemName + ", cashback " + formatMoney(cashback));
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("You lose, but got " + formatMoney(cashback) + " cashback. Balance: " + formatMoney(balance)), false);
        return 1;
    }

    private static int gambleMoney(CommandSourceStack source, String requestedAmount) throws CommandSyntaxException {
        if (!ensureFeature(source, "gambling")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        int balance = getMoney(player);
        int amount = parseMoneyAmount(requestedAmount, balance);
        if (amount <= 0) {
            source.sendFailure(Component.literal("Use /gamble money <amount|all>."));
            return 0;
        }
        if (amount > balance) {
            source.sendFailure(Component.literal("Not enough money. Balance: " + formatMoney(balance)));
            return 0;
        }

        setMoney(player, balance - amount);
        if (RANDOM.nextInt(100) < MONEY_GAMBLE_WIN_CHANCE_PERCENT) {
            int prize = amount * 2;
            int newBalance = addMoney(player, prize);
            addGambleHistory(player, "Money win: staked " + formatMoney(amount) + ", won " + formatMoney(prize));
            savePlayerData();
            sendEconomyState(player);
            source.sendSuccess(() -> Component.literal("You won " + formatMoney(prize) + ". Balance: " + formatMoney(newBalance)), false);
            return prize;
        }

        addGambleHistory(player, "Money loss: lost " + formatMoney(amount));
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("You lost " + formatMoney(amount) + ". Balance: " + formatMoney(getMoney(player))), false);
        return amount;
    }

    private static int requestDuel(CommandSourceStack source, ServerPlayer target, String requestedAmount) throws CommandSyntaxException {
        if (!ensureFeature(source, "wallet")) {
            return 0;
        }
        ServerPlayer challenger = source.getPlayerOrException();
        if (challenger.getUUID().equals(target.getUUID())) {
            source.sendFailure(Component.literal("You cannot duel yourself."));
            return 0;
        }

        int amount = parseMoneyAmount(requestedAmount, getMoney(challenger));
        if (amount <= 0) {
            source.sendFailure(Component.literal("Use /duel <player> <amount>."));
            return 0;
        }
        if (getMoney(challenger) < amount) {
            source.sendFailure(Component.literal("Not enough money. Balance: " + formatMoney(getMoney(challenger))));
            return 0;
        }

        DUEL_REQUESTS.put(target.getUUID(), new DuelRequest(challenger.getUUID(), amount, challenger.getName().getString()));
        target.sendSystemMessage(Component.literal(challenger.getName().getString() + " challenged you to a money duel for " + formatMoney(amount) + ". Use /duel accept or /duel deny."));
        source.sendSuccess(() -> Component.literal("Duel request sent to " + target.getName().getString() + " for " + formatMoney(amount) + "."), false);
        return 1;
    }

    private static int acceptDuel(CommandSourceStack source) throws CommandSyntaxException {
        if (!ensureFeature(source, "wallet")) {
            return 0;
        }
        ServerPlayer target = source.getPlayerOrException();
        DuelRequest request = DUEL_REQUESTS.remove(target.getUUID());
        if (request == null) {
            source.sendFailure(Component.literal("You have no pending duel request."));
            return 0;
        }

        ServerPlayer challenger = server(target).getPlayerList().getPlayer(request.challenger());
        if (challenger == null) {
            source.sendFailure(Component.literal("The challenger is no longer online."));
            return 0;
        }
        if (getMoney(challenger) < request.amount() || getMoney(target) < request.amount()) {
            source.sendFailure(Component.literal("Both players need " + formatMoney(request.amount()) + " to accept this duel."));
            challenger.sendSystemMessage(Component.literal("Your duel with " + target.getName().getString() + " failed because one player cannot pay."));
            return 0;
        }

        setMoney(challenger, getMoney(challenger) - request.amount());
        setMoney(target, getMoney(target) - request.amount());
        ServerPlayer winner = RANDOM.nextBoolean() ? challenger : target;
        ServerPlayer loser = winner == challenger ? target : challenger;
        int prize = request.amount() * 2;
        addMoney(winner, prize);
        savePlayerData();
        sendEconomyState(challenger);
        sendEconomyState(target);
        winner.sendSystemMessage(Component.literal("You won the duel and received " + formatMoney(prize) + "."));
        loser.sendSystemMessage(Component.literal("You lost the duel for " + formatMoney(request.amount()) + "."));
        return 1;
    }

    private static int denyDuel(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer target = source.getPlayerOrException();
        DuelRequest request = DUEL_REQUESTS.remove(target.getUUID());
        if (request == null) {
            source.sendFailure(Component.literal("You have no pending duel request."));
            return 0;
        }

        ServerPlayer challenger = server(target).getPlayerList().getPlayer(request.challenger());
        if (challenger != null) {
            challenger.sendSystemMessage(Component.literal(target.getName().getString() + " denied your duel request."));
        }
        source.sendSuccess(() -> Component.literal("Duel denied."), false);
        return 1;
    }

    private static int buyEffect(CommandSourceStack source, String effectName) throws CommandSyntaxException {
        if (!ensureFeature(source, "item_services")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        Holder<MobEffect> effect = BUYABLE_EFFECTS.get(effectName.toLowerCase(Locale.ROOT));
        if (effect == null) {
            source.sendFailure(Component.literal("Unknown effect. Available: " + String.join(", ", BUYABLE_EFFECTS.keySet())));
            return 0;
        }
        if (!charge(player, EFFECT_PRICE)) {
            source.sendFailure(Component.literal("Not enough money. Effect costs " + formatMoney(EFFECT_PRICE) + "."));
            return 0;
        }

        player.addEffect(new MobEffectInstance(effect, 20 * 60, 0));
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Bought " + effectName + " I for 1 minute. Balance: " + formatMoney(getMoney(player))), false);
        return 1;
    }

    private static int showDailyQuest(CommandSourceStack source) throws CommandSyntaxException {
        if (!ensureFeature(source, "quests")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        DailyQuest quest = dailyQuest(player);
        int progress = countInventoryItems(player, quest.item().item());
        source.sendSuccess(() -> Component.literal("Daily quest: turn in " + quest.count() + " " + quest.item().displayName() + " for " + formatMoney(quest.reward()) + ". Progress: " + Math.min(progress, quest.count()) + "/" + quest.count() + "."), false);
        return 1;
    }

    private static int claimDailyQuest(CommandSourceStack source) throws CommandSyntaxException {
        if (!ensureFeature(source, "quests")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        long day = economyDay(server(player));
        if (DAILY_QUEST_DAYS.getOrDefault(player.getUUID(), -1L) == day) {
            source.sendFailure(Component.literal("You already claimed today's quest."));
            return 0;
        }

        DailyQuest quest = dailyQuest(player);
        if (!removeInventoryItems(player, quest.item().item(), quest.count())) {
            source.sendFailure(Component.literal("You need " + quest.count() + " " + quest.item().displayName() + "."));
            return 0;
        }

        DAILY_QUEST_DAYS.put(player.getUUID(), day);
        int balance = addMoney(player, quest.reward());
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Daily quest complete. Reward: " + formatMoney(quest.reward()) + ". Balance: " + formatMoney(balance)), false);
        return 1;
    }

    private static int setDailyQuestWidget(CommandSourceStack source, boolean hidden) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (hidden) {
            HIDDEN_DAILY_QUEST_WIDGETS.add(player.getUUID());
        } else {
            HIDDEN_DAILY_QUEST_WIDGETS.remove(player.getUUID());
        }
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal(hidden ? "Daily quest card hidden." : "Daily quest card shown."), false);
        return 1;
    }

    private static int showFeatureSettings(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Advanced Economics features: " + featureSettings.summary()), false);
        return 1;
    }

    private static int toggleFeatureSetting(CommandSourceStack source, String feature) {
        String normalized = feature.toLowerCase(Locale.ROOT);
        if (!featureSettings.toggle(normalized)) {
            source.sendFailure(Component.literal("Unknown feature. Available: " + String.join(", ", featureSettings.featureNames())));
            return 0;
        }
        saveFeatureSettings();
        source.getServer().getPlayerList().getPlayers().forEach(AdvancedEconomicsMod::sendEconomyState);
        source.sendSuccess(() -> Component.literal(normalized + " is now " + (featureSettings.isEnabled(normalized) ? "enabled" : "disabled") + "."), true);
        return 1;
    }

    private static int professionStatus(CommandSourceStack source) throws CommandSyntaxException {
        if (!ensureFeature(source, "professions")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        String professionId = PLAYER_PROFESSIONS.get(player.getUUID());
        if (professionId == null) {
            source.sendSuccess(() -> Component.literal("No profession selected. Use /profession choose <" + String.join("|", PROFESSIONS.keySet()) + ">. First choice is free, changes cost " + formatMoney(PROFESSION_CHANGE_PRICE) + "."), false);
            return 0;
        }

        Profession profession = PROFESSIONS.get(professionId);
        source.sendSuccess(() -> Component.literal("Profession: " + profession.displayName() + ". Sell bonus: " + professionBonusPercent(player) + "%, max 25%. Progress value: " + formatMoney(PROFESSION_SALES.getOrDefault(player.getUUID(), 0)) + "."), false);
        return 1;
    }

    private static int chooseProfession(CommandSourceStack source, String requestedProfession) throws CommandSyntaxException {
        if (!ensureFeature(source, "professions")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        String professionId = requestedProfession.toLowerCase(Locale.ROOT);
        Profession profession = PROFESSIONS.get(professionId);
        if (profession == null) {
            source.sendFailure(Component.literal("Unknown profession. Available: " + String.join(", ", PROFESSIONS.keySet())));
            return 0;
        }

        String current = PLAYER_PROFESSIONS.get(player.getUUID());
        if (professionId.equals(current)) {
            source.sendSuccess(() -> Component.literal("You already are a " + profession.displayName() + "."), false);
            return 1;
        }
        if (current != null && !charge(player, PROFESSION_CHANGE_PRICE)) {
            source.sendFailure(Component.literal("Changing profession costs " + formatMoney(PROFESSION_CHANGE_PRICE) + "."));
            return 0;
        }

        PLAYER_PROFESSIONS.put(player.getUUID(), professionId);
        PROFESSION_SALES.put(player.getUUID(), 0);
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Profession set to " + profession.displayName() + ". Matching sales start with a 5% bonus and can grow to 25%."), false);
        return 1;
    }

    private static int professionQuest(CommandSourceStack source) throws CommandSyntaxException {
        if (!ensureFeature(source, "professions")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        ProfessionQuest quest = professionQuestFor(player);
        if (quest == null) {
            source.sendFailure(Component.literal("Choose a profession first with /profession choose <profession>."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Profession quest: turn in " + quest.count() + " " + quest.item().displayName() + " for " + formatMoney(quest.reward()) + ". Use /profession quest claim."), false);
        return 1;
    }

    private static int claimProfessionQuest(CommandSourceStack source) throws CommandSyntaxException {
        if (!ensureFeature(source, "professions")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        ProfessionQuest quest = professionQuestFor(player);
        if (quest == null) {
            source.sendFailure(Component.literal("Choose a profession first."));
            return 0;
        }

        Set<String> completed = COMPLETED_PROFESSION_QUESTS.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>());
        if (completed.contains(quest.key())) {
            source.sendFailure(Component.literal("You already completed this profession quest."));
            return 0;
        }
        if (!removeInventoryItems(player, quest.item().item(), quest.count())) {
            source.sendFailure(Component.literal("You need " + quest.count() + " " + quest.item().displayName() + "."));
            return 0;
        }

        completed.add(quest.key());
        int balance = addMoney(player, quest.reward());
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Profession quest complete. Reward: " + formatMoney(quest.reward()) + ". Balance: " + formatMoney(balance)), false);
        return 1;
    }

    private static int paidTeleport(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        if (!ensureFeature(source, "teleports")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        if (player.getUUID().equals(target.getUUID())) {
            source.sendFailure(Component.literal("You are already there."));
            return 0;
        }
        if (!charge(player, TELEPORT_PRICE)) {
            source.sendFailure(Component.literal("Teleport costs " + formatMoney(TELEPORT_PRICE) + "."));
            return 0;
        }

        player.teleportTo((ServerLevel) target.level(), target.getX(), target.getY(), target.getZ(), Set.of(), target.getYRot(), target.getXRot(), true);
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Teleported to " + target.getName().getString() + " for " + formatMoney(TELEPORT_PRICE) + "."), false);
        return 1;
    }

    private static int buyXpLevel(CommandSourceStack source, String requestedLevels) throws CommandSyntaxException {
        if (!ensureFeature(source, "xp_trading")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        int levels = parseAmount(requestedLevels, 1);
        if (levels <= 0) {
            source.sendFailure(Component.literal("Use /xpmoney buy <levels>."));
            return 0;
        }

        int cost = 0;
        int simulatedLevel = player.experienceLevel;
        for (int i = 0; i < levels; i++) {
            cost += xpLevelBuyPrice(simulatedLevel);
            simulatedLevel++;
        }
        if (!charge(player, cost)) {
            source.sendFailure(Component.literal("Not enough money. " + levels + " level(s) cost " + formatMoney(cost) + "."));
            return 0;
        }

        player.giveExperienceLevels(levels);
        int finalCost = cost;
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Bought " + levels + " XP level(s) for " + formatMoney(finalCost) + "."), false);
        return levels;
    }

    private static int sellXpLevel(CommandSourceStack source, String requestedLevels) throws CommandSyntaxException {
        if (!ensureFeature(source, "xp_trading")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        int levels = parseAmount(requestedLevels, player.experienceLevel);
        if (levels <= 0 || player.experienceLevel < levels) {
            source.sendFailure(Component.literal("You do not have enough XP levels."));
            return 0;
        }

        int payout = 0;
        int simulatedLevel = player.experienceLevel - levels;
        for (int i = 0; i < levels; i++) {
            payout += xpLevelBuyPrice(simulatedLevel + i) / 2;
        }

        player.giveExperienceLevels(-levels);
        int balance = addMoney(player, payout);
        int finalPayout = payout;
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Sold " + levels + " XP level(s) for " + formatMoney(finalPayout) + ". Balance: " + formatMoney(balance)), false);
        return levels;
    }

    private static int repairHeldItem(CommandSourceStack source) throws CommandSyntaxException {
        if (!ensureFeature(source, "item_services")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !held.isDamageableItem() || held.getDamageValue() <= 0) {
            source.sendFailure(Component.literal("Hold a damaged repairable item."));
            return 0;
        }

        int cost = held.getDamageValue() * REPAIR_PRICE_PER_DAMAGE;
        if (!charge(player, cost)) {
            source.sendFailure(Component.literal("Repair costs " + formatMoney(cost) + "."));
            return 0;
        }
        held.setDamageValue(0);
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Repaired held item for " + formatMoney(cost) + "."), false);
        return 1;
    }

    private static int removeHeldEnchantments(CommandSourceStack source) throws CommandSyntaxException {
        if (!ensureFeature(source, "item_services")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !EnchantmentHelper.hasAnyEnchantments(held)) {
            source.sendFailure(Component.literal("Hold an enchanted item."));
            return 0;
        }
        if (!charge(player, REMOVE_ENCHANT_PRICE)) {
            source.sendFailure(Component.literal("Removing enchantments costs " + formatMoney(REMOVE_ENCHANT_PRICE) + "."));
            return 0;
        }

        held.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        held.set(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Removed enchantments for " + formatMoney(REMOVE_ENCHANT_PRICE) + "."), false);
        return 1;
    }

    private static int renameHeldItem(CommandSourceStack source, String name) throws CommandSyntaxException {
        if (!ensureFeature(source, "item_services")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        String cleanedName = name.trim();
        if (held.isEmpty() || cleanedName.isEmpty() || cleanedName.length() > 40) {
            source.sendFailure(Component.literal("Hold an item and use /itemservice rename <1-40 char name>."));
            return 0;
        }
        if (!charge(player, RENAME_PRICE)) {
            source.sendFailure(Component.literal("Renaming costs " + formatMoney(RENAME_PRICE) + "."));
            return 0;
        }

        held.set(DataComponents.CUSTOM_NAME, Component.literal(cleanedName));
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Renamed held item for " + formatMoney(RENAME_PRICE) + "."), false);
        return 1;
    }

    private static int startTermDeposit(CommandSourceStack source, int days, String requestedAmount) throws CommandSyntaxException {
        if (!ensureFeature(source, "investments")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        if (days != 7 && days != 14 && days != 30) {
            source.sendFailure(Component.literal("Fixed-term deposits support 7, 14, or 30 days."));
            return 0;
        }
        if (TERM_DEPOSITS.containsKey(player.getUUID())) {
            source.sendFailure(Component.literal("You already have an active fixed-term deposit."));
            return 0;
        }

        int amount = parseMoneyAmount(requestedAmount, getMoney(player));
        int fee = percentage(amount, TERM_DEPOSIT_FEE_PERCENT);
        if (amount <= 0 || getMoney(player) < amount + fee) {
            source.sendFailure(Component.literal("Not enough money. Deposit fee is 1%."));
            return 0;
        }

        setMoney(player, getMoney(player) - amount - fee);
        TERM_DEPOSITS.put(player.getUUID(), new TermDeposit(amount, economyDay(server(player)) + days, days));
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Started " + days + "-day fixed deposit for " + formatMoney(amount) + " plus " + formatMoney(fee) + " fee."), false);
        return 1;
    }

    private static int claimTermDeposit(CommandSourceStack source) throws CommandSyntaxException {
        if (!ensureFeature(source, "investments")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        TermDeposit deposit = TERM_DEPOSITS.get(player.getUUID());
        if (deposit == null) {
            source.sendFailure(Component.literal("You have no active fixed-term deposit."));
            return 0;
        }
        long day = economyDay(server(player));
        if (day < deposit.maturityDay()) {
            source.sendFailure(Component.literal("Deposit matures in " + (deposit.maturityDay() - day) + " day(s)."));
            return 0;
        }

        int interestPercent = deposit.days() == 7 ? 3 : deposit.days() == 14 ? 7 : 18;
        int payout = deposit.amount() + percentage(deposit.amount(), interestPercent);
        TERM_DEPOSITS.remove(player.getUUID());
        int balance = addMoney(player, payout);
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Fixed deposit claimed: " + formatMoney(payout) + ". Balance: " + formatMoney(balance)), false);
        return 1;
    }

    private static int takeLoan(CommandSourceStack source, String requestedAmount) throws CommandSyntaxException {
        if (!ensureFeature(source, "loans")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        int amount = parseMoneyAmount(requestedAmount, 500000);
        if (amount <= 0 || amount > 500000) {
            source.sendFailure(Component.literal("Emergency loans are limited to " + formatMoney(500000) + "."));
            return 0;
        }
        if (LOANS.getOrDefault(player.getUUID(), 0) > 0) {
            source.sendFailure(Component.literal("Repay your current loan first."));
            return 0;
        }

        LOANS.put(player.getUUID(), amount + percentage(amount, 20));
        int balance = addMoney(player, amount);
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Loan received: " + formatMoney(amount) + ". Repay total: " + formatMoney(LOANS.get(player.getUUID())) + ". Balance: " + formatMoney(balance)), false);
        return 1;
    }

    private static int repayLoan(CommandSourceStack source, String requestedAmount) throws CommandSyntaxException {
        if (!ensureFeature(source, "loans")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        int loan = LOANS.getOrDefault(player.getUUID(), 0);
        if (loan <= 0) {
            source.sendFailure(Component.literal("You have no active loan."));
            return 0;
        }
        int amount = Math.min(parseMoneyAmount(requestedAmount, getMoney(player)), loan);
        if (amount <= 0 || getMoney(player) < amount) {
            source.sendFailure(Component.literal("Not enough money."));
            return 0;
        }

        setMoney(player, getMoney(player) - amount);
        int remaining = loan - amount;
        if (remaining == 0) {
            LOANS.remove(player.getUUID());
        } else {
            LOANS.put(player.getUUID(), remaining);
        }
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Loan payment: " + formatMoney(amount) + ". Remaining: " + formatMoney(remaining) + "."), false);
        return 1;
    }

    private static int sellNearestPet(CommandSourceStack source) throws CommandSyntaxException {
        if (!ensureFeature(source, "pets")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        TamableAnimal pet = nearestOwnedPet(player);
        if (pet == null) {
            source.sendFailure(Component.literal("No owned tamed pet found within 8 blocks."));
            return 0;
        }

        PET_INSURANCE_OWNERS.remove(pet.getUUID());
        pet.remove(RemovalReason.DISCARDED);
        int balance = addMoney(player, PET_SALE_PRICE);
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Sold nearby pet for " + formatMoney(PET_SALE_PRICE) + ". Balance: " + formatMoney(balance)), false);
        return 1;
    }

    private static int insureNearestPet(CommandSourceStack source) throws CommandSyntaxException {
        if (!ensureFeature(source, "pets")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        TamableAnimal pet = nearestOwnedPet(player);
        if (pet == null) {
            source.sendFailure(Component.literal("No owned tamed pet found within 8 blocks."));
            return 0;
        }
        if (PET_INSURANCE_OWNERS.containsKey(pet.getUUID())) {
            source.sendSuccess(() -> Component.literal("That pet is already insured."), false);
            return 1;
        }
        if (!charge(player, PET_INSURANCE_PRICE)) {
            source.sendFailure(Component.literal("Pet insurance costs " + formatMoney(PET_INSURANCE_PRICE) + "."));
            return 0;
        }

        PET_INSURANCE_OWNERS.put(pet.getUUID(), player.getUUID());
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Pet insured. Payout on death: " + formatMoney(PET_INSURANCE_PAYOUT) + "."), false);
        return 1;
    }

    private static int buyPlayerInsurance(CommandSourceStack source) throws CommandSyntaxException {
        if (!ensureFeature(source, "insurance")) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        if (PLAYER_INSURANCE.getOrDefault(player.getUUID(), false)) {
            source.sendSuccess(() -> Component.literal("You already have death insurance. Your next death loses 2% instead of 10%."), false);
            return 1;
        }
        if (!charge(player, PLAYER_INSURANCE_PRICE)) {
            source.sendFailure(Component.literal("Death insurance costs " + formatMoney(PLAYER_INSURANCE_PRICE) + "."));
            return 0;
        }

        PLAYER_INSURANCE.put(player.getUUID(), true);
        savePlayerData();
        sendEconomyState(player);
        source.sendSuccess(() -> Component.literal("Death insurance bought. Your next death loses 2% instead of 10%."), false);
        return 1;
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestStrings(com.mojang.brigadier.suggestion.SuggestionsBuilder builder, Iterable<String> values) {
        for (String value : values) {
            builder.suggest(value);
        }
        return builder.buildFuture();
    }

    private static DailyQuest dailyQuest(ServerPlayer player) {
        List<ShopItem> candidates = SHOP_ITEM_DEFINITIONS.stream()
                .map(ShopItemDefinition::toShopItem)
                .filter(item -> item.sellPrice() > 0 && item.sellPrice() <= 500)
                .toList();
        int index = Math.floorMod((int) (economyDay(server(player)) + worldQuestSalt + player.getUUID().getLeastSignificantBits()), candidates.size());
        ShopItem item = candidates.get(index);
        int count = Math.max(8, Math.min(96, 2000 / Math.max(1, item.sellPrice())));
        int reward = count * item.sellPrice() * 3;
        return new DailyQuest(item, count, reward);
    }

    private static ProfessionQuest professionQuestFor(ServerPlayer player) {
        Profession profession = PROFESSIONS.get(PLAYER_PROFESSIONS.get(player.getUUID()));
        if (profession == null) {
            return null;
        }

        ShopItem item = getShopItem(profession.questItem());
        if (item == null) {
            return null;
        }
        int count = Math.max(64, Math.min(512, 15000 / Math.max(1, item.sellPrice())));
        return new ProfessionQuest(profession.id(), item, count, count * item.sellPrice() * 5);
    }

    private static boolean removeInventoryItems(ServerPlayer player, Item item, int amount) {
        if (countInventoryItems(player, item) < amount) {
            return false;
        }

        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(item)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
        }
        return true;
    }

    private static int xpLevelBuyPrice(int currentLevel) {
        if (currentLevel < 10) {
            return 10000;
        }
        if (currentLevel < 15) {
            return 20000;
        }
        if (currentLevel < 30) {
            return 30000;
        }
        return (300 + ((currentLevel - 30) / 5 + 1) * 100) * 100;
    }

    private static TamableAnimal nearestOwnedPet(ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(8.0D);
        Predicate<TamableAnimal> ownedByPlayer = pet -> pet.isTame() && pet.isOwnedBy(player);
        return player.level().getEntities(player, area, entity -> entity instanceof TamableAnimal)
                .stream()
                .map(entity -> (TamableAnimal) entity)
                .filter(ownedByPlayer)
                .min((left, right) -> Double.compare(left.distanceToSqr(player), right.distanceToSqr(player)))
                .orElse(null);
    }

    private static void afterEntityDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource damageSource) {
        if (entity instanceof ServerPlayer player) {
            applyDeathMoneyLoss(player);
            return;
        }

        UUID ownerUuid = PET_INSURANCE_OWNERS.remove(entity.getUUID());
        if (ownerUuid == null || entity.level().isClientSide()) {
            return;
        }

        ServerPlayer owner = ((ServerLevel) entity.level()).getServer().getPlayerList().getPlayer(ownerUuid);
        if (owner != null) {
            int balance = addMoney(owner, PET_INSURANCE_PAYOUT);
            owner.sendSystemMessage(Component.literal("Pet insurance paid " + formatMoney(PET_INSURANCE_PAYOUT) + ". Balance: " + formatMoney(balance)));
            sendEconomyState(owner);
        }
        savePlayerData();
    }

    private static void applyDeathMoneyLoss(ServerPlayer player) {
        int balance = getMoney(player);
        if (balance <= 0) {
            return;
        }

        boolean insured = PLAYER_INSURANCE.getOrDefault(player.getUUID(), false);
        int loss = percentage(balance, insured ? INSURED_DEATH_LOSS_PERCENT : DEATH_LOSS_PERCENT);
        setMoney(player, Math.max(0, balance - loss));
        if (insured) {
            PLAYER_INSURANCE.put(player.getUUID(), false);
        }
        savePlayerData();
        sendEconomyState(player);
        player.sendSystemMessage(Component.literal("Death fee: " + formatMoney(loss) + (insured ? " with insurance." : ". Buy /insurance buy to reduce the next death fee.")));
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

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestShopItems(com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        for (String itemName : ITEM_ALIASES.keySet()) {
            builder.suggest(itemName);
        }
        return builder.buildFuture();
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

    private static boolean charge(ServerPlayer player, int amount) {
        if (amount < 0 || getMoney(player) < amount) {
            return false;
        }
        setMoney(player, getMoney(player) - amount);
        return true;
    }

    private static int percentage(int amount, int percent) {
        return BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .intValueExact();
    }

    private static void setMoney(ServerPlayer player, int balance) {
        BALANCES.put(player.getUUID(), balance);
    }

    private static void setInvestment(ServerPlayer player, int balance) {
        INVESTMENTS.put(player.getUUID(), balance);
    }

    private static boolean isUnlocked(ServerPlayer player, ShopItem option) {
        return UNLOCKED_ITEMS.getOrDefault(player.getUUID(), Set.of()).contains(option.primaryName());
    }

    private static void unlockItem(ServerPlayer player, ShopItem option) {
        unlockItem(player, option, "Found");
    }

    private static void unlockItem(ServerPlayer player, ShopItem option, String source) {
        boolean changed = UNLOCKED_ITEMS.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>()).add(option.primaryName());
        String previous = UNLOCK_SOURCES.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>()).putIfAbsent(option.primaryName(), source);
        if (changed || previous == null) {
            savePlayerData();
        }
    }

    private static int unlockPrice(ShopItem option) {
        return option.buyPrice() * 10;
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

    private static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
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

    private static int parseFeeInclusiveMoneyAmount(String requestedAmount, int balance, int feePercent) {
        String normalized = requestedAmount.toLowerCase(Locale.ROOT);
        if (!normalized.equals("all")) {
            return parseMoneyAmount(requestedAmount, balance);
        }

        int amount = balance * 100 / (100 + feePercent);
        while (amount > 0 && amount + percentage(amount, feePercent) > balance) {
            amount--;
        }
        return amount;
    }

    private static String formatMoney(int units) {
        return "$" + (units / 100) + "." + String.format(Locale.ROOT, "%02d", Math.abs(units % 100));
    }

    private static int saleValue(ServerPlayer player, ShopItem option, int count) {
        int base = option.sellPrice() * count;
        if (isDailyDeal(server(player), option)) {
            base *= 2;
        }

        int bonusPercent = professionMatches(player, option) ? professionBonusPercent(player) : 0;
        int total = base + percentage(base, bonusPercent);
        if (bonusPercent > 0) {
            PROFESSION_SALES.put(player.getUUID(), PROFESSION_SALES.getOrDefault(player.getUUID(), 0) + base);
        }
        return total;
    }

    private static int professionBonusPercent(ServerPlayer player) {
        return 5 + Math.min(20, PROFESSION_SALES.getOrDefault(player.getUUID(), 0) / 10000);
    }

    private static boolean professionMatches(ServerPlayer player, ShopItem option) {
        Profession profession = PROFESSIONS.get(PLAYER_PROFESSIONS.get(player.getUUID()));
        return profession != null && profession.matches(option.primaryName());
    }

    private static boolean isDailyDeal(MinecraftServer server, ShopItem option) {
        ShopItem deal = dailyDeal(server);
        return deal != null && deal.primaryName().equals(option.primaryName());
    }

    private static ShopItem dailyDeal(MinecraftServer server) {
        if (SHOP_ITEM_DEFINITIONS.isEmpty()) {
            return null;
        }
        int index = Math.floorMod((int) economyDay(server) + worldQuestSalt, SHOP_ITEM_DEFINITIONS.size());
        return SHOP_ITEM_DEFINITIONS.get(index).toShopItem();
    }

    private static long economyDay(MinecraftServer server) {
        return server.overworld().getOverworldClockTime() / 24000L;
    }

    private static MinecraftServer server(ServerPlayer player) {
        return ((ServerLevel) player.level()).getServer();
    }

    private static boolean ensureFeature(CommandSourceStack source, String feature) {
        if (featureSettings.isEnabled(feature)) {
            return true;
        }
        source.sendFailure(Component.literal("Advanced Economics feature disabled by server settings: " + feature));
        return false;
    }

    private static void reportDependencyState() {
        boolean fabricApiLoaded = FabricLoader.getInstance().isModLoaded("fabric-api");
        if (!fabricApiLoaded) {
            System.err.println("Advanced Economics requires Fabric API. Fabric should prevent launch through fabric.mod.json dependencies.");
            return;
        }
        System.out.println("Advanced Economics dependency check passed: Fabric API is installed.");
    }

    private static void loadDatapackPriceOverrides(MinecraftServer server) {
        List<Resource> resources = server.getResourceManager().getResourceStack(DATAPACK_PRICE_OVERRIDES);
        if (resources.isEmpty()) {
            return;
        }

        for (Resource resource : resources) {
            try (java.io.Reader reader = resource.openAsReader()) {
                JsonObject prices = GSON.fromJson(reader, JsonObject.class);
                if (prices == null) {
                    continue;
                }
                for (Map.Entry<String, JsonElement> entry : prices.entrySet()) {
                    applyDatapackPrice(entry.getKey(), entry.getValue());
                }
            } catch (RuntimeException | IOException exception) {
                System.err.println("Advanced Economics ignored datapack price override from " + resource.sourcePackId() + ": " + exception.getMessage());
            }
        }
        enforceValuableMinimumPrices();
    }

    private static void applyDatapackPrice(String requestedItem, JsonElement value) {
        String itemName = ITEM_ALIASES.get(requestedItem.trim().toLowerCase(Locale.ROOT));
        if (itemName == null) {
            itemName = requestedItem.trim().toLowerCase(Locale.ROOT);
        }
        if (!SELL_PRICES.containsKey(itemName)) {
            System.err.println("Advanced Economics ignored unknown datapack price item: " + requestedItem);
            return;
        }

        String rawValue = value.isJsonPrimitive() ? value.getAsString() : "";
        try {
            SELL_PRICES.put(itemName, parseMoney(rawValue));
        } catch (RuntimeException exception) {
            System.err.println("Advanced Economics ignored invalid datapack price for " + itemName + ": " + rawValue);
        }
    }

    private static void loadFeatureSettings() {
        if (!Files.exists(SETTINGS_CONFIG_PATH)) {
            saveFeatureSettings();
            return;
        }

        try {
            FeatureSettings loaded = GSON.fromJson(Files.readString(SETTINGS_CONFIG_PATH), FeatureSettings.class);
            if (loaded != null) {
                featureSettings = loaded.withDefaults();
            }
        } catch (RuntimeException | IOException exception) {
            System.err.println("Advanced Economics could not read " + SETTINGS_CONFIG_PATH + ": " + exception.getMessage());
        }
        saveFeatureSettings();
    }

    private static void saveFeatureSettings() {
        try {
            Files.createDirectories(SETTINGS_CONFIG_PATH.getParent());
            Files.writeString(SETTINGS_CONFIG_PATH, GSON.toJson(featureSettings.withDefaults()));
        } catch (IOException exception) {
            System.err.println("Advanced Economics could not write " + SETTINGS_CONFIG_PATH + ": " + exception.getMessage());
        }
    }

    private static int getGambleCashback(ShopItem option, int lostCount) {
        if (option == null || option.sellPrice() <= 0) {
            return GAMBLE_UNPRICED_CASHBACK_CENTS;
        }

        return Math.max(1, lostCount * option.sellPrice() * GAMBLE_CASHBACK_PERCENT / 100);
    }

    private static void addGambleHistory(ServerPlayer player, String message) {
        List<String> history = GAMBLE_HISTORY.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>());
        history.addFirst(message);
        while (history.size() > GAMBLE_HISTORY_LIMIT) {
            history.removeLast();
        }
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
        savePlayerData();
    }

    private static void rememberPlayerInventoryItems(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            rememberUnlockedItems(player);
        }
    }

    private static void rememberUnlockedItems(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ShopItem option = getShopItem(player.getInventory().getItem(slot));
            if (option != null) {
                unlockItem(player, option);
            }
        }
    }

    private static void syncPlayerEconomyStates(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendEconomyState(player);
        }
    }

    private static void savePlayerDataPeriodically(MinecraftServer server) {
        if (server.getTickCount() % 1200 == 0) {
            savePlayerData();
        }
    }

    private static void sendEconomyState(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, EconomyStatePayload.TYPE)) {
            return;
        }

        rememberUnlockedItems(player);
        List<EconomyStatePayload.Entry> entries = new ArrayList<>();
        Set<String> unlocked = UNLOCKED_ITEMS.getOrDefault(player.getUUID(), Set.of());
        Map<String, String> sources = UNLOCK_SOURCES.getOrDefault(player.getUUID(), Map.of());
        for (ShopItemDefinition definition : SHOP_ITEM_DEFINITIONS) {
            ShopItem item = definition.toShopItem();
            String primaryName = item.primaryName();
            entries.add(new EconomyStatePayload.Entry(
                    BuiltInRegistries.ITEM.getKey(item.item()).toString(),
                    item.displayName(),
                    primaryName,
                    item.sellPrice(),
                    item.buyPrice(),
                    unlockPrice(item),
                    unlocked.contains(primaryName),
                    sources.getOrDefault(primaryName, ""),
                    countInventoryItems(player, item.item())
            ));
        }

        ShopItem dailyDeal = dailyDeal(server(player));
        DailyQuest dailyQuest = dailyQuest(player);
        int questProgress = Math.min(countInventoryItems(player, dailyQuest.item().item()), dailyQuest.count());
        ServerPlayNetworking.send(player, new EconomyStatePayload(
                getMoney(player),
                getInvestment(player),
                LOANS.getOrDefault(player.getUUID(), 0),
                PLAYER_PROFESSIONS.getOrDefault(player.getUUID(), ""),
                PLAYER_PROFESSIONS.containsKey(player.getUUID()) ? professionBonusPercent(player) : 0,
                dailyDeal == null ? "" : dailyDeal.displayName(),
                new EconomyStatePayload.DailyQuestState(
                        BuiltInRegistries.ITEM.getKey(dailyQuest.item().item()).toString(),
                        dailyQuest.item().displayName(),
                        dailyQuest.count(),
                        questProgress,
                        dailyQuest.reward(),
                        DAILY_QUEST_DAYS.getOrDefault(player.getUUID(), -1L) == economyDay(server(player)),
                        HIDDEN_DAILY_QUEST_WIDGETS.contains(player.getUUID())
                ),
                featureSettings.toPayload(),
                entries,
                List.copyOf(GAMBLE_HISTORY.getOrDefault(player.getUUID(), List.of()))
        ));
    }

    private static void loadWorldPlayerData(MinecraftServer server) {
        playerDataPath = server.getWorldPath(LevelResource.ROOT)
                .resolve("advanced-economics")
                .resolve("player-data.json");
        worldQuestSalt = playerDataPath.getParent().toString().hashCode();
        clearPlayerData();
        lastInvestmentDay = -1L;
        loadPlayerData();
    }

    private static void resetLoadedWorldPlayerData() {
        clearPlayerData();
        DUEL_REQUESTS.clear();
        playerDataPath = FALLBACK_PLAYER_DATA_PATH;
        worldQuestSalt = 0;
        lastInvestmentDay = -1L;
    }

    private static void clearPlayerData() {
        BALANCES.clear();
        INVESTMENTS.clear();
        LOANS.clear();
        TERM_DEPOSITS.clear();
        PLAYER_PROFESSIONS.clear();
        PROFESSION_SALES.clear();
        DAILY_QUEST_DAYS.clear();
        COMPLETED_PROFESSION_QUESTS.clear();
        PLAYER_INSURANCE.clear();
        PET_INSURANCE_OWNERS.clear();
        UNLOCKED_ITEMS.clear();
        UNLOCK_SOURCES.clear();
        GAMBLE_HISTORY.clear();
        HIDDEN_DAILY_QUEST_WIDGETS.clear();
        WELCOME_HINT_SHOWN.clear();
    }

    private static int countInventoryItems(ServerPlayer player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void loadPlayerData() {
        if (!Files.exists(playerDataPath)) {
            return;
        }

        try {
            PlayerDataFile data = GSON.fromJson(Files.readString(playerDataPath), PlayerDataFile.class);
            if (data == null || data.players == null) {
                return;
            }

            for (Map.Entry<String, PlayerData> entry : data.players.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    PlayerData playerData = entry.getValue();
                    if (playerData == null) {
                        continue;
                    }

                    BALANCES.put(uuid, Math.max(0, playerData.money));
                    INVESTMENTS.put(uuid, Math.max(0, playerData.investment));
                    LOANS.put(uuid, Math.max(0, playerData.loan));
                    if (playerData.termDeposit != null && playerData.termDeposit.amount > 0) {
                        TERM_DEPOSITS.put(uuid, new TermDeposit(Math.max(0, playerData.termDeposit.amount), playerData.termDeposit.maturityDay, playerData.termDeposit.days));
                    }
                    if (playerData.profession != null && PROFESSIONS.containsKey(playerData.profession)) {
                        PLAYER_PROFESSIONS.put(uuid, playerData.profession);
                    }
                    PROFESSION_SALES.put(uuid, Math.max(0, playerData.professionSales));
                    DAILY_QUEST_DAYS.put(uuid, playerData.dailyQuestDay);
                    if (playerData.dailyQuestWidgetHidden) {
                        HIDDEN_DAILY_QUEST_WIDGETS.add(uuid);
                    }
                    COMPLETED_PROFESSION_QUESTS.put(uuid, new HashSet<>(playerData.completedProfessionQuests == null ? List.of() : playerData.completedProfessionQuests));
                    PLAYER_INSURANCE.put(uuid, playerData.playerInsurance);
                    if (playerData.insuredPets != null) {
                        for (String petUuid : playerData.insuredPets) {
                            try {
                                PET_INSURANCE_OWNERS.put(UUID.fromString(petUuid), uuid);
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                    UNLOCKED_ITEMS.put(uuid, new HashSet<>(playerData.unlockedItems == null ? List.of() : playerData.unlockedItems));
                    UNLOCK_SOURCES.put(uuid, new HashMap<>(playerData.unlockSources == null ? Map.of() : playerData.unlockSources));
                    GAMBLE_HISTORY.put(uuid, trimHistory(playerData.gambleHistory == null ? List.of() : playerData.gambleHistory));
                    if (playerData.welcomeHintShown) {
                        WELCOME_HINT_SHOWN.add(uuid);
                    }
                } catch (IllegalArgumentException exception) {
                    System.err.println("Advanced Economics ignored invalid player data UUID: " + entry.getKey());
                }
            }
        } catch (RuntimeException | IOException exception) {
            System.err.println("Advanced Economics could not read " + playerDataPath + ": " + exception.getMessage());
        }
    }

    private static void savePlayerData() {
        PlayerDataFile data = new PlayerDataFile();
        for (UUID uuid : allTrackedPlayers()) {
            PlayerData playerData = new PlayerData();
            playerData.money = BALANCES.getOrDefault(uuid, 0);
            playerData.investment = INVESTMENTS.getOrDefault(uuid, 0);
            playerData.loan = LOANS.getOrDefault(uuid, 0);
            TermDeposit termDeposit = TERM_DEPOSITS.get(uuid);
            if (termDeposit != null) {
                playerData.termDeposit = new TermDepositData();
                playerData.termDeposit.amount = termDeposit.amount();
                playerData.termDeposit.maturityDay = termDeposit.maturityDay();
                playerData.termDeposit.days = termDeposit.days();
            }
            playerData.profession = PLAYER_PROFESSIONS.get(uuid);
            playerData.professionSales = PROFESSION_SALES.getOrDefault(uuid, 0);
            playerData.dailyQuestDay = DAILY_QUEST_DAYS.getOrDefault(uuid, -1L);
            playerData.dailyQuestWidgetHidden = HIDDEN_DAILY_QUEST_WIDGETS.contains(uuid);
            playerData.completedProfessionQuests = COMPLETED_PROFESSION_QUESTS.getOrDefault(uuid, Set.of()).stream().sorted().toList();
            playerData.playerInsurance = PLAYER_INSURANCE.getOrDefault(uuid, false);
            playerData.insuredPets = PET_INSURANCE_OWNERS.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(uuid))
                    .map(entry -> entry.getKey().toString())
                    .sorted()
                    .toList();
            playerData.unlockedItems = UNLOCKED_ITEMS.getOrDefault(uuid, Set.of()).stream().sorted().toList();
            playerData.unlockSources = new HashMap<>(UNLOCK_SOURCES.getOrDefault(uuid, Map.of()));
            playerData.gambleHistory = trimHistory(GAMBLE_HISTORY.getOrDefault(uuid, List.of()));
            playerData.welcomeHintShown = WELCOME_HINT_SHOWN.contains(uuid);
            data.players.put(uuid.toString(), playerData);
        }

        try {
            Files.createDirectories(playerDataPath.getParent());
            Files.writeString(playerDataPath, GSON.toJson(data));
        } catch (IOException exception) {
            System.err.println("Advanced Economics could not write " + playerDataPath + ": " + exception.getMessage());
        }
    }

    private static Set<UUID> allTrackedPlayers() {
        Set<UUID> uuids = new HashSet<>();
        uuids.addAll(BALANCES.keySet());
        uuids.addAll(INVESTMENTS.keySet());
        uuids.addAll(LOANS.keySet());
        uuids.addAll(TERM_DEPOSITS.keySet());
        uuids.addAll(PLAYER_PROFESSIONS.keySet());
        uuids.addAll(PROFESSION_SALES.keySet());
        uuids.addAll(DAILY_QUEST_DAYS.keySet());
        uuids.addAll(HIDDEN_DAILY_QUEST_WIDGETS);
        uuids.addAll(COMPLETED_PROFESSION_QUESTS.keySet());
        uuids.addAll(PLAYER_INSURANCE.keySet());
        uuids.addAll(PET_INSURANCE_OWNERS.values());
        uuids.addAll(UNLOCKED_ITEMS.keySet());
        uuids.addAll(UNLOCK_SOURCES.keySet());
        uuids.addAll(GAMBLE_HISTORY.keySet());
        uuids.addAll(WELCOME_HINT_SHOWN);
        return uuids;
    }

    private static List<String> trimHistory(List<String> history) {
        List<String> trimmed = new ArrayList<>();
        for (String message : history) {
            if (message == null || message.isBlank()) {
                continue;
            }
            trimmed.add(message);
            if (trimmed.size() >= GAMBLE_HISTORY_LIMIT) {
                break;
            }
        }
        return trimmed;
    }

    private static void loadPriceConfig() {
        resetDefaultSellPrices();
        createDefaultPriceConfigIfMissing();

        try {
            List<String> lines = Files.readAllLines(PRICE_CONFIG_PATH);
            for (String line : lines) {
                loadPriceConfigLine(line);
            }
            enforceValuableMinimumPrices();
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
        lines.add("# Buy prices are always 4x the sell price.");
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

    private static void enforceValuableMinimumPrices() {
        setMinimumSellPrice("diamond", 750);
        setMinimumSellPrice("diamond_ore", 900);
        setMinimumSellPrice("diamond_sword", 1500);
        setMinimumSellPrice("diamond_shovel", 750);
        setMinimumSellPrice("diamond_pickaxe", 2400);
        setMinimumSellPrice("diamond_axe", 2250);
        setMinimumSellPrice("diamond_hoe", 1500);
        setMinimumSellPrice("netherite_scrap", 1800);
        setMinimumSellPrice("netherite_ingot", 7200);
        setMinimumSellPrice("ancient_debris", 1800);
        setMinimumSellPrice("netherite_sword", 3750);
        setMinimumSellPrice("netherite_shovel", 3000);
        setMinimumSellPrice("netherite_pickaxe", 4650);
        setMinimumSellPrice("netherite_axe", 4500);
        setMinimumSellPrice("netherite_hoe", 3750);
    }

    private static void setMinimumSellPrice(String itemName, int cents) {
        if (SELL_PRICES.containsKey(itemName) && SELL_PRICES.get(itemName) < cents) {
            SELL_PRICES.put(itemName, cents);
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
        register(Items.DIAMOND, "diamond", "diamond", 750, "minecraft:diamond");
        register(Items.NETHERITE_SCRAP, "netherite scrap", "netherite_scrap", 1800, "minecraft:netherite_scrap");
        register(Items.NETHERITE_INGOT, "netherite ingot", "netherite_ingot", 7200, "netherite", "minecraft:netherite_ingot");
        register(Items.EMERALD, "emerald", "emerald", 400, "minecraft:emerald");

        register(Items.COAL_ORE, "coal ore", "coal_ore", 25, "minecraft:coal_ore");
        register(Items.COPPER_ORE, "copper ore", "copper_ore", 25, "minecraft:copper_ore");
        register(Items.IRON_ORE, "iron ore", "iron_ore", 70, "minecraft:iron_ore");
        register(Items.GOLD_ORE, "gold ore", "gold_ore", 70, "minecraft:gold_ore");
        register(Items.REDSTONE_ORE, "redstone ore", "redstone_ore", 50, "minecraft:redstone_ore");
        register(Items.LAPIS_ORE, "lapis ore", "lapis_ore", 60, "minecraft:lapis_ore");
        register(Items.DIAMOND_ORE, "diamond ore", "diamond_ore", 900, "minecraft:diamond_ore");
        register(Items.EMERALD_ORE, "emerald ore", "emerald_ore", 500, "minecraft:emerald_ore");
        register(Items.NETHER_QUARTZ_ORE, "nether quartz ore", "nether_quartz_ore", 40, "minecraft:nether_quartz_ore");
        register(Items.ANCIENT_DEBRIS, "ancient debris", "ancient_debris", 1800, "minecraft:ancient_debris");

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
        register(Items.PORKCHOP, "raw porkchop", "porkchop", 15, "minecraft:porkchop");
        register(Items.COOKED_PORKCHOP, "cooked porkchop", "cooked_porkchop", 25, "minecraft:cooked_porkchop");
        register(Items.CHICKEN, "raw chicken", "chicken", 12, "minecraft:chicken");
        register(Items.COOKED_CHICKEN, "cooked chicken", "cooked_chicken", 22, "minecraft:cooked_chicken");
        register(Items.MUTTON, "raw mutton", "mutton", 12, "minecraft:mutton");
        register(Items.COOKED_MUTTON, "cooked mutton", "cooked_mutton", 22, "minecraft:cooked_mutton");
        register(Items.COD, "raw cod", "cod", 10, "minecraft:cod");
        register(Items.COOKED_COD, "cooked cod", "cooked_cod", 18, "minecraft:cooked_cod");
        register(Items.SALMON, "raw salmon", "salmon", 12, "minecraft:salmon");
        register(Items.COOKED_SALMON, "cooked salmon", "cooked_salmon", 22, "minecraft:cooked_salmon");
        register(Items.COOKIE, "cookie", "cookie", 8, "minecraft:cookie");
        register(Items.PUMPKIN_PIE, "pumpkin pie", "pumpkin_pie", 30, "minecraft:pumpkin_pie");
        register(Items.GOLDEN_APPLE, "golden apple", "golden_apple", 900, "minecraft:golden_apple");
        register(Items.ENCHANTED_GOLDEN_APPLE, "enchanted golden apple", "enchanted_golden_apple", 8000, "minecraft:enchanted_golden_apple");

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

        register(Items.WOODEN_SWORD, "wooden sword", "wooden_sword", 12, "minecraft:wooden_sword");
        register(Items.WOODEN_SHOVEL, "wooden shovel", "wooden_shovel", 8, "minecraft:wooden_shovel");
        register(Items.WOODEN_PICKAXE, "wooden pickaxe", "wooden_pickaxe", 15, "minecraft:wooden_pickaxe");
        register(Items.WOODEN_AXE, "wooden axe", "wooden_axe", 15, "minecraft:wooden_axe");
        register(Items.WOODEN_HOE, "wooden hoe", "wooden_hoe", 8, "minecraft:wooden_hoe");
        register(Items.COPPER_SWORD, "copper sword", "copper_sword", 120, "minecraft:copper_sword");
        register(Items.COPPER_SHOVEL, "copper shovel", "copper_shovel", 90, "minecraft:copper_shovel");
        register(Items.COPPER_PICKAXE, "copper pickaxe", "copper_pickaxe", 240, "minecraft:copper_pickaxe");
        register(Items.COPPER_AXE, "copper axe", "copper_axe", 270, "minecraft:copper_axe");
        register(Items.COPPER_HOE, "copper hoe", "copper_hoe", 180, "minecraft:copper_hoe");
        register(Items.STONE_SWORD, "stone sword", "stone_sword", 20, "minecraft:stone_sword");
        register(Items.STONE_SHOVEL, "stone shovel", "stone_shovel", 12, "minecraft:stone_shovel");
        register(Items.STONE_PICKAXE, "stone pickaxe", "stone_pickaxe", 25, "minecraft:stone_pickaxe");
        register(Items.STONE_AXE, "stone axe", "stone_axe", 25, "minecraft:stone_axe");
        register(Items.STONE_HOE, "stone hoe", "stone_hoe", 12, "minecraft:stone_hoe");
        register(Items.GOLDEN_SWORD, "golden sword", "golden_sword", 180, "minecraft:golden_sword");
        register(Items.GOLDEN_SHOVEL, "golden shovel", "golden_shovel", 90, "minecraft:golden_shovel");
        register(Items.GOLDEN_PICKAXE, "golden pickaxe", "golden_pickaxe", 300, "gold_pickaxe", "minecraft:golden_pickaxe");
        register(Items.GOLDEN_AXE, "golden axe", "golden_axe", 270, "gold_axe", "minecraft:golden_axe");
        register(Items.GOLDEN_HOE, "golden hoe", "golden_hoe", 180, "gold_hoe", "minecraft:golden_hoe");
        register(Items.IRON_SWORD, "iron sword", "iron_sword", 180, "minecraft:iron_sword");
        register(Items.IRON_SHOVEL, "iron shovel", "iron_shovel", 90, "minecraft:iron_shovel");
        register(Items.IRON_PICKAXE, "iron pickaxe", "iron_pickaxe", 300, "minecraft:iron_pickaxe");
        register(Items.IRON_AXE, "iron axe", "iron_axe", 270, "minecraft:iron_axe");
        register(Items.IRON_HOE, "iron hoe", "iron_hoe", 180, "minecraft:iron_hoe");
        register(Items.DIAMOND_SWORD, "diamond sword", "diamond_sword", 1500, "minecraft:diamond_sword");
        register(Items.DIAMOND_SHOVEL, "diamond shovel", "diamond_shovel", 750, "minecraft:diamond_shovel");
        register(Items.DIAMOND_PICKAXE, "diamond pickaxe", "diamond_pickaxe", 2400, "minecraft:diamond_pickaxe");
        register(Items.DIAMOND_AXE, "diamond axe", "diamond_axe", 2250, "minecraft:diamond_axe");
        register(Items.DIAMOND_HOE, "diamond hoe", "diamond_hoe", 1500, "minecraft:diamond_hoe");
        register(Items.NETHERITE_SWORD, "netherite sword", "netherite_sword", 3750, "minecraft:netherite_sword");
        register(Items.NETHERITE_SHOVEL, "netherite shovel", "netherite_shovel", 3000, "minecraft:netherite_shovel");
        register(Items.NETHERITE_PICKAXE, "netherite pickaxe", "netherite_pickaxe", 4650, "minecraft:netherite_pickaxe");
        register(Items.NETHERITE_AXE, "netherite axe", "netherite_axe", 4500, "minecraft:netherite_axe");
        register(Items.NETHERITE_HOE, "netherite hoe", "netherite_hoe", 3750, "minecraft:netherite_hoe");
        register(Items.BOW, "bow", "bow", 60, "minecraft:bow");
        register(Items.CROSSBOW, "crossbow", "crossbow", 180, "minecraft:crossbow");
        register(Items.TRIDENT, "trident", "trident", 1800, "minecraft:trident");
        register(Items.MACE, "mace", "mace", 2400, "minecraft:mace");
        register(Items.SHIELD, "shield", "shield", 180, "minecraft:shield");
        register(Items.LEATHER_HELMET, "leather helmet", "leather_helmet", 60, "minecraft:leather_helmet");
        register(Items.LEATHER_CHESTPLATE, "leather chestplate", "leather_chestplate", 120, "minecraft:leather_chestplate");
        register(Items.LEATHER_LEGGINGS, "leather leggings", "leather_leggings", 100, "minecraft:leather_leggings");
        register(Items.LEATHER_BOOTS, "leather boots", "leather_boots", 50, "minecraft:leather_boots");
        register(Items.COPPER_HELMET, "copper helmet", "copper_helmet", 250, "minecraft:copper_helmet");
        register(Items.COPPER_CHESTPLATE, "copper chestplate", "copper_chestplate", 400, "minecraft:copper_chestplate");
        register(Items.COPPER_LEGGINGS, "copper leggings", "copper_leggings", 350, "minecraft:copper_leggings");
        register(Items.COPPER_BOOTS, "copper boots", "copper_boots", 200, "minecraft:copper_boots");
        register(Items.IRON_HELMET, "iron helmet", "iron_helmet", 450, "minecraft:iron_helmet");
        register(Items.IRON_CHESTPLATE, "iron chestplate", "iron_chestplate", 720, "minecraft:iron_chestplate");
        register(Items.IRON_LEGGINGS, "iron leggings", "iron_leggings", 630, "minecraft:iron_leggings");
        register(Items.IRON_BOOTS, "iron boots", "iron_boots", 360, "minecraft:iron_boots");
        register(Items.GOLDEN_HELMET, "golden helmet", "golden_helmet", 450, "minecraft:golden_helmet");
        register(Items.GOLDEN_CHESTPLATE, "golden chestplate", "golden_chestplate", 720, "minecraft:golden_chestplate");
        register(Items.GOLDEN_LEGGINGS, "golden leggings", "golden_leggings", 630, "minecraft:golden_leggings");
        register(Items.GOLDEN_BOOTS, "golden boots", "golden_boots", 360, "minecraft:golden_boots");
        register(Items.CHAINMAIL_HELMET, "chainmail helmet", "chainmail_helmet", 300, "minecraft:chainmail_helmet");
        register(Items.CHAINMAIL_CHESTPLATE, "chainmail chestplate", "chainmail_chestplate", 480, "minecraft:chainmail_chestplate");
        register(Items.CHAINMAIL_LEGGINGS, "chainmail leggings", "chainmail_leggings", 420, "minecraft:chainmail_leggings");
        register(Items.CHAINMAIL_BOOTS, "chainmail boots", "chainmail_boots", 240, "minecraft:chainmail_boots");
        register(Items.TURTLE_HELMET, "turtle helmet", "turtle_helmet", 1200, "minecraft:turtle_helmet");
        register(Items.LEATHER_HORSE_ARMOR, "leather horse armor", "leather_horse_armor", 160, "minecraft:leather_horse_armor");
        register(Items.COPPER_HORSE_ARMOR, "copper horse armor", "copper_horse_armor", 500, "minecraft:copper_horse_armor");
        register(Items.IRON_HORSE_ARMOR, "iron horse armor", "iron_horse_armor", 900, "minecraft:iron_horse_armor");
        register(Items.GOLDEN_HORSE_ARMOR, "golden horse armor", "golden_horse_armor", 900, "minecraft:golden_horse_armor");
        register(Items.DIAMOND_HORSE_ARMOR, "diamond horse armor", "diamond_horse_armor", 4500, "minecraft:diamond_horse_armor");
        register(Items.NETHERITE_HORSE_ARMOR, "netherite horse armor", "netherite_horse_armor", 9000, "minecraft:netherite_horse_armor");
        register(Items.DIAMOND_HELMET, "diamond helmet", "diamond_helmet", 3750, "minecraft:diamond_helmet");
        register(Items.DIAMOND_CHESTPLATE, "diamond chestplate", "diamond_chestplate", 6000, "minecraft:diamond_chestplate");
        register(Items.DIAMOND_LEGGINGS, "diamond leggings", "diamond_leggings", 5250, "minecraft:diamond_leggings");
        register(Items.DIAMOND_BOOTS, "diamond boots", "diamond_boots", 3000, "minecraft:diamond_boots");
        register(Items.NETHERITE_HELMET, "netherite helmet", "netherite_helmet", 9000, "minecraft:netherite_helmet");
        register(Items.NETHERITE_CHESTPLATE, "netherite chestplate", "netherite_chestplate", 12000, "minecraft:netherite_chestplate");
        register(Items.NETHERITE_LEGGINGS, "netherite leggings", "netherite_leggings", 11250, "minecraft:netherite_leggings");
        register(Items.NETHERITE_BOOTS, "netherite boots", "netherite_boots", 8250, "minecraft:netherite_boots");
        register(Items.FISHING_ROD, "fishing rod", "fishing_rod", 30, "minecraft:fishing_rod");
        register(Items.SHEARS, "shears", "shears", 180, "minecraft:shears");
        register(Items.FLINT_AND_STEEL, "flint and steel", "flint_and_steel", 100, "minecraft:flint_and_steel");
        register(Items.BRUSH, "brush", "brush", 70, "minecraft:brush");
        register(Items.PAINTING, "painting", "painting", 80, "minecraft:painting");
        register(Items.ITEM_FRAME, "item frame", "item_frame", 40, "minecraft:item_frame");
        register(Items.GLOW_ITEM_FRAME, "glow item frame", "glow_item_frame", 100, "minecraft:glow_item_frame");
        register(Items.FLOWER_POT, "flower pot", "flower_pot", 20, "minecraft:flower_pot");
        register(Items.BOOKSHELF, "bookshelf", "bookshelf", 90, "minecraft:bookshelf");
        register(Items.LANTERN, "lantern", "lantern", 60, "minecraft:lantern");
        register(Items.SOUL_LANTERN, "soul lantern", "soul_lantern", 80, "minecraft:soul_lantern");
        register(Items.CANDLE, "candle", "candle", 12, "minecraft:candle");
        register(Items.WHITE_WOOL, "white wool", "white_wool", 20, "wool", "minecraft:white_wool");
        register(Items.WHITE_CARPET, "white carpet", "white_carpet", 8, "carpet", "minecraft:white_carpet");
        register(Items.WHITE_BED, "white bed", "white_bed", 80, "bed", "minecraft:white_bed");
        register(Items.WHITE_BANNER, "white banner", "white_banner", 70, "banner", "minecraft:white_banner");
        register(Items.MUSIC_DISC_13, "music disc 13", "music_disc_13", 600, "minecraft:music_disc_13");
        register(Items.MUSIC_DISC_CAT, "music disc cat", "music_disc_cat", 600, "minecraft:music_disc_cat");
        register(Items.MUSIC_DISC_BLOCKS, "music disc blocks", "music_disc_blocks", 700, "minecraft:music_disc_blocks");
        register(Items.MUSIC_DISC_CHIRP, "music disc chirp", "music_disc_chirp", 700, "minecraft:music_disc_chirp");
        register(Items.MUSIC_DISC_FAR, "music disc far", "music_disc_far", 700, "minecraft:music_disc_far");
        register(Items.MUSIC_DISC_CREATOR, "music disc creator", "music_disc_creator", 800, "minecraft:music_disc_creator");
        register(Items.MUSIC_DISC_CREATOR_MUSIC_BOX, "music disc creator music box", "music_disc_creator_music_box", 800, "minecraft:music_disc_creator_music_box");
        register(Items.MUSIC_DISC_LAVA_CHICKEN, "music disc lava chicken", "music_disc_lava_chicken", 900, "minecraft:music_disc_lava_chicken");
        register(Items.MUSIC_DISC_MALL, "music disc mall", "music_disc_mall", 700, "minecraft:music_disc_mall");
        register(Items.MUSIC_DISC_MELLOHI, "music disc mellohi", "music_disc_mellohi", 700, "minecraft:music_disc_mellohi");
        register(Items.MUSIC_DISC_STAL, "music disc stal", "music_disc_stal", 700, "minecraft:music_disc_stal");
        register(Items.MUSIC_DISC_STRAD, "music disc strad", "music_disc_strad", 700, "minecraft:music_disc_strad");
        register(Items.MUSIC_DISC_WARD, "music disc ward", "music_disc_ward", 700, "minecraft:music_disc_ward");
        register(Items.MUSIC_DISC_11, "music disc 11", "music_disc_11", 700, "minecraft:music_disc_11");
        register(Items.MUSIC_DISC_WAIT, "music disc wait", "music_disc_wait", 700, "minecraft:music_disc_wait");
        register(Items.MUSIC_DISC_OTHERSIDE, "music disc otherside", "music_disc_otherside", 1200, "minecraft:music_disc_otherside");
        register(Items.MUSIC_DISC_RELIC, "music disc relic", "music_disc_relic", 1200, "minecraft:music_disc_relic");
        register(Items.MUSIC_DISC_5, "music disc 5", "music_disc_5", 1200, "minecraft:music_disc_5");
        register(Items.MUSIC_DISC_PIGSTEP, "music disc pigstep", "music_disc_pigstep", 1400, "minecraft:music_disc_pigstep");
        register(Items.MUSIC_DISC_PRECIPICE, "music disc precipice", "music_disc_precipice", 1200, "minecraft:music_disc_precipice");
        register(Items.MUSIC_DISC_TEARS, "music disc tears", "music_disc_tears", 1200, "minecraft:music_disc_tears");
    }

    private static void registerProfessions() {
        PROFESSIONS.put("lumberjack", new Profession("lumberjack", "Lumberjack", "oak_log", Set.of("log", "planks", "stick")));
        PROFESSIONS.put("miner", new Profession("miner", "Miner", "iron_ingot", Set.of("stone", "ore", "ingot", "diamond", "emerald", "coal", "redstone", "lapis", "quartz", "netherite", "debris")));
        PROFESSIONS.put("farmer", new Profession("farmer", "Farmer", "wheat", Set.of("wheat", "seeds", "carrot", "potato", "apple", "pumpkin")));
        PROFESSIONS.put("fisherman", new Profession("fisherman", "Fisherman", "cod", Set.of("cod", "salmon", "fishing")));
        PROFESSIONS.put("hunter", new Profession("hunter", "Hunter", "beef", Set.of("beef", "porkchop", "chicken", "mutton", "leather", "feather", "bone", "string", "gunpowder", "flesh", "pearl", "blaze", "slime")));
        PROFESSIONS.put("builder", new Profession("builder", "Builder", "stone", Set.of("stone", "dirt", "sand", "gravel", "glass", "brick", "clay", "torch", "chest", "furnace")));
    }

    private static void registerBuyableEffects() {
        BUYABLE_EFFECTS.put("speed", MobEffects.SPEED);
        BUYABLE_EFFECTS.put("haste", MobEffects.HASTE);
        BUYABLE_EFFECTS.put("strength", MobEffects.STRENGTH);
        BUYABLE_EFFECTS.put("jump_boost", MobEffects.JUMP_BOOST);
        BUYABLE_EFFECTS.put("regeneration", MobEffects.REGENERATION);
        BUYABLE_EFFECTS.put("resistance", MobEffects.RESISTANCE);
        BUYABLE_EFFECTS.put("fire_resistance", MobEffects.FIRE_RESISTANCE);
        BUYABLE_EFFECTS.put("water_breathing", MobEffects.WATER_BREATHING);
        BUYABLE_EFFECTS.put("night_vision", MobEffects.NIGHT_VISION);
        BUYABLE_EFFECTS.put("invisibility", MobEffects.INVISIBILITY);
        BUYABLE_EFFECTS.put("luck", MobEffects.LUCK);
        BUYABLE_EFFECTS.put("slow_falling", MobEffects.SLOW_FALLING);
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
            return sellPrice * BUY_PRICE_MULTIPLIER;
        }
    }

    private record DuelRequest(UUID challenger, int amount, String challengerName) {
    }

    private record DailyQuest(ShopItem item, int count, int reward) {
    }

    private record ProfessionQuest(String key, ShopItem item, int count, int reward) {
    }

    private record TermDeposit(int amount, long maturityDay, int days) {
    }

    public record CommandHelp(String syntax, String example, String description) {
    }

    private record Profession(String id, String displayName, String questItem, Set<String> keywords) {
        private boolean matches(String itemName) {
            for (String keyword : keywords) {
                if (itemName.contains(keyword)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class PlayerDataFile {
        private Map<String, PlayerData> players = new HashMap<>();
    }

    private static class PlayerData {
        private int money;
        private int investment;
        private int loan;
        private TermDepositData termDeposit;
        private String profession;
        private int professionSales;
        private long dailyQuestDay = -1L;
        private boolean dailyQuestWidgetHidden;
        private List<String> completedProfessionQuests = new ArrayList<>();
        private boolean playerInsurance;
        private List<String> insuredPets = new ArrayList<>();
        private List<String> unlockedItems = new ArrayList<>();
        private Map<String, String> unlockSources = new HashMap<>();
        private List<String> gambleHistory = new ArrayList<>();
        private boolean welcomeHintShown;
    }

    private static class TermDepositData {
        private int amount;
        private long maturityDay;
        private int days;
    }

    private static class FeatureSettings {
        private boolean shopSell = true;
        private boolean wallet = true;
        private boolean investments = true;
        private boolean gambling = true;
        private boolean quests = true;
        private boolean professions = true;
        private boolean teleports = true;
        private boolean xpTrading = true;
        private boolean itemServices = true;
        private boolean pets = true;
        private boolean insurance = true;
        private boolean loans = true;

        private FeatureSettings withDefaults() {
            return this;
        }

        private List<String> featureNames() {
            return List.of("shop_sell", "wallet", "investments", "gambling", "quests", "professions", "teleports", "xp_trading", "item_services", "pets", "insurance", "loans");
        }

        private boolean isEnabled(String feature) {
            return switch (feature) {
                case "shop_sell" -> shopSell;
                case "wallet" -> wallet;
                case "investments" -> investments;
                case "gambling" -> gambling;
                case "quests" -> quests;
                case "professions" -> professions;
                case "teleports" -> teleports;
                case "xp_trading" -> xpTrading;
                case "item_services" -> itemServices;
                case "pets" -> pets;
                case "insurance" -> insurance;
                case "loans" -> loans;
                default -> true;
            };
        }

        private boolean toggle(String feature) {
            switch (feature) {
                case "shop_sell" -> shopSell = !shopSell;
                case "wallet" -> wallet = !wallet;
                case "investments" -> investments = !investments;
                case "gambling" -> gambling = !gambling;
                case "quests" -> quests = !quests;
                case "professions" -> professions = !professions;
                case "teleports" -> teleports = !teleports;
                case "xp_trading" -> xpTrading = !xpTrading;
                case "item_services" -> itemServices = !itemServices;
                case "pets" -> pets = !pets;
                case "insurance" -> insurance = !insurance;
                case "loans" -> loans = !loans;
                default -> {
                    return false;
                }
            }
            return true;
        }

        private String summary() {
            List<String> entries = new ArrayList<>();
            for (String feature : featureNames()) {
                entries.add(feature + "=" + (isEnabled(feature) ? "on" : "off"));
            }
            return String.join(", ", entries);
        }

        private EconomyStatePayload.FeatureSettingsState toPayload() {
            return new EconomyStatePayload.FeatureSettingsState(shopSell, wallet, investments, gambling, quests, professions, teleports, xpTrading, itemServices, pets, insurance, loans);
        }
    }
}
