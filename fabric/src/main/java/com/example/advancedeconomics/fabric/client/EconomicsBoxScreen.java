package com.example.advancedeconomics.fabric.client;

import com.example.advancedeconomics.fabric.network.*;
import com.example.advancedeconomics.profession.Profession;
import com.example.advancedeconomics.shop.ShopItem;
import com.example.advancedeconomics.shop.ShopTable;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Advanced Economics Box Screen (v0.39).
 *
 * Universal Cursor Fix:
 * - All buttons maintain active = true for normal mouse hover cursor (never shows unavailable/cross cursor).
 * - Visually disabled buttons temporarily toggle active = false only during rendering to draw Minecraft's locked button texture sprite.
 */
public class EconomicsBoxScreen extends Screen {

    public enum Tab {
        SHOP,
        PROFESSION,
        SETTINGS
    }

    private static final int BOX_WIDTH   = 315;
    private static final int BOX_HEIGHT  = 171;
    private static final int BORDER      = 2;
    private static final int BTN_WIDTH   = 72;
    private static final int BTN_HEIGHT  = 18;
    private static final int BTN_GAP     = 3;
    private static final int BTN_MARGIN  = 4;
    private static final int CLOSE_WIDTH = 18;

    private Tab activeTab = Tab.SHOP;
    private int scrollOffset = 0;
    private int professionScrollOffset = 0;
    private int settingsScrollOffset = 0;
    private boolean isDraggingScrollbar = false;

    private EditBox searchBox;
    private String searchQuery = "";

    private Button shopTabBtn;
    private Button profTabBtn;
    private Button setTabBtn;

    private final List<Button> disabledActionButtons = new ArrayList<>();

    public EconomicsBoxScreen() {
        this(Tab.SHOP);
    }

    public EconomicsBoxScreen(Tab initialTab) {
        super(Component.literal("Advanced Economics"));
        if (initialTab != null) {
            this.activeTab = initialTab;
        }
    }

    private List<ShopItem> getSortedShopItems() {
        List<ShopItem> all = ShopTable.getItems();
        List<ShopItem> unlocked = new ArrayList<>();
        List<ShopItem> locked = new ArrayList<>();

        String query = (searchQuery != null) ? searchQuery.trim().toLowerCase() : "";

        for (ShopItem item : all) {
            if (!query.isEmpty()) {
                boolean matchesName = item.displayName().toLowerCase().contains(query);
                boolean matchesId = item.id().toLowerCase().contains(query);
                if (!matchesName && !matchesId) continue;
            }

            if (ClientEconomyState.isUnlocked(item.id())) {
                unlocked.add(item);
            } else {
                locked.add(item);
            }
        }
        unlocked.addAll(locked);
        return unlocked;
    }

    @Override
    protected void init() {
        super.init();

        int boxX  = (this.width  - BOX_WIDTH)  / 2;
        int boxY  = (this.height - BOX_HEIGHT) / 2;
        int btnY  = boxY - BTN_HEIGHT - BTN_MARGIN;

        int visualLeft  = boxX - BORDER;
        int visualRight = boxX + BOX_WIDTH + BORDER;

        // Header Tab Buttons (Keep active = true for normal hover cursor)
        shopTabBtn = Button.builder(Component.literal("Shop"), btn -> switchTab(Tab.SHOP))
                .bounds(visualLeft, btnY, BTN_WIDTH, BTN_HEIGHT).build();
        addRenderableWidget(shopTabBtn);

        profTabBtn = Button.builder(Component.literal("Profession"), btn -> switchTab(Tab.PROFESSION))
                .bounds(visualLeft + BTN_WIDTH + BTN_GAP, btnY, BTN_WIDTH, BTN_HEIGHT).build();
        addRenderableWidget(profTabBtn);

        setTabBtn = Button.builder(Component.literal("Settings"), btn -> switchTab(Tab.SETTINGS))
                .bounds(visualLeft + (BTN_WIDTH + BTN_GAP) * 2, btnY, BTN_WIDTH, BTN_HEIGHT).build();
        addRenderableWidget(setTabBtn);

        addRenderableWidget(Button.builder(Component.literal("✕"), btn -> this.onClose())
                .bounds(visualRight - CLOSE_WIDTH, btnY, CLOSE_WIDTH, BTN_HEIGHT)
                .build());

        rebuildTabContent(boxX, boxY);
    }

    private void switchTab(Tab tab) {
        this.activeTab = tab;
        this.rebuildWidgets();
    }

    private void rebuildTabContent(int boxX, int boxY) {
        disabledActionButtons.clear();

        if (activeTab == Tab.SHOP) {
            buildShopTabWidgets(boxX, boxY);
        } else if (activeTab == Tab.SETTINGS) {
            buildSettingsTabWidgets(boxX, boxY);
        } else if (activeTab == Tab.PROFESSION) {
            buildProfessionTabWidgets(boxX, boxY);
        }
    }

