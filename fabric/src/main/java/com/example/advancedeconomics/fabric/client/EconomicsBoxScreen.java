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
 * Advanced Economics Box Screen (v0.31).
 *
 * Tab Button Styling:
 * - Selected tab button renders in default bright Minecraft button style.
 * - Non-selected tab buttons render with a sleek dark background (0xFF141414) and 100% bright white text (0xFFFFFFFF).
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
    private boolean isDraggingScrollbar = false;

    private EditBox searchBox;
    private String searchQuery = "";

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

        // Header Tab Buttons
        addRenderableWidget(Button.builder(Component.literal("Shop"), btn -> switchTab(Tab.SHOP))
                .bounds(visualLeft, btnY, BTN_WIDTH, BTN_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.literal("Profession"), btn -> switchTab(Tab.PROFESSION))
                .bounds(visualLeft + BTN_WIDTH + BTN_GAP, btnY, BTN_WIDTH, BTN_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.literal("Settings"), btn -> switchTab(Tab.SETTINGS))
                .bounds(visualLeft + (BTN_WIDTH + BTN_GAP) * 2, btnY, BTN_WIDTH, BTN_HEIGHT).build());

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

                Button sellBtn = Button.builder(Component.literal("Sell " + sellStr), btn -> {
                    ClientPlayNetworking.send(new RequestSellPayload(shopItem.id()));
                }).bounds(boxX + 150, rowY + 8, 64, 18).build();
                sellBtn.active = hasItemInInventory;
                addRenderableWidget(sellBtn);

                addRenderableWidget(Button.builder(Component.literal("Buy " + buyStr), btn -> {
                    ClientPlayNetworking.send(new RequestBuyPayload(shopItem.id()));
                }).bounds(boxX + 218, rowY + 8, 66, 18).build());
            } else {
                String unlockStr = formatPrice(unlockPrice);

                addRenderableWidget(Button.builder(Component.literal("Unlock " + unlockStr), btn -> {
                    ClientPlayNetworking.send(new RequestUnlockPayload(shopItem.id()));
                }).bounds(boxX + 204, rowY + 8, 80, 18).build());
            }
        }
    }

    private void buildProfessionTabWidgets(int boxX, int boxY) {
        Profession[] selectable = new Profession[]{
                Profession.LUMBERJACK, Profession.MINER, Profession.FARMER, Profession.HUNTER, Profession.WEAPONSMITH
        };

        int maxOffset = Math.max(0, selectable.length - 3);
        professionScrollOffset = Math.clamp(professionScrollOffset, 0, maxOffset);

        // Sleek Thinner Scroll Buttons (12px wide x 10px high)
        addRenderableWidget(Button.builder(Component.literal("▲"), btn -> {
            if (professionScrollOffset > 0) {
                professionScrollOffset--;
                rebuildWidgets();
            }
        }).bounds(boxX + BOX_WIDTH - 21, boxY + 58, 12, 10).build());

        addRenderableWidget(Button.builder(Component.literal("▼"), btn -> {
            if (professionScrollOffset < maxOffset) {
                professionScrollOffset++;
                rebuildWidgets();
            }
        }).bounds(boxX + BOX_WIDTH - 21, boxY + 152, 12, 10).build());

        int startY = boxY + 58;
        int rowHeight = 35;
        String currentProfName = ClientEconomyState.getProfession();

        int visibleCount = Math.min(3, selectable.length - professionScrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            final Profession prof = selectable[professionScrollOffset + i];
            int rowY = startY + (i * rowHeight);

            boolean isCurrent = currentProfName.equalsIgnoreCase(prof.getDisplayName()) || currentProfName.equalsIgnoreCase(prof.name());

            if (!isCurrent) {
                addRenderableWidget(Button.builder(Component.literal("Select"), btn -> {
                    ClientPlayNetworking.send(new RequestSetProfessionPayload(prof.name()));
                    ClientEconomyState.setProfession(prof.getDisplayName());
                    rebuildWidgets();
                }).bounds(boxX + 224, rowY + 8, 58, 18).build());
            }
        }
    }

    private void buildSettingsTabWidgets(int boxX, int boxY) {
        int startY = boxY + 28;

        int sellM   = ClientEconomyState.getSellMultiplier();
        int buyM    = ClientEconomyState.getBuyMultiplier();
        int unlockM = ClientEconomyState.getUnlockMultiplier();

        int r1 = startY + 20;
        int r2 = r1 + 28;
        int r3 = r2 + 28;

        addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            sendUpdateSettings(Math.max(1, sellM - 1), buyM, unlockM);
        }).bounds(boxX + 225, r1 - 2, 22, 18).build());

        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            sendUpdateSettings(sellM + 1, buyM, unlockM);
        }).bounds(boxX + 252, r1 - 2, 22, 18).build());

        addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            sendUpdateSettings(sellM, Math.max(1, buyM - 1), unlockM);
        }).bounds(boxX + 225, r2 - 2, 22, 18).build());

        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            sendUpdateSettings(sellM, buyM + 1, unlockM);
        }).bounds(boxX + 252, r2 - 2, 22, 18).build());

        addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            sendUpdateSettings(sellM, buyM, Math.max(1, unlockM - 1));
        }).bounds(boxX + 225, r3 - 2, 22, 18).build());

        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            sendUpdateSettings(sellM, buyM, unlockM + 1);
        }).bounds(boxX + 252, r3 - 2, 22, 18).build());
    }

    private void sendUpdateSettings(int sellM, int buyM, int unlockM) {
        ClientPlayNetworking.send(new RequestUpdateSettingsPayload(sellM, buyM, unlockM));
        ClientEconomyState.setMultipliers(sellM, buyM, unlockM);
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
            graphics.fill(boxX + 5, boxY + 24, boxX + BOX_WIDTH - 26, boxY + 168, 0xFF0A0A0A);
            renderShopRows(graphics, boxX, boxY);
            renderDraggableScrollbar(graphics, boxX, boxY);
        } else if (activeTab == Tab.SETTINGS) {
            String tabTitle = "Economy Settings & Price Multipliers";
            graphics.centeredText(this.getFont(), Component.literal(tabTitle), this.width / 2, boxY + 6, 0xFFFFDD55);
            graphics.fill(boxX + 6, boxY + 18, boxX + BOX_WIDTH - 6, boxY + 19, 0xFF444444);
            renderSettingsTab(graphics, boxX, boxY);
        } else if (activeTab == Tab.PROFESSION) {
            String tabTitle = "Career Path & Professions";
            graphics.centeredText(this.getFont(), Component.literal(tabTitle), this.width / 2, boxY + 6, 0xFFFFDD55);
            graphics.fill(boxX + 6, boxY + 18, boxX + BOX_WIDTH - 6, boxY + 19, 0xFF444444);
            renderProfessionTab(graphics, boxX, boxY);
        }

        // Pass 4: Draw all interactive widgets & buttons on top!
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // Pass 5: Render dark background & 100% bright white text for non-selected tab buttons
        int btnY = boxY - BTN_HEIGHT - BTN_MARGIN;
        int visualLeft = boxX - BORDER;

        int shopX = visualLeft;
        int profX = visualLeft + BTN_WIDTH + BTN_GAP;
        int setX  = visualLeft + (BTN_WIDTH + BTN_GAP) * 2;

        if (activeTab != Tab.SHOP) {
            graphics.fill(shopX, btnY, shopX + BTN_WIDTH, btnY + BTN_HEIGHT, 0xFF141414);
            graphics.fill(shopX, btnY, shopX + BTN_WIDTH, btnY + 1, 0xFF3D3D3D);
            graphics.centeredText(this.getFont(), Component.literal("Shop"), shopX + BTN_WIDTH / 2, btnY + 5, 0xFFFFFFFF);
        }
        if (activeTab != Tab.PROFESSION) {
            graphics.fill(profX, btnY, profX + BTN_WIDTH, btnY + BTN_HEIGHT, 0xFF141414);
            graphics.fill(profX, btnY, profX + BTN_WIDTH, btnY + 1, 0xFF3D3D3D);
            graphics.centeredText(this.getFont(), Component.literal("Profession"), profX + BTN_WIDTH / 2, btnY + 5, 0xFFFFFFFF);
        }
        if (activeTab != Tab.SETTINGS) {
            graphics.fill(setX, btnY, setX + BTN_WIDTH, btnY + BTN_HEIGHT, 0xFF141414);
            graphics.fill(setX, btnY, setX + BTN_WIDTH, btnY + 1, 0xFF3D3D3D);
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
            graphics.fill(boxX + 6, rowY + 1, boxX + BOX_WIDTH - 28, rowY + rowHeight - 1, bgTint);
            graphics.fill(boxX + 6, rowY + rowHeight - 1, boxX + BOX_WIDTH - 28, rowY + rowHeight, 0xFF282828);

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
        int startY = boxY + 28;

        int r1 = startY + 20;
        int r2 = r1 + 28;
        int r3 = r2 + 28;

        graphics.fill(boxX + 10, r1 - 3, boxX + BOX_WIDTH - 10, r1 + 19, 0xFF181818);
        graphics.text(this.getFont(), Component.literal("Sell Multiplier:"), boxX + 16, r1 + 3, 0xFFFFFFFF, true);
        graphics.text(this.getFont(), Component.literal("§e" + ClientEconomyState.getSellMultiplier() + "x"), boxX + 180, r1 + 3, 0xFFFFDD55, true);

        graphics.fill(boxX + 10, r2 - 3, boxX + BOX_WIDTH - 10, r2 + 19, 0xFF181818);
        graphics.text(this.getFont(), Component.literal("Buy Multiplier:"), boxX + 16, r2 + 3, 0xFFFFFFFF, true);
        graphics.text(this.getFont(), Component.literal("§e" + ClientEconomyState.getBuyMultiplier() + "x"), boxX + 180, r2 + 3, 0xFFFFDD55, true);

        graphics.fill(boxX + 10, r3 - 3, boxX + BOX_WIDTH - 10, r3 + 19, 0xFF181818);
        graphics.text(this.getFont(), Component.literal("Unlock Multiplier:"), boxX + 16, r3 + 3, 0xFFFFFFFF, true);
        graphics.text(this.getFont(), Component.literal("§e" + ClientEconomyState.getUnlockMultiplier() + "x"), boxX + 180, r3 + 3, 0xFFFFDD55, true);

        graphics.text(this.getFont(), Component.literal("§7Multiplies item prices in Shop (e.g. 5x base price)."), boxX + 14, startY + 112, 0xFFAAAAAA, true);
    }

    private void renderProfessionTab(GuiGraphicsExtractor graphics, int boxX, int boxY) {
        String profession = ClientEconomyState.getProfession();
        int level = ClientEconomyState.getLevel();
        long xp = ClientEconomyState.getXp();
        long maxXp = ClientEconomyState.getMaxXp();

        String headerStr = "Active: §e" + profession + " §7(Level " + level + ")";
        graphics.text(this.getFont(), Component.literal(headerStr), boxX + 12, boxY + 22, 0xFFFFFFFF, true);

        int bonusPct = level * 2;
        graphics.text(this.getFont(), Component.literal("§a+" + bonusPct + "% Sell Bonus"), boxX + 218, boxY + 22, 0xFF55FF55, true);

        // XP Progress Bar
        int barX = boxX + 12;
        int barY = boxY + 35;
        int barW = 291;
        int barH = 14;

        graphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF444444);
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF0A1A0A);

        double ratio = Math.clamp((double) xp / (double) Math.max(1, maxXp), 0.0, 1.0);
        int fillW = (int) Math.round(barW * ratio);
        if (fillW > 0) {
            graphics.fill(barX, barY, barX + fillW, barY + barH, 0xFF22AA22);
            graphics.fill(barX, barY, barX + fillW, barY + 3, 0xFF55FF55);
        }

        String xpStr = "XP: " + xp + " / " + maxXp;
        graphics.centeredText(this.getFont(), Component.literal(xpStr), this.width / 2, barY + 3, 0xFFFFFFFF);

        Profession[] selectable = new Profession[]{
                Profession.LUMBERJACK, Profession.MINER, Profession.FARMER, Profession.HUNTER, Profession.WEAPONSMITH
        };

        graphics.fill(boxX + 10, boxY + 56, boxX + BOX_WIDTH - 26, boxY + 168, 0xFF0A0A0A);

        int maxOffset = Math.max(0, selectable.length - 3);
        professionScrollOffset = Math.clamp(professionScrollOffset, 0, maxOffset);

        int startY = boxY + 58;
        int rowHeight = 35;

        int visibleCount = Math.min(3, selectable.length - professionScrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            Profession prof = selectable[professionScrollOffset + i];
            int rowY = startY + (i * rowHeight);

            boolean isCurrent = profession.equalsIgnoreCase(prof.getDisplayName()) || profession.equalsIgnoreCase(prof.name());

            int bgTint = isCurrent ? 0xFF1E2E1E : 0xFF141414;
            graphics.fill(boxX + 12, rowY + 1, boxX + BOX_WIDTH - 28, rowY + rowHeight - 2, bgTint);
            graphics.fill(boxX + 12, rowY + rowHeight - 2, boxX + BOX_WIDTH - 28, rowY + rowHeight - 1, 0xFF2A2A2A);

            Item profItem = getProfessionItemIcon(prof);
            if (profItem != null && profItem != Items.AIR) {
                graphics.item(new ItemStack(profItem), boxX + 16, rowY + 9);
            }

            int nameColor = isCurrent ? 0xFF55FF55 : 0xFFFFFFFF;
            graphics.text(this.getFont(), Component.literal(prof.getDisplayName()), boxX + 36, rowY + 5, nameColor, true);
            graphics.text(this.getFont(), Component.literal("§7" + prof.getDescription()), boxX + 36, rowY + 17, 0xFFAAAAAA, true);

            if (isCurrent) {
                graphics.text(this.getFont(), Component.literal("§a✔ Active"), boxX + 232, rowY + 10, 0xFF55FF55, true);
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
        int trackY = boxY + 70;
        int trackW = 6;
        int trackH = 80;

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
        } else if (activeTab == Tab.PROFESSION) {
            int trackY = boxY + 70;
            int trackW = 6;
            int trackH = 80;

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
        } else if (activeTab == Tab.PROFESSION && isDraggingScrollbar) {
            int trackY = boxY + 70;
            int trackH = 80;
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
