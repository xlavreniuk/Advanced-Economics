package com.example.advancedeconomics;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class EconomyScreen extends Screen {
    private static final int PANEL_WIDTH = 520;
    private static final int PANEL_HEIGHT = 304;
    private static final int TAB_HEIGHT = 18;
    private static final int TITLE_SPACE = 24;
    private static final int TABS_PER_PAGE = 4;
    private static final int BUTTON_HEIGHT = 18;
    private static final int ROW_HEIGHT = 24;
    private static final int GAP = 6;
    private static final int PANEL_FILL = 0xE0C6C6C6;
    private static final int PANEL_DARK_EDGE = 0xFF373737;
    private static final int PANEL_LIGHT_EDGE = 0xFFFFFFFF;
    private static final int PANEL_SHADOW = 0xFF000000;
    private static final int LABEL_COLOR = 0xFF404040;
    private static final int MUTED_COLOR = 0xFF343434;
    private static final int VALUE_COLOR = 0xFF2F2F2F;
    private static final int MONEY_COLOR = 0xFF2E8B2E;
    private static final int ACCENT_COLOR = 0xFF707070;
    private static final int LOCKED_COLOR = 0xFF8F3A3A;
    private static final int UNLOCKED_COLOR = 0xFF2E6F2E;
    private static final int BUTTON_DEFAULT_FILL = 0xFF8F8F8F;
    private static final int BUTTON_DEFAULT_TEXT = 0xFFFFFFFF;
    private static final int BUTTON_PRESSED_FILL = 0xFF3F3F3F;
    private static final int BUTTON_PRESSED_TEXT = 0xFFFFFFA0;

    private Tab tab = Tab.SHOP;
    private EditBox amountField;
    private EditBox playerField;
    private EditBox searchField;
    private String searchValue = "";
    private int scrollOffset;
    private int listLeft;
    private int listTop;
    private int listWidth;
    private int listHeight;
    private int tabPage;
    private PriceSort priceSort = PriceSort.NAME;

    public EconomyScreen() {
        super(Component.translatable("screen.advanced-economics.title"));
    }

    @Override
    protected void init() {
        rebuildTab();
    }

    private void rebuildTab() {
        this.clearWidgets();
        int left = panelLeft();
        int top = panelTop();
        int panelWidth = panelWidth();
        Tab[] visibleTabs = visibleTabs();
        int tabWidth = panelWidth / Math.max(1, visibleTabs.length);
        int navY = top - TAB_HEIGHT * 2 - 2;
        int pageButtonWidth = (panelWidth - 12) / 3;
        for (int page = 0; page < 3; page++) {
            int pageIndex = page;
            StyledButton pageButton = styledButton(left + page * (pageButtonWidth + 6), navY, pageButtonWidth, TAB_HEIGHT, pageLabel(page), ignored -> selectTabPage(pageIndex));
            pageButton.active = page != tabPage;
            this.addRenderableWidget(pageButton);
        }

        for (int i = 0; i < visibleTabs.length; i++) {
            Tab value = visibleTabs[i];
            int x = left + i * tabWidth;
            StyledButton button = styledButton(x, top - TAB_HEIGHT, tabWidth - 2, TAB_HEIGHT, Component.translatable(value.translationKey), ignored -> selectTab(value), tabIcon(value));
            button.active = value != tab;
            this.addRenderableWidget(button);
        }

        listLeft = left + 12;
        listTop = top + 88;
        listWidth = panelWidth - 24;
        listHeight = panelHeight() - 128;

        int fieldTop = top + 36;
        if (tab == Tab.WALLET) {
            int actionX = left + panelWidth - 236;
            playerField = field(left + 12, fieldTop, Math.max(116, panelWidth - 390), "screen.advanced-economics.field.player", "");
            amountField = field(actionX - 82, fieldTop, 74, "screen.advanced-economics.field.amount", "1");
            addCommandButton(actionX, fieldTop, 108, "screen.advanced-economics.button.send_money", () -> "money send " + cleanPlayer() + " " + cleanAmount());
            addCommandButton(actionX + 116, fieldTop, 104, "screen.advanced-economics.button.paid_tp", () -> "paidtp " + cleanPlayer());
            addCommandButton(actionX, fieldTop + 22, 108, "screen.advanced-economics.button.duel", () -> "duel " + cleanPlayer() + " " + cleanAmount());
            addCommandButton(actionX + 116, fieldTop + 22, 104, "screen.advanced-economics.button.duel_accept", "duel accept");
        } else if (tab == Tab.INVEST) {
            amountField = field(left + 12, fieldTop, 76, "screen.advanced-economics.field.amount", "1");
            int actionX = left + 96;
            int actionWidth = Math.max(74, (panelWidth - 114) / 3);
            addCommandButton(actionX, fieldTop, actionWidth - 4, "screen.advanced-economics.button.invest", () -> "invest " + cleanAmount());
            addCommandButton(actionX + actionWidth, fieldTop, actionWidth - 4, "screen.advanced-economics.button.invest_all", "invest all");
            addCommandButton(actionX + actionWidth * 2, fieldTop, actionWidth - 4, "screen.advanced-economics.button.withdraw", "invest withdraw");
            addCommandButton(actionX, fieldTop + 22, actionWidth - 4, "screen.advanced-economics.button.deposit_7", () -> "termdeposit 7 " + cleanAmount());
            addCommandButton(actionX + actionWidth, fieldTop + 22, actionWidth - 4, "screen.advanced-economics.button.loan", () -> "loan take " + cleanAmount());
            addCommandButton(actionX + actionWidth * 2, fieldTop + 22, actionWidth - 4, "screen.advanced-economics.button.loan_repay", () -> "loan repay " + cleanAmount());
        } else if (tab == Tab.GAMBLE) {
            amountField = field(left + 12, fieldTop, 76, "screen.advanced-economics.field.amount", "1");
            int actionX = left + 96;
            int actionWidth = Math.max(60, (panelWidth - 112) / 4);
            addCommandButton(actionX, fieldTop, actionWidth - 4, "screen.advanced-economics.button.gamble_money", () -> "gamble money " + cleanAmount());
            addCommandButton(actionX + actionWidth, fieldTop, actionWidth - 4, "screen.advanced-economics.button.gamble_roll", "gamble");
            addCommandButton(actionX + actionWidth * 2, fieldTop, actionWidth - 4, "screen.advanced-economics.button.speed", "effectbuy speed");
            addCommandButton(actionX + actionWidth * 3, fieldTop, actionWidth - 4, "screen.advanced-economics.button.haste", "effectbuy haste");
            addCommandButton(actionX, fieldTop + 22, actionWidth - 4, "screen.advanced-economics.button.repair", "itemservice repair");
            addCommandButton(actionX + actionWidth, fieldTop + 22, actionWidth - 4, "screen.advanced-economics.button.insurance", "insurance buy");
        } else if (tab == Tab.QUESTS) {
            int jobWidth = (panelWidth - 32) / 5;
            addCommandButton(left + 12, fieldTop, jobWidth - 4, "screen.advanced-economics.button.prof_miner", "profession choose miner");
            addCommandButton(left + 12 + jobWidth, fieldTop, jobWidth - 4, "screen.advanced-economics.button.prof_lumber", "profession choose lumberjack");
            addCommandButton(left + 12 + jobWidth * 2, fieldTop, jobWidth - 4, "screen.advanced-economics.button.prof_farmer", "profession choose farmer");
            addCommandButton(left + 12 + jobWidth * 3, fieldTop, jobWidth - 4, "screen.advanced-economics.button.prof_fisher", "profession choose fisherman");
            addCommandButton(left + 12 + jobWidth * 4, fieldTop, jobWidth - 4, "screen.advanced-economics.button.prof_hunter", "profession choose hunter");
            int questWidth = (panelWidth - 32) / 4;
            addCommandButton(left + 12, fieldTop + 22, questWidth - 4, "screen.advanced-economics.button.daily", "dailyquest");
            addCommandButton(left + 12 + questWidth, fieldTop + 22, questWidth - 4, "screen.advanced-economics.button.prof_quest", "profession quest");
            addCommandButton(left + 12 + questWidth * 2, fieldTop + 22, questWidth - 4, "screen.advanced-economics.button.prof_claim", "profession quest claim");
            addCommandButton(left + 12 + questWidth * 3, fieldTop + 22, questWidth - 4, "screen.advanced-economics.button.daily_show", "dailyquest show");
        } else if (tab == Tab.SETTINGS) {
            addCommandButton(left + 12, fieldTop, 116, "screen.advanced-economics.button.settings_reload", "aesettings");
        } else if (tab == Tab.ABOUT || tab == Tab.CREDITS) {
            // Static information tabs do not need command buttons.
        } else {
            amountField = field(left + 12, fieldTop, 76, "screen.advanced-economics.field.amount", "1");
            int actionX = left + panelWidth - 190;
            int sortX = actionX - 64;
            searchField = field(left + 96, fieldTop, Math.max(52, sortX - left - 104), "screen.advanced-economics.field.search", searchValue);
            searchField.setResponder(value -> {
                searchValue = value;
                scrollOffset = 0;
            });
            this.addRenderableWidget(styledButton(sortX, fieldTop, 56, BUTTON_HEIGHT, Component.literal(priceSort.label()), ignored -> cyclePriceSort()));
            if (tab == Tab.SELL) {
                addCommandButton(actionX, fieldTop, 86, "screen.advanced-economics.button.sell_held", () -> "sell " + cleanAmount());
                addCommandButton(actionX + 94, fieldTop, 94, "screen.advanced-economics.button.sell_inventory", "sell inventory");
            } else if (tab == Tab.SHOP) {
                addCommandButton(actionX, fieldTop, 86, "screen.advanced-economics.button.daily", "dailyquest");
                addCommandButton(actionX + 94, fieldTop, 94, "screen.advanced-economics.button.daily_claim", "dailyquest claim");
            } else {
                addCommandButton(actionX, fieldTop, 86, "screen.advanced-economics.button.balance", "money");
                addCommandButton(actionX + 94, fieldTop, 94, "screen.advanced-economics.button.held_price", "price");
            }
        }
    }

    private Tab[] visibleTabs() {
        Tab[] values = Tab.values();
        int start = tabPage * TABS_PER_PAGE;
        int end = Math.min(values.length, start + TABS_PER_PAGE);
        Tab[] visible = new Tab[end - start];
        for (int i = start; i < end; i++) {
            visible[i - start] = values[i];
        }
        return visible;
    }

    private Component pageLabel(int page) {
        return switch (page) {
            case 0 -> Component.literal("Market");
            case 1 -> Component.literal("Earnings");
            default -> Component.literal("More");
        };
    }

    private void selectTabPage(int page) {
        tabPage = Math.max(0, Math.min(pageCount() - 1, page));
        boolean currentVisible = false;
        for (Tab value : visibleTabs()) {
            if (value == tab) {
                currentVisible = true;
                break;
            }
        }
        if (!currentVisible) {
            tab = visibleTabs()[0];
        }
        scrollOffset = 0;
        amountField = null;
        playerField = null;
        searchField = null;
        rebuildTab();
    }

    private void cyclePriceSort() {
        priceSort = priceSort.next();
        scrollOffset = 0;
        rebuildTab();
    }

    private int pageCount() {
        return Math.max(1, (Tab.values().length + TABS_PER_PAGE - 1) / TABS_PER_PAGE);
    }

    private void selectTab(Tab value) {
        if (tab == value) {
            return;
        }

        tab = value;
        scrollOffset = 0;
        amountField = null;
        playerField = null;
        searchField = null;
        rebuildTab();
    }

    private EditBox field(int x, int y, int width, String hintKey, String value) {
        EditBox field = new EditBox(this.font, x, y, width, 18, Component.translatable(hintKey));
        field.setMaxLength(64);
        field.setValue(value);
        field.setHint(Component.translatable(hintKey));
        this.addRenderableWidget(field);
        return field;
    }

    private void addCommandButton(int x, int y, int width, String labelKey, String command) {
        addCommandButton(x, y, width, labelKey, () -> command);
    }

    private void addCommandButton(int x, int y, int width, String labelKey, CommandFactory commandFactory) {
        this.addRenderableWidget(styledButton(x, y, width, BUTTON_HEIGHT, Component.translatable(labelKey), ignored -> runCommand(commandFactory.command())));
    }

    private StyledButton styledButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
        return styledButton(x, y, width, height, message, onPress, null);
    }

    private StyledButton styledButton(int x, int y, int width, int height, Component message, Button.OnPress onPress, Item icon) {
        return new StyledButton(x, y, width, height, message, onPress, icon);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractMenuBackground(graphics);

        int left = panelLeft();
        int top = panelTop();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        drawVanillaPanel(graphics, left - 12, top - TAB_HEIGHT * 2 - TITLE_SPACE, panelWidth + 24, panelHeight + TAB_HEIGHT * 2 + TITLE_SPACE + 2);
        drawCenteredTextNoShadow(graphics, this.title, this.width / 2, top - TAB_HEIGHT * 2 - 16, 0xFFFFFFFF);
        drawBalances(graphics, left, top);
        drawFieldLabels(graphics, left, top);
        drawSectionHeader(graphics, left, top);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        drawDailyQuestToast(graphics);

        if (tab == Tab.SHOP || tab == Tab.UNLOCKS || tab == Tab.SELL) {
            drawItemList(graphics, mouseX, mouseY);
        } else if (tab == Tab.WALLET) {
            drawWallet(graphics, left, top);
        } else if (tab == Tab.INVEST) {
            drawInvest(graphics, left, top);
        } else if (tab == Tab.GAMBLE) {
            drawGamble(graphics, left, top);
        } else if (tab == Tab.QUESTS) {
            drawQuests(graphics, left, top);
        } else if (tab == Tab.SETTINGS) {
            drawSettings(graphics, left, top, mouseX, mouseY);
        } else if (tab == Tab.ABOUT || tab == Tab.CREDITS) {
            drawInfo(graphics, left, top);
        }
    }

    private void drawBalances(GuiGraphicsExtractor graphics, int left, int top) {
        EconomyStatePayload state = AdvancedEconomicsClient.latestState();
        graphics.text(this.font, Component.translatable("screen.advanced-economics.label.money"), left + 12, top + 12, LABEL_COLOR, false);
        graphics.text(this.font, formatMoney(state.money()), left + 56, top + 12, MONEY_COLOR, false);
        graphics.text(this.font, Component.translatable("screen.advanced-economics.label.invested"), left + 156, top + 12, LABEL_COLOR, false);
        graphics.text(this.font, formatMoney(state.investment()), left + 214, top + 12, MONEY_COLOR, false);
    }

    private void drawFieldLabels(GuiGraphicsExtractor graphics, int left, int top) {
        int labelY = top + 22;
        if (tab == Tab.WALLET) {
            graphics.text(this.font, Component.translatable("screen.advanced-economics.label.player"), left + 12, labelY, LABEL_COLOR, false);
            graphics.text(this.font, Component.translatable("screen.advanced-economics.label.count"), left + 136, labelY, LABEL_COLOR, false);
        } else if (amountField != null) {
            graphics.text(this.font, Component.translatable("screen.advanced-economics.label.count"), left + 12, labelY, LABEL_COLOR, false);
            if (searchField != null) {
                graphics.text(this.font, Component.translatable("screen.advanced-economics.label.search"), left + 96, labelY, LABEL_COLOR, false);
            }
        }
    }

    private void drawSectionHeader(GuiGraphicsExtractor graphics, int left, int top) {
        int panelWidth = panelWidth();
        int headerY = sectionHeaderY(top);
        graphics.fill(left + 12, headerY, left + panelWidth - 12, headerY + 3, tabAccentColor(tab));
        graphics.horizontalLine(left + 12, left + panelWidth - 13, headerY + 3, 0xFF8B8B8B);
        graphics.horizontalLine(left + 12, left + panelWidth - 13, headerY + 3, 0xFFFFFFFF);
        drawFittedText(graphics, Component.translatable("screen.advanced-economics.description." + tab.translationName), left + 14, headerY + 10, panelWidth - 28, MUTED_COLOR);

        String key = switch (tab) {
            case SHOP -> "screen.advanced-economics.footer.shop";
            case SELL -> "screen.advanced-economics.footer.sell";
            case WALLET -> "screen.advanced-economics.footer.wallet";
            case INVEST -> "screen.advanced-economics.footer.invest";
            case UNLOCKS -> "screen.advanced-economics.footer.unlocks";
            case QUESTS -> "screen.advanced-economics.footer.quests";
            case SETTINGS -> "screen.advanced-economics.footer.settings";
            case GAMBLE -> "screen.advanced-economics.footer.gamble";
            case ABOUT -> "screen.advanced-economics.footer.about";
            case CREDITS -> "screen.advanced-economics.footer.credits";
        };
        drawCenteredFittedText(graphics, Component.translatable(key), this.width / 2, top + panelHeight() - 22, panelWidth() - 34, MUTED_COLOR);
    }

    private int sectionHeaderY(int top) {
        return switch (tab) {
            case WALLET, INVEST, GAMBLE, QUESTS -> top + 82;
            default -> top + 60;
        };
    }

    private int contentTop(int top) {
        return sectionHeaderY(top) + 26;
    }

    private void drawItemList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<EconomyStatePayload.Entry> entries = filteredEntries();
        clampScroll(entries.size());
        drawListFrame(graphics);

        int visibleRows = visibleRows();
        int end = Math.min(entries.size(), scrollOffset + visibleRows);
        graphics.enableScissor(listLeft + 1, listTop + 1, listLeft + listWidth - 1, listTop + listHeight - 1);
        for (int index = scrollOffset; index < end; index++) {
            int rowTop = listTop + (index - scrollOffset) * ROW_HEIGHT;
            drawItemRow(graphics, entries.get(index), rowTop, mouseX, mouseY);
        }
        graphics.disableScissor();

        if (entries.isEmpty()) {
            drawCenteredTextNoShadow(graphics, Component.translatable("screen.advanced-economics.empty.no_items"), this.width / 2, listTop + 54, MUTED_COLOR);
        }

        if (entries.size() > visibleRows) {
            int scrollX = listLeft + listWidth - 6;
            int thumbHeight = Math.max(18, listHeight * visibleRows / entries.size());
            int maxThumbTravel = listHeight - thumbHeight - 4;
            int thumbY = listTop + 2 + (entries.size() == visibleRows ? 0 : maxThumbTravel * scrollOffset / Math.max(1, entries.size() - visibleRows));
            graphics.fill(scrollX, listTop + 2, scrollX + 3, listTop + listHeight - 2, 0xFF6E6E6E);
            graphics.fill(scrollX, thumbY, scrollX + 3, thumbY + thumbHeight, 0xFFCFCFCF);
        }
    }

    private void drawItemRow(GuiGraphicsExtractor graphics, EconomyStatePayload.Entry entry, int rowTop, int mouseX, int mouseY) {
        boolean hovered = mouseX >= listLeft && mouseX < listLeft + listWidth && mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT;
        int background = hovered ? 0xFFE4E4E4 : (entry.unlocked() ? 0xFFD0D0D0 : 0xFFB9B9B9);
        graphics.fill(listLeft + 1, rowTop, listLeft + listWidth - 7, rowTop + ROW_HEIGHT - 1, background);
        graphics.fill(listLeft + 1, rowTop, listLeft + 4, rowTop + ROW_HEIGHT - 1, rowAccentColor(entry));
        graphics.horizontalLine(listLeft + 1, listLeft + listWidth - 7, rowTop + ROW_HEIGHT - 1, 0xFF8B8B8B);

        ItemStack stack = new ItemStack(itemFor(entry));
        graphics.item(stack, listLeft + 8, rowTop + 5);

        int textX = listLeft + 31;
        graphics.text(this.font, itemName(entry, stack), textX, rowTop + 4, entry.unlocked() ? LABEL_COLOR : 0xFF5E5E5E, false);
        graphics.text(this.font, Component.translatable("screen.advanced-economics.row.sell", formatMoney(entry.sellPrice())), textX, rowTop + 15, MONEY_COLOR, false);
        graphics.text(this.font, Component.translatable("screen.advanced-economics.row.buy", formatMoney(entry.buyPrice())), textX + 84, rowTop + 15, MONEY_COLOR, false);

        Component state = entry.unlocked() ? stateLabel(entry) : Component.translatable("screen.advanced-economics.state.locked");
        graphics.text(this.font, state, listLeft + 198, rowTop + 8, entry.unlocked() ? UNLOCKED_COLOR : LOCKED_COLOR, false);

        if (tab == Tab.SELL) {
            graphics.text(this.font, "x" + entry.inventoryCount(), listLeft + 298, rowTop + 8, LABEL_COLOR, false);
            int actionX = listLeft + listWidth - 66;
            drawSmallButton(graphics, actionX, rowTop + 3, 52, 17, Component.translatable("screen.advanced-economics.action.sell"), entry.inventoryCount() > 0);
            return;
        }

        boolean canBuy = tab == Tab.SHOP && entry.unlocked();
        Component action = Component.translatable(canBuy ? "screen.advanced-economics.action.buy" : entry.unlocked() ? "screen.advanced-economics.action.done" : "screen.advanced-economics.action.unlock");
        int actionX = listLeft + listWidth - 66;
        drawSmallButton(graphics, actionX, rowTop + 3, 52, 17, action, !entry.unlocked() || canBuy);
    }

    private void drawWallet(GuiGraphicsExtractor graphics, int left, int top) {
        EconomyStatePayload state = AdvancedEconomicsClient.latestState();
        int contentTop = contentTop(top);
        int cardX = left + 64;
        int cardY = contentTop + 12;
        int cardWidth = panelWidth() - 128;
        drawVanillaPanel(graphics, cardX, cardY, cardWidth, 58);
        graphics.text(this.font, Component.translatable("screen.advanced-economics.label.balance"), cardX + 18, cardY + 14, LABEL_COLOR, false);
        graphics.text(this.font, Component.literal(formatMoney(state.money())), cardX + cardWidth - 88, cardY + 14, MONEY_COLOR, false);
        graphics.text(this.font, Component.translatable("screen.advanced-economics.wallet.loan", formatMoney(state.loan())), cardX + 18, cardY + 31, MONEY_COLOR, false);
        drawCenteredFittedText(graphics, Component.translatable("screen.advanced-economics.wallet.help"), this.width / 2, cardY + 70, panelWidth() - 52, MUTED_COLOR);
    }

    private void drawInvest(GuiGraphicsExtractor graphics, int left, int top) {
        int contentTop = contentTop(top);
        drawVanillaPanel(graphics, left + 42, contentTop + 8, panelWidth() - 84, 76);
        drawCenteredTextNoShadow(graphics, Component.translatable("screen.advanced-economics.label.investment_balance"), this.width / 2, contentTop + 21, LABEL_COLOR);
        drawCenteredTextNoShadow(graphics, Component.literal(formatMoney(AdvancedEconomicsClient.latestState().investment())), this.width / 2, contentTop + 40, MONEY_COLOR);
        drawCenteredTextNoShadow(graphics, Component.translatable("screen.advanced-economics.invest.help"), this.width / 2, contentTop + 64, MUTED_COLOR);
        drawCenteredFittedText(graphics, Component.translatable("screen.advanced-economics.invest.term_help"), this.width / 2, contentTop + 77, panelWidth() - 64, MUTED_COLOR);
    }

    private void drawGamble(GuiGraphicsExtractor graphics, int left, int top) {
        int contentTop = contentTop(top);
        drawVanillaPanel(graphics, left + 30, contentTop + 8, panelWidth() - 60, 96);
        drawCenteredTextNoShadow(graphics, Component.translatable("screen.advanced-economics.gamble.title"), this.width / 2, contentTop + 20, LABEL_COLOR);
        drawCenteredFittedText(graphics, Component.translatable("screen.advanced-economics.gamble.help"), this.width / 2, contentTop + 35, panelWidth() - 76, MUTED_COLOR);
        List<String> history = AdvancedEconomicsClient.latestState().gambleHistory();
        if (history.isEmpty()) {
            drawCenteredTextNoShadow(graphics, Component.translatable("screen.advanced-economics.gamble.empty"), this.width / 2, contentTop + 64, MUTED_COLOR);
            return;
        }

        int y = contentTop + 53;
        for (int i = 0; i < Math.min(5, history.size()); i++) {
            graphics.text(this.font, history.get(i), left + 70, y + i * 12, LABEL_COLOR, false);
        }
    }

    private void drawQuests(GuiGraphicsExtractor graphics, int left, int top) {
        EconomyStatePayload state = AdvancedEconomicsClient.latestState();
        EconomyStatePayload.DailyQuestState daily = state.dailyQuest();
        int contentTop = contentTop(top);
        int dailyY = contentTop + 8;
        drawVanillaPanel(graphics, left + 18, dailyY, panelWidth() - 36, 50);
        graphics.item(new ItemStack(itemFromId(daily.itemId())), left + 30, dailyY + 14);
        graphics.text(this.font, Component.translatable("screen.advanced-economics.quest.daily"), left + 54, dailyY + 8, LABEL_COLOR, false);
        graphics.text(this.font, Component.literal(daily.displayName() + " " + daily.progress() + "/" + daily.required()), left + 54, dailyY + 21, daily.claimed() ? UNLOCKED_COLOR : VALUE_COLOR, false);
        drawProgressBar(graphics, left + 54, dailyY + 35, panelWidth() - 140, 6, daily.progress(), Math.max(1, daily.required()), daily.claimed() ? UNLOCKED_COLOR : ACCENT_COLOR);
        drawSmallButton(graphics, left + panelWidth() - 96, dailyY + 24, 64, 15, Component.literal("+" + formatMoney(daily.reward())), !daily.claimed() && daily.progress() >= daily.required());

        int professionY = dailyY + 58;
        drawVanillaPanel(graphics, left + 18, professionY, panelWidth() - 36, 46);
        graphics.item(new ItemStack(Items.WRITABLE_BOOK), left + 30, professionY + 14);
        graphics.text(this.font, Component.translatable("screen.advanced-economics.quest.profession"), left + 54, professionY + 9, LABEL_COLOR, false);
        String profession = state.profession().isBlank() ? "none" : state.profession();
        graphics.text(this.font, Component.translatable("screen.advanced-economics.quest.profession_status", profession, state.professionBonus()), left + 54, professionY + 22, MUTED_COLOR, false);
        drawWrappedText(graphics, Component.translatable("screen.advanced-economics.quest.profession_help", profession, state.professionBonus()), left + 54, professionY + 35, panelWidth() - 96, MUTED_COLOR, 3);
    }

    private void drawSettings(GuiGraphicsExtractor graphics, int left, int top, int mouseX, int mouseY) {
        EconomyStatePayload.FeatureSettingsState settings = AdvancedEconomicsClient.latestState().featureSettings();
        String[] names = settingNames();
        Item[] icons = settingIcons();
        boolean[] values = settingValues(settings);
        int startY = contentTop(top);
        for (int i = 0; i < names.length; i++) {
            int row = i % 6;
            int col = i / 6;
            int x = left + 14 + col * 196;
            int y = startY + row * 24;
            boolean hovered = mouseX >= x && mouseX <= x + 184 && mouseY >= y && mouseY <= y + 20;
            graphics.fill(x, y, x + 184, y + 20, hovered ? 0xFFE5E5E5 : 0xFFD2D2D2);
            graphics.fill(x, y, x + 4, y + 20, values[i] ? UNLOCKED_COLOR : LOCKED_COLOR);
            graphics.item(new ItemStack(icons[i]), x + 8, y + 2);
            drawFittedText(graphics, Component.translatable("screen.advanced-economics.setting." + names[i]), x + 30, y + 6, 108, LABEL_COLOR);
            drawPocketSwitch(graphics, x + 144, y + 4, values[i]);
        }
    }

    private void drawPocketSwitch(GuiGraphicsExtractor graphics, int x, int y, boolean enabled) {
        int track = enabled ? 0xFF4C8F4C : 0xFF777777;
        int knobX = enabled ? x + 19 : x + 2;
        graphics.fill(x, y + 2, x + 36, y + 14, 0xFF373737);
        graphics.fill(x + 1, y + 3, x + 35, y + 13, track);
        graphics.fill(knobX, y, knobX + 15, y + 16, 0xFFFAFAFA);
        graphics.horizontalLine(knobX, knobX + 14, y, PANEL_LIGHT_EDGE);
        graphics.verticalLine(knobX, y, y + 15, PANEL_LIGHT_EDGE);
        graphics.horizontalLine(knobX, knobX + 14, y + 15, PANEL_DARK_EDGE);
        graphics.verticalLine(knobX + 14, y, y + 15, PANEL_DARK_EDGE);
    }

    private void drawDailyQuestToast(GuiGraphicsExtractor graphics) {
        EconomyStatePayload.DailyQuestState quest = AdvancedEconomicsClient.latestState().dailyQuest();
        if (quest.hidden() || quest.required() <= 0) {
            return;
        }

        int x = Math.max(8, this.width - 150);
        int y = 10;
        int width = 142;
        drawVanillaPanel(graphics, x, y, width, 52);
        graphics.item(new ItemStack(itemFromId(quest.itemId())), x + 8, y + 18);
        graphics.text(this.font, Component.translatable("screen.advanced-economics.quest.daily"), x + 28, y + 8, LABEL_COLOR, false);
        graphics.text(this.font, Component.literal(quest.progress() + "/" + quest.required() + " " + quest.displayName()), x + 28, y + 21, MUTED_COLOR, false);
        drawProgressBar(graphics, x + 28, y + 36, 62, 5, quest.progress(), Math.max(1, quest.required()), quest.claimed() ? UNLOCKED_COLOR : ACCENT_COLOR);
        drawSmallButton(graphics, x + 94, y + 31, 42, 15, Component.literal("+" + formatMoney(quest.reward())), !quest.claimed() && quest.progress() >= quest.required());
        graphics.text(this.font, Component.literal("x"), x + width - 12, y + 6, LOCKED_COLOR, false);
    }

    private void drawProgressBar(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int progress, int required, int color) {
        graphics.fill(x, y, x + width, y + height, 0xFF777777);
        int filled = Math.max(0, Math.min(width, width * progress / required));
        if (filled > 1) {
            graphics.fill(x + 1, y + 1, x + filled - 1, y + height - 1, color);
        }
    }

    private void drawListFrame(GuiGraphicsExtractor graphics) {
        graphics.fill(listLeft, listTop, listLeft + listWidth, listTop + listHeight, 0xFF8B8B8B);
        graphics.fill(listLeft + 1, listTop + 1, listLeft + listWidth - 1, listTop + listHeight - 1, 0xFF2B2B2B);
    }

    private void drawSmallButton(GuiGraphicsExtractor graphics, int x, int y, int width, int height, Component text, boolean active) {
        StyledButton button = styledButton(x, y, width, height, text, ignored -> {
        });
        button.active = active;
        button.extractRenderState(graphics, -1, -1, 0.0F);
    }

    private void drawButtonFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean pressed) {
        int topEdge = pressed ? PANEL_DARK_EDGE : PANEL_LIGHT_EDGE;
        int bottomEdge = pressed ? PANEL_LIGHT_EDGE : PANEL_DARK_EDGE;
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, buttonFill(pressed));
        graphics.horizontalLine(x, x + width - 1, y, topEdge);
        graphics.verticalLine(x, y, y + height - 1, topEdge);
        graphics.horizontalLine(x, x + width - 1, y + height - 1, bottomEdge);
        graphics.verticalLine(x + width - 1, y, y + height - 1, bottomEdge);
    }

    private int buttonFill(boolean pressed) {
        return pressed ? BUTTON_PRESSED_FILL : BUTTON_DEFAULT_FILL;
    }

    private int buttonTextColor(boolean pressed) {
        return pressed ? BUTTON_PRESSED_TEXT : BUTTON_DEFAULT_TEXT;
    }

    private void drawInfo(GuiGraphicsExtractor graphics, int left, int top) {
        int boxX = left + 18;
        int boxY = top + 88;
        int boxWidth = panelWidth() - 36;
        int boxHeight = panelHeight() - 126;
        drawVanillaPanel(graphics, boxX, boxY, boxWidth, boxHeight);
        if (tab == Tab.ABOUT) {
            drawCommandGuide(graphics, boxX, boxY, boxWidth, boxHeight);
            return;
        }
        String prefix = tab == Tab.ABOUT ? "screen.advanced-economics.about." : "screen.advanced-economics.credits.";
        int y = boxY + 12;
        for (int i = 1; i <= 8; i++) {
            Component line = Component.translatable(prefix + i);
            y = drawWrappedText(graphics, line, boxX + 14, y, boxWidth - 28, i == 1 ? LABEL_COLOR : MUTED_COLOR, 2) + 3;
        }
    }

    private void drawCommandGuide(GuiGraphicsExtractor graphics, int boxX, int boxY, int boxWidth, int boxHeight) {
        int contentX = boxX + 12;
        int contentY = boxY + 10 - scrollOffset;
        int maxWidth = boxWidth - 28;
        graphics.enableScissor(boxX + 2, boxY + 2, boxX + boxWidth - 2, boxY + boxHeight - 2);
        graphics.text(this.font, Component.literal("Commands"), contentX, contentY, LABEL_COLOR, false);
        contentY += 14;
        for (AdvancedEconomicsMod.CommandHelp command : AdvancedEconomicsMod.COMMAND_HELP) {
            graphics.text(this.font, Component.literal(command.syntax()), contentX, contentY, MONEY_COLOR, false);
            contentY += 11;
            contentY = drawWrappedText(graphics, Component.literal(command.description() + " " + command.example()), contentX + 8, contentY, maxWidth - 8, MUTED_COLOR, 2) + 4;
        }
        graphics.disableScissor();
        int contentHeight = aboutContentHeight(maxWidth);
        int visibleHeight = boxHeight - 10;
        if (contentHeight > visibleHeight) {
            int scrollX = boxX + boxWidth - 7;
            int thumbHeight = Math.max(18, visibleHeight * visibleHeight / contentHeight);
            int maxTravel = visibleHeight - thumbHeight;
            int thumbY = boxY + 5 + maxTravel * scrollOffset / Math.max(1, contentHeight - visibleHeight);
            graphics.fill(scrollX, boxY + 5, scrollX + 3, boxY + boxHeight - 5, 0xFF6E6E6E);
            graphics.fill(scrollX, thumbY, scrollX + 3, thumbY + thumbHeight, 0xFFCFCFCF);
        }
    }

    private int aboutContentHeight(int maxWidth) {
        int height = 24;
        for (AdvancedEconomicsMod.CommandHelp command : AdvancedEconomicsMod.COMMAND_HELP) {
            height += 11;
            height += wrappedLineCount(command.description() + " " + command.example(), maxWidth - 8, 2) * 11 + 4;
        }
        return height;
    }

    private int wrappedLineCount(String text, int maxWidth, int maxLines) {
        String[] words = text.split(" ");
        String line = "";
        int lines = 0;
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (this.font.width(candidate) > maxWidth && !line.isEmpty()) {
                lines++;
                if (lines >= maxLines) {
                    return lines;
                }
                line = word;
            } else {
                line = candidate;
            }
        }
        return line.isEmpty() ? lines : Math.min(maxLines, lines + 1);
    }

    private void drawFittedText(GuiGraphicsExtractor graphics, Component text, int x, int y, int maxWidth, int color) {
        String value = fittedValue(text, maxWidth);
        graphics.text(this.font, Component.literal(value), x, y, color, false);
    }

    private void drawCenteredFittedText(GuiGraphicsExtractor graphics, Component text, int centerX, int y, int maxWidth, int color) {
        String value = fittedValue(text, maxWidth);
        graphics.text(this.font, Component.literal(value), centerX - this.font.width(value) / 2, y, color, false);
    }

    private String fittedValue(Component text, int maxWidth) {
        String value = text.getString();
        if (this.font.width(value) > maxWidth) {
            while (!value.isEmpty() && this.font.width(value + ".") > maxWidth) {
                value = value.substring(0, value.length() - 1);
            }
            value = value + ".";
        }
        return value;
    }

    private int drawWrappedText(GuiGraphicsExtractor graphics, Component text, int x, int y, int maxWidth, int color, int maxLines) {
        String[] words = text.getString().split(" ");
        String line = "";
        int lines = 0;
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (this.font.width(candidate) > maxWidth && !line.isEmpty()) {
                graphics.text(this.font, Component.literal(line), x, y, color, false);
                y += 11;
                lines++;
                if (lines >= maxLines) {
                    return y;
                }
                line = word;
            } else {
                line = candidate;
            }
        }
        if (!line.isEmpty() && lines < maxLines) {
            graphics.text(this.font, Component.literal(line), x, y, color, false);
            y += 11;
        }
        return y;
    }

    private int rowAccentColor(EconomyStatePayload.Entry entry) {
        if (!entry.unlocked()) {
            return LOCKED_COLOR;
        }
        if (tab == Tab.UNLOCKS) {
            return UNLOCKED_COLOR;
        }
        return ACCENT_COLOR;
    }

    private int tabAccentColor(Tab value) {
        return ACCENT_COLOR;
    }

    private int darken(int argb) {
        int red = ((argb >> 16) & 0xFF) * 3 / 5;
        int green = ((argb >> 8) & 0xFF) * 3 / 5;
        int blue = (argb & 0xFF) * 3 / 5;
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private Item tabIcon(Tab value) {
        return switch (value) {
            case SHOP -> Items.EMERALD;
            case SELL -> Items.CHEST;
            case WALLET -> Items.GOLD_INGOT;
            case INVEST -> Items.DIAMOND;
            case UNLOCKS -> Items.IRON_PICKAXE;
            case QUESTS -> Items.MAP;
            case GAMBLE -> Items.EXPERIENCE_BOTTLE;
            case SETTINGS -> Items.COMPARATOR;
            case ABOUT -> Items.BOOK;
            case CREDITS -> Items.NAME_TAG;
        };
    }

    private void drawCenteredTextNoShadow(GuiGraphicsExtractor graphics, Component text, int centerX, int y, int color) {
        graphics.text(this.font, text, centerX - this.font.width(text) / 2, y, color, false);
    }

    private void drawVanillaPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL_FILL);
        graphics.horizontalLine(x + 1, x + width - 2, y + 1, PANEL_LIGHT_EDGE);
        graphics.verticalLine(x + 1, y + 1, y + height - 2, PANEL_LIGHT_EDGE);
        graphics.horizontalLine(x + 1, x + width - 2, y + height - 2, PANEL_DARK_EDGE);
        graphics.verticalLine(x + width - 2, y + 1, y + height - 2, PANEL_DARK_EDGE);
        graphics.horizontalLine(x, x + width - 1, y, PANEL_DARK_EDGE);
        graphics.verticalLine(x, y, y + height - 1, PANEL_DARK_EDGE);
        graphics.horizontalLine(x, x + width - 1, y + height - 1, PANEL_SHADOW);
        graphics.verticalLine(x + width - 1, y, y + height - 1, PANEL_SHADOW);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (tab == Tab.ABOUT) {
            int boxHeight = panelHeight() - 126;
            int contentHeight = aboutContentHeight(panelWidth() - 64);
            int maxScroll = Math.max(0, contentHeight - (boxHeight - 10));
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(verticalAmount) * 18));
            return true;
        }
        if (mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= listTop && mouseY <= listTop + listHeight) {
            scrollOffset -= (int) Math.signum(verticalAmount);
            clampScroll(filteredEntries().size());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && clickDailyQuestToast(mouseX, mouseY)) {
            return true;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && clickSettingsAction(mouseX, mouseY)) {
            return true;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && clickQuestPanelAction(mouseX, mouseY)) {
            return true;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && clickListAction((int) event.x(), (int) event.y())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (AdvancedEconomicsClient.isOpenEconomyKey(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private boolean clickDailyQuestToast(int mouseX, int mouseY) {
        EconomyStatePayload.DailyQuestState quest = AdvancedEconomicsClient.latestState().dailyQuest();
        if (quest.hidden() || quest.required() <= 0) {
            return false;
        }

        int x = Math.max(8, this.width - 150);
        int y = 10;
        if (mouseX >= x + 130 && mouseX <= x + 142 && mouseY >= y + 3 && mouseY <= y + 17) {
            runCommand("dailyquest hide");
            return true;
        }
        if (!quest.claimed() && quest.progress() >= quest.required() && mouseX >= x + 94 && mouseX <= x + 136 && mouseY >= y + 31 && mouseY <= y + 46) {
            runCommand("dailyquest claim");
            return true;
        }
        return false;
    }

    private boolean clickSettingsAction(int mouseX, int mouseY) {
        if (tab != Tab.SETTINGS) {
            return false;
        }

        int left = panelLeft();
        int startY = contentTop(panelTop());
        String[] names = settingNames();
        for (int i = 0; i < names.length; i++) {
            int row = i % 6;
            int col = i / 6;
            int x = left + 14 + col * 196;
            int y = startY + row * 24;
            if (mouseX >= x && mouseX <= x + 184 && mouseY >= y && mouseY <= y + 20) {
                runCommand("aesettings toggle " + names[i]);
                return true;
            }
        }
        return false;
    }

    private boolean clickQuestPanelAction(int mouseX, int mouseY) {
        if (tab != Tab.QUESTS) {
            return false;
        }
        EconomyStatePayload.DailyQuestState daily = AdvancedEconomicsClient.latestState().dailyQuest();
        int left = panelLeft();
        int top = panelTop();
        int dailyY = contentTop(top) + 8;
        int x = left + panelWidth() - 96;
        int y = dailyY + 24;
        if (!daily.claimed() && daily.progress() >= daily.required() && mouseX >= x && mouseX <= x + 64 && mouseY >= y && mouseY <= y + 15) {
            runCommand("dailyquest claim");
            return true;
        }
        return false;
    }

    private boolean clickListAction(int mouseX, int mouseY) {
        if (tab != Tab.SHOP && tab != Tab.UNLOCKS && tab != Tab.SELL) {
            return false;
        }

        if (mouseX < listLeft || mouseX > listLeft + listWidth || mouseY < listTop || mouseY > listTop + listHeight) {
            return false;
        }

        int row = (mouseY - listTop) / ROW_HEIGHT;
        List<EconomyStatePayload.Entry> entries = filteredEntries();
        int index = scrollOffset + row;
        if (index < 0 || index >= entries.size()) {
            return false;
        }

        int actionX = listLeft + listWidth - 66;
        int actionY = listTop + row * ROW_HEIGHT + 4;
        if (mouseX < actionX || mouseX > actionX + 52 || mouseY < actionY || mouseY > actionY + 18) {
            return false;
        }

        EconomyStatePayload.Entry entry = entries.get(index);
        if (tab == Tab.SELL && entry.inventoryCount() > 0) {
            runCommand("sell " + entry.primaryName() + " " + cleanAmount());
        } else if (tab == Tab.SHOP && entry.unlocked()) {
            runCommand("buy " + entry.primaryName() + " " + cleanAmount());
        } else if (!entry.unlocked()) {
            runCommand("unlock " + entry.primaryName());
        }
        return true;
    }

    private List<EconomyStatePayload.Entry> filteredEntries() {
        List<EconomyStatePayload.Entry> entries = new ArrayList<>(AdvancedEconomicsClient.latestState().entries());
        Comparator<EconomyStatePayload.Entry> priceComparator = switch (priceSort) {
            case NAME -> Comparator.comparing(EconomyStatePayload.Entry::primaryName);
            case SELL -> Comparator.comparingInt(EconomyStatePayload.Entry::sellPrice).reversed()
                    .thenComparing(EconomyStatePayload.Entry::primaryName);
            case BUY -> Comparator.comparingInt(EconomyStatePayload.Entry::buyPrice).reversed()
                    .thenComparing(EconomyStatePayload.Entry::primaryName);
        };
        entries.sort(Comparator
                .comparing((EconomyStatePayload.Entry entry) -> tab == Tab.UNLOCKS && !entry.unlocked())
                .thenComparing(entry -> !entry.unlocked())
                .thenComparing(priceComparator));

        if (tab == Tab.SELL) {
            entries.removeIf(entry -> entry.inventoryCount() <= 0);
        }
        String query = searchValue.trim().toLowerCase(Locale.ROOT);
        if (!query.isEmpty()) {
            entries.removeIf(entry -> !matchesSearch(entry, query));
        }
        return entries;
    }

    private boolean matchesSearch(EconomyStatePayload.Entry entry, String query) {
        ItemStack stack = new ItemStack(itemFor(entry));
        return itemName(entry, stack).getString().toLowerCase(Locale.ROOT).contains(query)
                || entry.displayName().toLowerCase(Locale.ROOT).contains(query)
                || entry.primaryName().toLowerCase(Locale.ROOT).contains(query)
                || entry.itemId().toLowerCase(Locale.ROOT).contains(query);
    }

    private int visibleRows() {
        return Math.max(1, listHeight / ROW_HEIGHT);
    }

    private void clampScroll(int size) {
        int maxScroll = Math.max(0, size - visibleRows());
        if (scrollOffset < 0) {
            scrollOffset = 0;
        } else if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
    }

    private Item itemFor(EconomyStatePayload.Entry entry) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(entry.itemId()));
        return item == null ? Items.BARRIER : item;
    }

    private Item itemFromId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return Items.BARRIER;
        }
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
        return item == null ? Items.BARRIER : item;
    }

    private String[] settingNames() {
        return new String[]{"shop_sell", "wallet", "investments", "gambling", "quests", "professions", "teleports", "xp_trading", "item_services", "pets", "insurance", "loans"};
    }

    private Item[] settingIcons() {
        return new Item[]{Items.EMERALD, Items.GOLD_INGOT, Items.DIAMOND, Items.EXPERIENCE_BOTTLE, Items.MAP, Items.WRITABLE_BOOK, Items.ENDER_PEARL, Items.EXPERIENCE_BOTTLE, Items.ANVIL, Items.BONE, Items.TOTEM_OF_UNDYING, Items.PAPER};
    }

    private boolean[] settingValues(EconomyStatePayload.FeatureSettingsState settings) {
        return new boolean[]{
                settings.shopSell(),
                settings.wallet(),
                settings.investments(),
                settings.gambling(),
                settings.quests(),
                settings.professions(),
                settings.teleports(),
                settings.xpTrading(),
                settings.itemServices(),
                settings.pets(),
                settings.insurance(),
                settings.loans()
        };
    }

    private Component itemName(EconomyStatePayload.Entry entry, ItemStack stack) {
        if (stack.is(Items.BARRIER)) {
            return Component.literal(entry.displayName());
        }
        return stack.getHoverName();
    }

    private Component stateLabel(EconomyStatePayload.Entry entry) {
        if (entry.unlockSource().isBlank()) {
            return Component.translatable("screen.advanced-economics.state.unlocked");
        }
        if (entry.unlockSource().equalsIgnoreCase("found")) {
            return Component.translatable("screen.advanced-economics.state.found");
        }
        if (entry.unlockSource().equalsIgnoreCase("paid")) {
            return Component.translatable("screen.advanced-economics.state.paid");
        }
        return Component.literal(entry.unlockSource());
    }

    private void runCommand(String command) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        String cleaned = command.trim();
        if (cleaned.contains("  ") || cleaned.endsWith("send") || cleaned.endsWith("send all")) {
            player.sendSystemMessage(Component.translatable("screen.advanced-economics.error.required_fields"));
            return;
        }

        player.connection.sendCommand(cleaned);
    }

    private String cleanAmount() {
        return fallback(amountField == null ? "" : amountField.getValue(), "1");
    }

    private String cleanPlayer() {
        return fallback(playerField == null ? "" : playerField.getValue(), "");
    }

    private String fallback(String value, String fallback) {
        String cleaned = value.trim();
        return cleaned.isEmpty() ? fallback : cleaned;
    }

    private String formatMoney(int units) {
        return "$" + (units / 100) + "." + String.format(Locale.ROOT, "%02d", Math.abs(units % 100));
    }

    private int panelLeft() {
        return Math.max(12, (this.width - panelWidth()) / 2);
    }

    private int panelTop() {
        return Math.max(68, (this.height - panelHeight()) / 2 + 18);
    }

    private int panelWidth() {
        return Math.max(360, Math.min(PANEL_WIDTH, this.width - 24));
    }

    private int panelHeight() {
        return Math.max(224, Math.min(PANEL_HEIGHT, this.height - 72));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private interface CommandFactory {
        String command();
    }

    private class StyledButton extends Button {
        private final Item icon;
        private boolean mousePressed;

        private StyledButton(int x, int y, int width, int height, Component message, OnPress onPress, Item icon) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.icon = icon;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            boolean pressed = pressedStyle();
            drawButtonFrame(graphics, getX(), getY(), getWidth(), getHeight(), pressed);
            if (!getMessage().getString().isEmpty()) {
                drawButtonContent(graphics, pressed);
            }
        }

        private void drawButtonContent(GuiGraphicsExtractor graphics, boolean pressed) {
            String value = fittedValue(getMessage(), getWidth() - (icon == null ? 8 : 26));
            int textWidth = font.width(value);
            int contentWidth = textWidth + (icon == null ? 0 : 20);
            int contentX = getX() + (getWidth() - contentWidth) / 2;
            int textY = getY() + Math.max(1, (getHeight() - font.lineHeight) / 2);
            if (icon != null) {
                graphics.item(new ItemStack(icon), contentX, getY() + Math.max(1, (getHeight() - 16) / 2));
                contentX += 20;
            }
            graphics.text(font, Component.literal(value), contentX, textY, buttonTextColor(pressed), true);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            boolean clicked = super.mouseClicked(event, doubleClick);
            if (clicked) {
                mousePressed = true;
            }
            return clicked;
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            mousePressed = false;
            return super.mouseReleased(event);
        }

        @Override
        public void onPress(InputWithModifiers modifiers) {
            mousePressed = false;
            super.onPress(modifiers);
        }

        private boolean pressedStyle() {
            return mousePressed || !active;
        }
    }

    private enum Tab {
        SHOP("screen.advanced-economics.tab.shop", "shop"),
        SELL("screen.advanced-economics.tab.sell", "sell"),
        WALLET("screen.advanced-economics.tab.wallet", "wallet"),
        INVEST("screen.advanced-economics.tab.invest", "invest"),
        UNLOCKS("screen.advanced-economics.tab.unlocks", "unlocks"),
        QUESTS("screen.advanced-economics.tab.quests", "quests"),
        GAMBLE("screen.advanced-economics.tab.gamble", "gamble"),
        SETTINGS("screen.advanced-economics.tab.settings", "settings"),
        ABOUT("screen.advanced-economics.tab.about", "about"),
        CREDITS("screen.advanced-economics.tab.credits", "credits");

        private final String translationKey;
        private final String translationName;

        Tab(String translationKey, String translationName) {
            this.translationKey = translationKey;
            this.translationName = translationName;
        }
    }

    private enum PriceSort {
        NAME("Name"),
        SELL("Sell $"),
        BUY("Buy $");

        private final String label;

        PriceSort(String label) {
            this.label = label;
        }

        private String label() {
            return label;
        }

        private PriceSort next() {
            PriceSort[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }
}