    private void buildShopTabWidgets(int boxX, int boxY) {
        int searchX = boxX + 6;
        int searchY = boxY + 5;
        int searchW = 175;
        int searchH = 16;

        searchBox = new EditBox(this.getFont(), searchX, searchY, searchW, searchH, Component.literal("Search"));
        searchBox.setHint(Component.literal("Search items..."));
        searchBox.setMaxLength(256);
        searchBox.setValue(this.searchQuery);

        searchBox.setResponder(text -> {
            this.searchQuery = text;
            this.scrollOffset = 0;
            boolean wasFocused = (this.searchBox != null && (this.searchBox.isFocused() || this.getFocused() == this.searchBox));
            int pos = (this.searchBox != null) ? this.searchBox.getCursorPosition() : text.length();

            this.rebuildWidgets();

            if (this.searchBox != null && wasFocused) {
                this.setFocused(this.searchBox);
                this.searchBox.setFocused(true);
                this.searchBox.setCursorPosition(pos);
            }
        });

        addRenderableWidget(searchBox);

        List<ShopItem> sortedItems = getSortedShopItems();
        int maxOffset = Math.max(0, sortedItems.size() - 4);
        scrollOffset = Math.clamp(scrollOffset, 0, maxOffset);

        // Sleek Thinner Scroll Buttons (12px wide x 10px high)
        addRenderableWidget(Button.builder(Component.literal("▲"), btn -> {
            if (scrollOffset > 0) {
                scrollOffset--;
                rebuildWidgets();
            }
        }).bounds(boxX + BOX_WIDTH - 21, boxY + 26, 12, 10).build());

        addRenderableWidget(Button.builder(Component.literal("▼"), btn -> {
            if (scrollOffset < maxOffset) {
                scrollOffset++;
                rebuildWidgets();
            }
        }).bounds(boxX + BOX_WIDTH - 21, boxY + 154, 12, 10).build());

        int startY = boxY + 24;
        int rowHeight = 36;

        int visibleCount = Math.min(4, sortedItems.size() - scrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            final ShopItem shopItem = sortedItems.get(scrollOffset + i);
            int rowY = startY + (i * rowHeight);

            boolean isUnlocked = ClientEconomyState.isUnlocked(shopItem.id());
            double sellPrice   = shopItem.basePrice() * ClientEconomyState.getSellMultiplier();
            double buyPrice    = shopItem.basePrice() * ClientEconomyState.getBuyMultiplier();
            double unlockPrice = shopItem.basePrice() * ClientEconomyState.getUnlockMultiplier();

            boolean hasItemInInventory = false;
            if (this.minecraft != null && this.minecraft.player != null) {
                Item mcItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", shopItem.id()));
                if (mcItem != null && mcItem != Items.AIR) {
                    hasItemInInventory = this.minecraft.player.getInventory().contains(new ItemStack(mcItem));
                }
            }

            if (isUnlocked) {
                String sellStr = formatPrice(sellPrice);
                String buyStr  = formatPrice(buyPrice);

                boolean canSell = hasItemInInventory && ClientEconomyState.isAllowSelling();
                Button sellBtn = Button.builder(Component.literal("Sell " + sellStr), btn -> {
                    if (canSell) {
                        ClientPlayNetworking.send(new RequestSellPayload(shopItem.id()));
                    }
                }).bounds(boxX + 168, rowY + 8, 64, 18).build();
                addRenderableWidget(sellBtn);

                if (!canSell) {
                    disabledActionButtons.add(sellBtn);
                }

                boolean canBuy = ClientEconomyState.isAllowBuying();
                Button buyBtn = Button.builder(Component.literal("Buy " + buyStr), btn -> {
                    if (canBuy) {
                        ClientPlayNetworking.send(new RequestBuyPayload(shopItem.id()));
                    }
                }).bounds(boxX + 236, rowY + 8, 58, 18).build();
                addRenderableWidget(buyBtn);

                if (!canBuy) {
                    disabledActionButtons.add(buyBtn);
                }
            } else {
                String unlockStr = formatPrice(unlockPrice);

                boolean canUnlock = ClientEconomyState.isAllowUnlocking();
                Button unlockBtn = Button.builder(Component.literal("Unlock " + unlockStr), btn -> {
                    if (canUnlock) {
                        ClientPlayNetworking.send(new RequestUnlockPayload(shopItem.id()));
                    }
                }).bounds(boxX + 220, rowY + 8, 74, 18).build();
                addRenderableWidget(unlockBtn);

                if (!canUnlock) {
                    disabledActionButtons.add(unlockBtn);
                }
            }
        }
    }

    private void buildProfessionTabWidgets(int boxX, int boxY) {
        Profession[] selectable = new Profession[]{
                Profession.LUMBERJACK, Profession.MINER, Profession.FARMER, Profession.HUNTER, Profession.WEAPONSMITH
        };

        int maxOffset = Math.max(0, selectable.length - 3);
        professionScrollOffset = Math.clamp(professionScrollOffset, 0, maxOffset);

        // Scroll Buttons below grey line
        addRenderableWidget(Button.builder(Component.literal("▲"), btn -> {
            if (professionScrollOffset > 0) {
                professionScrollOffset--;
                rebuildWidgets();
            }
        }).bounds(boxX + BOX_WIDTH - 21, boxY + 36, 12, 10).build());

        addRenderableWidget(Button.builder(Component.literal("▼"), btn -> {
            if (professionScrollOffset < maxOffset) {
                professionScrollOffset++;
                rebuildWidgets();
            }
        }).bounds(boxX + BOX_WIDTH - 21, boxY + 154, 12, 10).build());

        int startY = boxY + 36;
        int rowHeight = 43;
        String currentProfName = ClientEconomyState.getProfession();

        int visibleCount = Math.min(3, selectable.length - professionScrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            final Profession prof = selectable[professionScrollOffset + i];
            int rowY = startY + (i * rowHeight);

            boolean isCurrent = currentProfName.equalsIgnoreCase(prof.getDisplayName()) || currentProfName.equalsIgnoreCase(prof.name());

            if (!isCurrent) {
                boolean canSelect = ClientEconomyState.isEnableProfessions();
                Button selectBtn = Button.builder(Component.literal("Select"), btn -> {
                    if (canSelect) {
                        ClientPlayNetworking.send(new RequestSetProfessionPayload(prof.name()));
                        ClientEconomyState.setProfession(prof.getDisplayName());
                        rebuildWidgets();
                    }
                }).bounds(boxX + 240, rowY + 12, 54, 18).build();
                addRenderableWidget(selectBtn);

                if (!canSelect) {
                    disabledActionButtons.add(selectBtn);
                }
            }
        }
    }

    // Settings logical rows: 0=divider(Toggles), 1-5=toggles, 6=divider(Multipliers), 7-9=steppers
    // Total = 10 logical rows
    private static final int SETTINGS_TOTAL_ROWS = 10;
    private static final int SETTINGS_ROW_H      = 28; // tighter rows for full-height fit

    private void buildSettingsTabWidgets(int boxX, int boxY) {
        int visibleH    = BOX_HEIGHT - 8;           // full tab area top-to-bottom
        int visibleRows = visibleH / SETTINGS_ROW_H;
        int maxOffset   = Math.max(0, SETTINGS_TOTAL_ROWS - visibleRows);
        settingsScrollOffset = Math.clamp(settingsScrollOffset, 0, maxOffset);

        int startY = boxY + 4;                      // start right at top of box

        int sellM  = ClientEconomyState.getSellMultiplier();
        int buyM   = ClientEconomyState.getBuyMultiplier();
        int unlockM = ClientEconomyState.getUnlockMultiplier();
        boolean sellOn = ClientEconomyState.isAllowSelling();
        boolean buyOn  = ClientEconomyState.isAllowBuying();
        boolean unkOn  = ClientEconomyState.isAllowUnlocking();
        boolean profOn = ClientEconomyState.isEnableProfessions();
        boolean xpOn   = ClientEconomyState.isEnableXpLeveling();

        // Scroll arrows at far right, full height
        int arrowX = boxX + BOX_WIDTH - 21;
        addRenderableWidget(Button.builder(Component.literal("▲"), btn -> {
            if (settingsScrollOffset > 0) { settingsScrollOffset--; rebuildWidgets(); }
        }).bounds(arrowX, boxY + 4, 12, 10).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), btn -> {
            if (settingsScrollOffset < maxOffset) { settingsScrollOffset++; rebuildWidgets(); }
        }).bounds(arrowX, boxY + BOX_HEIGHT - 14, 12, 10).build());

        for (int vi = 0; vi < visibleRows; vi++) {
            int logRow = settingsScrollOffset + vi;
            if (logRow >= SETTINGS_TOTAL_ROWS) break;
            int rowY = startY + vi * SETTINGS_ROW_H;
            int btnY = rowY + (SETTINGS_ROW_H - 18) / 2;

            // row 0 = Toggles divider, row 6 = Multipliers divider → no widget
            switch (logRow) {
                case 1 -> { // Selling
                    String lbl = sellOn ? "§a[ON]" : "§c[OFF]";
                    addRenderableWidget(Button.builder(Component.literal(lbl), btn ->
                        sendUpdateSettings(sellM, buyM, unlockM, !sellOn, buyOn, unkOn, profOn, xpOn)
                    ).bounds(boxX + 236, btnY, 58, 18).build());
                }
                case 2 -> { // Buying
                    String lbl = buyOn ? "§a[ON]" : "§c[OFF]";
                    addRenderableWidget(Button.builder(Component.literal(lbl), btn ->
                        sendUpdateSettings(sellM, buyM, unlockM, sellOn, !buyOn, unkOn, profOn, xpOn)
                    ).bounds(boxX + 236, btnY, 58, 18).build());
                }
                case 3 -> { // Unlocking
                    String lbl = unkOn ? "§a[ON]" : "§c[OFF]";
                    addRenderableWidget(Button.builder(Component.literal(lbl), btn ->
                        sendUpdateSettings(sellM, buyM, unlockM, sellOn, buyOn, !unkOn, profOn, xpOn)
                    ).bounds(boxX + 236, btnY, 58, 18).build());
                }
                case 4 -> { // Professions
                    String lbl = profOn ? "§a[ON]" : "§c[OFF]";
                    addRenderableWidget(Button.builder(Component.literal(lbl), btn ->
                        sendUpdateSettings(sellM, buyM, unlockM, sellOn, buyOn, unkOn, !profOn, xpOn)
                    ).bounds(boxX + 236, btnY, 58, 18).build());
                }
                case 5 -> { // XP Leveling
                    String lbl = xpOn ? "§a[ON]" : "§c[OFF]";
                    addRenderableWidget(Button.builder(Component.literal(lbl), btn ->
                        sendUpdateSettings(sellM, buyM, unlockM, sellOn, buyOn, unkOn, profOn, !xpOn)
                    ).bounds(boxX + 236, btnY, 58, 18).build());
                }
                case 7 -> { // Sell ×
                    addRenderableWidget(Button.builder(Component.literal("-"), btn ->
                        sendUpdateSettings(Math.max(1, sellM - 1), buyM, unlockM, sellOn, buyOn, unkOn, profOn, xpOn)
                    ).bounds(boxX + 236, btnY, 26, 18).build());
                    addRenderableWidget(Button.builder(Component.literal("+"), btn ->
                        sendUpdateSettings(sellM + 1, buyM, unlockM, sellOn, buyOn, unkOn, profOn, xpOn)
                    ).bounds(boxX + 268, btnY, 26, 18).build());
                }
                case 8 -> { // Buy ×
                    addRenderableWidget(Button.builder(Component.literal("-"), btn ->
                        sendUpdateSettings(sellM, Math.max(1, buyM - 1), unlockM, sellOn, buyOn, unkOn, profOn, xpOn)
                    ).bounds(boxX + 236, btnY, 26, 18).build());
                    addRenderableWidget(Button.builder(Component.literal("+"), btn ->
                        sendUpdateSettings(sellM, buyM + 1, unlockM, sellOn, buyOn, unkOn, profOn, xpOn)
                    ).bounds(boxX + 268, btnY, 26, 18).build());
                }
                case 9 -> { // Unlock ×
                    addRenderableWidget(Button.builder(Component.literal("-"), btn ->
                        sendUpdateSettings(sellM, buyM, Math.max(1, unlockM - 1), sellOn, buyOn, unkOn, profOn, xpOn)
                    ).bounds(boxX + 236, btnY, 26, 18).build());
                    addRenderableWidget(Button.builder(Component.literal("+"), btn ->
                        sendUpdateSettings(sellM, buyM, unlockM + 1, sellOn, buyOn, unkOn, profOn, xpOn)
                    ).bounds(boxX + 268, btnY, 26, 18).build());
                }
            }
        }
    }

    private void sendUpdateSettings(int sellM, int buyM, int unlockM, boolean sellOn, boolean buyOn, boolean unkOn, boolean profOn, boolean xpOn) {
        ClientPlayNetworking.send(new RequestUpdateSettingsPayload(sellM, buyM, unlockM, sellOn, buyOn, unkOn, profOn, xpOn));
        ClientEconomyState.setSettings(sellM, buyM, unlockM, sellOn, buyOn, unkOn, profOn, xpOn);
        rebuildWidgets();
    }

    private String formatPrice(double price) {
        if (price < 1.0) {
            return String.format(Locale.US, "$%.2f", price);
        } else if (price == (long) price) {
            return String.format(Locale.US, "$%d", (long) price);
        } else {
            return String.format(Locale.US, "$%.2f", price);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Pass 1: Translucent screen overlay
        graphics.fill(0, 0, this.width, this.height, 0xA0000000);

        int boxX = (this.width  - BOX_WIDTH)  / 2;
        int boxY = (this.height - BOX_HEIGHT) / 2;

        // Pass 2: Outer container frame & dark box
        graphics.fill(boxX - BORDER, boxY - BORDER,
                      boxX + BOX_WIDTH + BORDER, boxY + BOX_HEIGHT + BORDER, 0xFF777777);
        graphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, 0xF0101010);

        // Pass 3: Tab-specific backgrounds, text & item icons
        if (activeTab == Tab.SHOP) {
            renderMoneyBalanceBox(graphics, boxX + BOX_WIDTH - 124, boxY + 5, 118, 16);
            graphics.fill(boxX + 5, boxY + 24, boxX + BOX_WIDTH - 10, boxY + 168, 0xFF0A0A0A);
            renderShopRows(graphics, boxX, boxY);
            renderDraggableScrollbar(graphics, boxX, boxY);
        } else if (activeTab == Tab.SETTINGS) {
            graphics.fill(boxX + 4, boxY + 4, boxX + BOX_WIDTH - 14, boxY + BOX_HEIGHT - 4, 0xFF0A0A0A);
            renderSettingsTab(graphics, boxX, boxY);
            renderSettingsScrollbar(graphics, boxX, boxY, SETTINGS_TOTAL_ROWS);
        } else if (activeTab == Tab.PROFESSION) {
            renderProfessionTab(graphics, boxX, boxY);
        }

        // Pass 4: Temporarily set active = false for disabled buttons to render locked texture sprite, then restore active = true for normal hover cursor!
        for (Button disabledBtn : disabledActionButtons) {
            disabledBtn.active = false;
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        for (Button disabledBtn : disabledActionButtons) {
            disabledBtn.active = true;
        }

        // Pass 5: Draw a dark translucent overlay over inactive tabs + white text.
        // IMPORTANT: active is NEVER set to false on tab buttons — that would trigger cursor change.
        // Instead, paint a semi-transparent dark rect over them to create the "inactive" look.
        int btnY = boxY - BTN_HEIGHT - BTN_MARGIN;
        int visualLeft = boxX - BORDER;

        int shopX = visualLeft;
        int profX = visualLeft + BTN_WIDTH + BTN_GAP;
        int setX  = visualLeft + (BTN_WIDTH + BTN_GAP) * 2;

        if (activeTab != Tab.SHOP) {
            // Dark overlay over the shop tab button to make it look inactive
            graphics.fill(shopX, btnY, shopX + BTN_WIDTH, btnY + BTN_HEIGHT, 0x88000000);
            graphics.centeredText(this.getFont(), Component.literal("Shop"), shopX + BTN_WIDTH / 2, btnY + 5, 0xFFFFFFFF);
        }
        if (activeTab != Tab.PROFESSION) {
            graphics.fill(profX, btnY, profX + BTN_WIDTH, btnY + BTN_HEIGHT, 0x88000000);
            graphics.centeredText(this.getFont(), Component.literal("Profession"), profX + BTN_WIDTH / 2, btnY + 5, 0xFFFFFFFF);
        }
        if (activeTab != Tab.SETTINGS) {
            graphics.fill(setX, btnY, setX + BTN_WIDTH, btnY + BTN_HEIGHT, 0x88000000);
            graphics.centeredText(this.getFont(), Component.literal("Settings"), setX + BTN_WIDTH / 2, btnY + 5, 0xFFFFFFFF);
        }
    }

    private void renderMoneyBalanceBox(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF444444);
        graphics.fill(x, y, x + width, y + height, 0xFF000000);

        long balanceCents = ClientEconomyState.getBalance();
        double balanceDollars = balanceCents / 100.0;
        String text = String.format(Locale.US, "§a$%.2f", balanceDollars);
        graphics.centeredText(this.getFont(), Component.literal(text), x + width / 2, y + 4, 0xFF55FF55);
    }

    private void renderShopRows(GuiGraphicsExtractor graphics, int boxX, int boxY) {
        List<ShopItem> sortedItems = getSortedShopItems();
        int startY = boxY + 24;
        int rowHeight = 36;

        int visibleCount = Math.min(4, sortedItems.size() - scrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            ShopItem shopItem = sortedItems.get(scrollOffset + i);
            int rowY = startY + (i * rowHeight);
            boolean isUnlocked = ClientEconomyState.isUnlocked(shopItem.id());

            int bgTint = isUnlocked ? 0xFF181818 : 0xFF101010;
            graphics.fill(boxX + 6, rowY + 1, boxX + BOX_WIDTH - 20, rowY + rowHeight - 1, bgTint);
            graphics.fill(boxX + 6, rowY + rowHeight - 1, boxX + BOX_WIDTH - 20, rowY + rowHeight, 0xFF282828);

            Item mcItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", shopItem.id()));
            if (mcItem != null && mcItem != Items.AIR) {
                graphics.item(new ItemStack(mcItem), boxX + 8, rowY + 9);
            }

            int nameColor = isUnlocked ? 0xFFFFFFFF : 0xFF777777;
            graphics.text(this.getFont(), Component.literal(shopItem.displayName()), boxX + 28, rowY + 7, nameColor, true);

            String statusStr = isUnlocked ? "§a✔ Unlocked" : "§7🔒 Locked";
            graphics.text(this.getFont(), Component.literal(statusStr), boxX + 28, rowY + 19, 0xFFAAAAAA, true);
        }
    }

    private void renderDraggableScrollbar(GuiGraphicsExtractor graphics, int boxX, int boxY) {
        List<ShopItem> sortedItems = getSortedShopItems();
        int totalItems = sortedItems.size();
        int maxOffset = Math.max(0, totalItems - 4);
        if (maxOffset <= 0) return;

        int trackX = boxX + BOX_WIDTH - 18;
        int trackY = boxY + 38;
        int trackW = 6;
        int trackH = 114;

        graphics.fill(trackX, trackY, trackX + trackW, trackY + trackH, 0xFF151515);

        int thumbH = Math.max(16, (trackH * 4) / Math.max(1, totalItems));
        int thumbY = trackY + ((trackH - thumbH) * scrollOffset) / maxOffset;
        int thumbColor = isDraggingScrollbar ? 0xFFAAAAAA : 0xFF666666;

        graphics.fill(trackX, thumbY, trackX + trackW, thumbY + thumbH, thumbColor);
    }

    private void renderSettingsTab(GuiGraphicsExtractor graphics, int boxX, int boxY) {
        int visibleH    = BOX_HEIGHT - 8;
        int visibleRows = visibleH / SETTINGS_ROW_H;
        int startY      = boxY + 4;

        int sellM   = ClientEconomyState.getSellMultiplier();
        int buyM    = ClientEconomyState.getBuyMultiplier();
        int unlockM = ClientEconomyState.getUnlockMultiplier();

        // Logical row definitions: [name, description]  (null = section divider)
        String[][] rows = new String[][]{
            null,                                                         // 0  Toggles divider
            {"Selling",    "Players can sell items for money"},           // 1
            {"Buying",     "Players can buy unlocked items"},             // 2
            {"Unlocking",  "Players can unlock new items"},              // 3
            {"Professions","Career bonuses & profession tree"},           // 4
            {"XP Leveling","Gain XP and level up professions"},          // 5
            null,                                                         // 6  Multipliers divider
            {"Sell ×",     "Current: " + sellM + "x  (sell price scale)"},  // 7
            {"Buy ×",      "Current: " + buyM  + "x  (buy price scale)"},   // 8
            {"Unlock ×",   "Current: " + unlockM + "x  (unlock price scale)"}, // 9
        };

        String[] dividerLabels = {"Toggles", "Multipliers"};
        int dividerIdx = 0;

        for (int vi = 0; vi < visibleRows; vi++) {
            int logRow = settingsScrollOffset + vi;
            if (logRow >= SETTINGS_TOTAL_ROWS) break;
            int rowY = startY + vi * SETTINGS_ROW_H;

            if (rows[logRow] == null) {
                // Section divider — full-width header strip
                String label = logRow == 0 ? "Toggles" : "Multipliers";
                graphics.fill(boxX + 4, rowY + 2, boxX + BOX_WIDTH - 14, rowY + SETTINGS_ROW_H - 2, 0xFF111111);
                // Left accent bar
                graphics.fill(boxX + 4, rowY + 2, boxX + 7, rowY + SETTINGS_ROW_H - 2, 0xFF4488FF);
                // Divider lines top + bottom
                graphics.fill(boxX + 4, rowY + 2, boxX + BOX_WIDTH - 14, rowY + 3, 0xFF333344);
                graphics.fill(boxX + 4, rowY + SETTINGS_ROW_H - 3, boxX + BOX_WIDTH - 14, rowY + SETTINGS_ROW_H - 2, 0xFF333344);
                graphics.text(this.getFont(), Component.literal("§b§l" + label), boxX + 14, rowY + (SETTINGS_ROW_H - 7) / 2, 0xFF88CCFF, true);
            } else {
                // Normal row
                graphics.fill(boxX + 6, rowY + 1, boxX + BOX_WIDTH - 20, rowY + SETTINGS_ROW_H - 1, 0xFF181818);
                graphics.fill(boxX + 6, rowY + SETTINGS_ROW_H - 1, boxX + BOX_WIDTH - 20, rowY + SETTINGS_ROW_H, 0xFF282828);
                // Bold name
                graphics.text(this.getFont(), Component.literal("§f§l" + rows[logRow][0]), boxX + 14, rowY + 4, 0xFFFFFFFF, true);
                // Dim description
                graphics.text(this.getFont(), Component.literal("§7" + rows[logRow][1]), boxX + 14, rowY + 15, 0xFFAAAAAA, true);
            }
        }
    }

    private void renderSettingsScrollbar(GuiGraphicsExtractor graphics, int boxX, int boxY, int totalItems) {
        int visibleH    = BOX_HEIGHT - 8;
        int visibleRows = visibleH / SETTINGS_ROW_H;
        int maxOffset   = Math.max(0, totalItems - visibleRows);
        if (maxOffset <= 0) return;

        int trackX = boxX + BOX_WIDTH - 18;
        int trackY = boxY + 16;
        int trackW = 5;
        int trackH = BOX_HEIGHT - 28;

        graphics.fill(trackX, trackY, trackX + trackW, trackY + trackH, 0xFF151515);

        int thumbH = Math.max(12, (trackH * visibleRows) / Math.max(1, totalItems));
        int thumbY = trackY + ((trackH - thumbH) * settingsScrollOffset) / maxOffset;
        int thumbColor = isDraggingScrollbar ? 0xFFAAAAAA : 0xFF666666;
        graphics.fill(trackX, thumbY, trackX + trackW, thumbY + thumbH, thumbColor);
    }

    private void renderProfessionTab(GuiGraphicsExtractor graphics, int boxX, int boxY) {
        String profession = ClientEconomyState.getProfession();
        int level = ClientEconomyState.getLevel();
        long xp = ClientEconomyState.getXp();
        long maxXp = ClientEconomyState.getMaxXp();

        // 1. Header Area ABOVE Grey Line
        String headerStr = "Active: §e" + profession + " §7(Level " + level + ")";
        graphics.text(this.getFont(), Component.literal(headerStr), boxX + 8, boxY + 4, 0xFFFFFFFF, true);

        int bonusPct = level * 2;
        graphics.text(this.getFont(), Component.literal("§a+" + bonusPct + "% Sell Bonus"), boxX + 218, boxY + 4, 0xFF55FF55, true);

        // XP Progress Bar ABOVE Grey Line
        int barX = boxX + 8;
        int barY = boxY + 16;
        int barW = 299;
        int barH = 12;

        graphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF444444);
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF0A1A0A);

        double ratio = Math.clamp((double) xp / (double) Math.max(1, maxXp), 0.0, 1.0);
        int fillW = (int) Math.round(barW * ratio);
        if (fillW > 0) {
            graphics.fill(barX, barY, barX + fillW, barY + barH, 0xFF22AA22);
            graphics.fill(barX, barY, barX + fillW, barY + 3, 0xFF55FF55);
        }

        String xpStr = "XP: " + xp + " / " + maxXp;
        graphics.centeredText(this.getFont(), Component.literal(xpStr), this.width / 2, barY + 2, 0xFFFFFFFF);

        // Grey Dividing Line
        graphics.fill(boxX + 6, boxY + 31, boxX + BOX_WIDTH - 6, boxY + 32, 0xFF444444);

        // 2. Profession Selection List BELOW Grey Line
        Profession[] selectable = new Profession[]{
                Profession.LUMBERJACK, Profession.MINER, Profession.FARMER, Profession.HUNTER, Profession.WEAPONSMITH
        };

        graphics.fill(boxX + 5, boxY + 34, boxX + BOX_WIDTH - 10, boxY + 168, 0xFF0A0A0A);

        int maxOffset = Math.max(0, selectable.length - 3);
        professionScrollOffset = Math.clamp(professionScrollOffset, 0, maxOffset);

        int startY = boxY + 36;
        int rowHeight = 43;

        int visibleCount = Math.min(3, selectable.length - professionScrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            Profession prof = selectable[professionScrollOffset + i];
            int rowY = startY + (i * rowHeight);

            boolean isCurrent = profession.equalsIgnoreCase(prof.getDisplayName()) || profession.equalsIgnoreCase(prof.name());

            int bgTint = isCurrent ? 0xFF1E2E1E : 0xFF141414;
            graphics.fill(boxX + 8, rowY + 1, boxX + BOX_WIDTH - 20, rowY + rowHeight - 2, bgTint);
            graphics.fill(boxX + 8, rowY + rowHeight - 2, boxX + BOX_WIDTH - 20, rowY + rowHeight - 1, 0xFF2A2A2A);

            Item profItem = getProfessionItemIcon(prof);
            if (profItem != null && profItem != Items.AIR) {
                graphics.item(new ItemStack(profItem), boxX + 12, rowY + 13);
            }

            int nameColor = isCurrent ? 0xFF55FF55 : 0xFFFFFFFF;
            graphics.text(this.getFont(), Component.literal(prof.getDisplayName()), boxX + 34, rowY + 8, nameColor, true);
            graphics.text(this.getFont(), Component.literal("§7" + prof.getDescription()), boxX + 34, rowY + 22, 0xFFAAAAAA, true);

            if (isCurrent) {
                graphics.text(this.getFont(), Component.literal("§a✔ Active"), boxX + 245, rowY + 14, 0xFF55FF55, true);
            }
        }

        renderProfessionScrollbar(graphics, boxX, boxY, selectable.length);
    }

    private Item getProfessionItemIcon(Profession prof) {
        switch (prof) {
            case LUMBERJACK: return Items.OAK_LOG;
            case MINER: return Items.IRON_PICKAXE;
            case FARMER: return Items.WHEAT;
            case HUNTER: return Items.LEATHER;
            case WEAPONSMITH: return Items.IRON_SWORD;
            default: return Items.AIR;
        }
    }

    private void renderProfessionScrollbar(GuiGraphicsExtractor graphics, int boxX, int boxY, int totalItems) {
        int maxOffset = Math.max(0, totalItems - 3);
        if (maxOffset <= 0) return;

        int trackX = boxX + BOX_WIDTH - 18;
        int trackY = boxY + 48;
        int trackW = 6;
        int trackH = 104;

        graphics.fill(trackX, trackY, trackX + trackW, trackY + trackH, 0xFF151515);

        int thumbH = Math.max(16, (trackH * 3) / Math.max(1, totalItems));
        int thumbY = trackY + ((trackH - thumbH) * professionScrollOffset) / maxOffset;
        int thumbColor = isDraggingScrollbar ? 0xFFAAAAAA : 0xFF666666;

        graphics.fill(trackX, thumbY, trackX + trackW, thumbY + thumbH, thumbColor);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int boxX = (this.width  - BOX_WIDTH)  / 2;
        int boxY = (this.height - BOX_HEIGHT) / 2;

        int trackX = boxX + BOX_WIDTH - 18;
        double mx = event.x();
        double my = event.y();

        if (activeTab == Tab.SHOP) {
            int trackY = boxY + 38;
            int trackW = 6;
            int trackH = 114;

            if (mx >= trackX - 4 && mx <= trackX + trackW + 4 && my >= trackY && my <= trackY + trackH) {
                isDraggingScrollbar = true;
                updateScrollFromMouseY((int) my, trackY, trackH);
                return true;
            }
        } else if (activeTab == Tab.SETTINGS) {
            int trackY = boxY + 38;
            int trackW = 6;
            int trackH = 114;

            if (mx >= trackX - 4 && mx <= trackX + trackW + 4 && my >= trackY && my <= trackY + trackH) {
                isDraggingScrollbar = true;
                updateSettingsScrollFromMouseY((int) my, trackY, trackH);
                return true;
            }
        } else if (activeTab == Tab.PROFESSION) {
            int trackY = boxY + 48;
            int trackW = 6;
            int trackH = 104;

            if (mx >= trackX - 4 && mx <= trackX + trackW + 4 && my >= trackY && my <= trackY + trackH) {
                isDraggingScrollbar = true;
                updateProfessionScrollFromMouseY((int) my, trackY, trackH);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        int boxY = (this.height - BOX_HEIGHT) / 2;

        if (activeTab == Tab.SHOP && isDraggingScrollbar) {
            int trackY = boxY + 38;
            int trackH = 114;
            updateScrollFromMouseY((int) event.y(), trackY, trackH);
            return true;
        } else if (activeTab == Tab.SETTINGS && isDraggingScrollbar) {
            int trackY = boxY + 38;
            int trackH = 114;
            updateSettingsScrollFromMouseY((int) event.y(), trackY, trackH);
            return true;
        } else if (activeTab == Tab.PROFESSION && isDraggingScrollbar) {
            int trackY = boxY + 48;
            int trackH = 104;
            updateProfessionScrollFromMouseY((int) event.y(), trackY, trackH);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        isDraggingScrollbar = false;
        return super.mouseReleased(event);
    }

    private void updateScrollFromMouseY(int mouseY, int trackY, int trackH) {
        List<ShopItem> sortedItems = getSortedShopItems();
        int totalItems = sortedItems.size();
        int maxOffset = Math.max(0, totalItems - 4);
        if (maxOffset <= 0) return;

        int thumbH = Math.max(16, (trackH * 4) / Math.max(1, totalItems));
        float ratio = (float) (mouseY - trackY - (thumbH / 2)) / (trackH - thumbH);
        int newOffset = Math.clamp(Math.round(ratio * maxOffset), 0, maxOffset);

        if (newOffset != scrollOffset) {
            scrollOffset = newOffset;
            rebuildWidgets();
        }
    }

    private void updateSettingsScrollFromMouseY(int mouseY, int trackY, int trackH) {
        int totalItems = 8;
        int maxOffset = Math.max(0, totalItems - 4);
        if (maxOffset <= 0) return;

        int thumbH = Math.max(16, (trackH * 4) / totalItems);
        float ratio = (float) (mouseY - trackY - (thumbH / 2)) / (trackH - thumbH);
        int newOffset = Math.clamp(Math.round(ratio * maxOffset), 0, maxOffset);

        if (newOffset != settingsScrollOffset) {
            settingsScrollOffset = newOffset;
            rebuildWidgets();
        }
    }

    private void updateProfessionScrollFromMouseY(int mouseY, int trackY, int trackH) {
        int totalItems = 5;
        int maxOffset = Math.max(0, totalItems - 3);
        if (maxOffset <= 0) return;

        int thumbH = Math.max(16, (trackH * 3) / totalItems);
        float ratio = (float) (mouseY - trackY - (thumbH / 2)) / (trackH - thumbH);
        int newOffset = Math.clamp(Math.round(ratio * maxOffset), 0, maxOffset);

        if (newOffset != professionScrollOffset) {
            professionScrollOffset = newOffset;
            rebuildWidgets();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (activeTab == Tab.SHOP) {
            List<ShopItem> sortedItems = getSortedShopItems();
            int maxOffset = Math.max(0, sortedItems.size() - 4);
            if (verticalAmount < 0 && scrollOffset < maxOffset) {
                scrollOffset++;
                rebuildWidgets();
                return true;
            } else if (verticalAmount > 0 && scrollOffset > 0) {
                scrollOffset--;
                rebuildWidgets();
                return true;
            }
        } else if (activeTab == Tab.SETTINGS) {
            int maxOffset = Math.max(0, 8 - 4);
            if (verticalAmount < 0 && settingsScrollOffset < maxOffset) {
                settingsScrollOffset++;
                rebuildWidgets();
                return true;
            } else if (verticalAmount > 0 && settingsScrollOffset > 0) {
                settingsScrollOffset--;
                rebuildWidgets();
                return true;
            }
        } else if (activeTab == Tab.PROFESSION) {
            int maxOffset = Math.max(0, 5 - 3);
            if (verticalAmount < 0 && professionScrollOffset < maxOffset) {
                professionScrollOffset++;
                rebuildWidgets();
                return true;
            } else if (verticalAmount > 0 && professionScrollOffset > 0) {
                professionScrollOffset--;
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.searchBox != null && (this.searchBox.isFocused() || this.getFocused() == this.searchBox)) {
            if (event.key() == 256) {
                this.searchBox.setFocused(false);
                this.setFocused(null);
                return true;
            }
            this.searchBox.keyPressed(event);
            return true;
        }

        if (event.key() == 78) { // GLFW_KEY_N
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    public Tab getActiveTab() {
        return activeTab;
    }

    public void refreshUI() {
        this.rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
