package com.example.advancedeconomics.fabric.client;

import com.example.advancedeconomics.fabric.network.*;
import com.example.advancedeconomics.shop.ShopItem;
import com.example.advancedeconomics.shop.ShopTable;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Advanced Economics Box Screen (v0.10).
 * Features 3 Tabs: SHOP, PROFESSION, SETTINGS.
 *
 * SHOP Tab:
 * - Scrollable column of rows (20 common Minecraft items)
 * - Item Stack icon, item name, prices
 * - Interactive Buy, Sell, and Unlock buttons
 *
 * SETTINGS Tab:
 * - Interactive controls for 1x Sell, 5x Buy, and 10x Unlock price multipliers
 */
public class EconomicsBoxScreen extends Screen {

    public enum Tab {
        SHOP,
        PROFESSION,
        SETTINGS
    }

    private static final int BOX_WIDTH   = 280;
    private static final int BOX_HEIGHT  = 170;
    private static final int BORDER      = 2;
    private static final int BTN_WIDTH   = 68;
    private static final int BTN_HEIGHT  = 18;
    private static final int BTN_GAP     = 3;
    private static final int BTN_MARGIN  = 4;
    private static final int CLOSE_WIDTH = 18;

    private Tab activeTab = Tab.SHOP;
    private int scrollOffset = 0; // Scroll position for Shop rows (0..16)

    public EconomicsBoxScreen() {
        this(Tab.SHOP);
    }

    public EconomicsBoxScreen(Tab initialTab) {
        super(Component.literal("Advanced Economics"));
        if (initialTab != null) {
            this.activeTab = initialTab;
        }
    }

    @Override
    protected void init() {
        super.init();

        int boxX  = (this.width  - BOX_WIDTH)  / 2;
        int boxY  = (this.height - BOX_HEIGHT) / 2;
        int btnY  = boxY - BTN_HEIGHT - BTN_MARGIN;

        int visualLeft  = boxX - BORDER;
        int visualRight = boxX + BOX_WIDTH + BORDER;

        // [Shop] Tab Button
        addRenderableWidget(Button.builder(Component.literal("Shop"), btn -> switchTab(Tab.SHOP))
                .bounds(visualLeft, btnY, BTN_WIDTH, BTN_HEIGHT).build());

        // [Profession] Tab Button
        addRenderableWidget(Button.builder(Component.literal("Profession"), btn -> switchTab(Tab.PROFESSION))
                .bounds(visualLeft + BTN_WIDTH + BTN_GAP, btnY, BTN_WIDTH, BTN_HEIGHT).build());

        // [Settings] Tab Button
        addRenderableWidget(Button.builder(Component.literal("Settings"), btn -> switchTab(Tab.SETTINGS))
                .bounds(visualLeft + (BTN_WIDTH + BTN_GAP) * 2, btnY, BTN_WIDTH, BTN_HEIGHT).build());

        // [✕] Close Button
        addRenderableWidget(Button.builder(Component.literal("✕"), btn -> this.onClose())
                .bounds(visualRight - CLOSE_WIDTH, btnY, CLOSE_WIDTH, BTN_HEIGHT)
                .build());

        // Build widgets for active tab
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
        }
    }

    private void buildShopTabWidgets(int boxX, int boxY) {
        List<ShopItem> items = ShopTable.getItems();
        int maxOffset = Math.max(0, items.size() - 4);
        scrollOffset = Math.min(scrollOffset, maxOffset);

        int startY = boxY + 22;
        int rowHeight = 32;

        // Scroll Up / Scroll Down buttons
        addRenderableWidget(Button.builder(Component.literal("▲"), btn -> {
            if (scrollOffset > 0) {
                scrollOffset--;
                rebuildWidgets();
            }
        }).bounds(boxX + BOX_WIDTH - 24, startY, 20, 16).build());

        addRenderableWidget(Button.builder(Component.literal("▼"), btn -> {
            if (scrollOffset < maxOffset) {
                scrollOffset++;
                rebuildWidgets();
            }
        }).bounds(boxX + BOX_WIDTH - 24, startY + 120, 20, 16).build());

        // Build up to 4 visible rows
        int visibleCount = Math.min(4, items.size() - scrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            final ShopItem shopItem = items.get(scrollOffset + i);
            int rowY = startY + (i * rowHeight);

            boolean isUnlocked = ClientEconomyState.isUnlocked(shopItem.id());
            long sellPrice   = shopItem.basePrice() * ClientEconomyState.getSellMultiplier();
            long buyPrice    = shopItem.basePrice() * ClientEconomyState.getBuyMultiplier();
            long unlockPrice = shopItem.basePrice() * ClientEconomyState.getUnlockMultiplier();

            if (isUnlocked) {
                // [Buy] button
                addRenderableWidget(Button.builder(Component.literal("Buy $" + buyPrice), btn -> {
                    ClientPlayNetworking.send(new RequestBuyPayload(shopItem.id()));
                }).bounds(boxX + 130, rowY + 6, 56, 18).build());

                // [Sell] button
                addRenderableWidget(Button.builder(Component.literal("Sell $" + sellPrice), btn -> {
                    ClientPlayNetworking.send(new RequestSellPayload(shopItem.id()));
                }).bounds(boxX + 190, rowY + 6, 58, 18).build());
            } else {
                // [Unlock 10x] button
                addRenderableWidget(Button.builder(Component.literal("Unlock $" + unlockPrice), btn -> {
                    ClientPlayNetworking.send(new RequestUnlockPayload(shopItem.id()));
                }).bounds(boxX + 130, rowY + 6, 118, 18).build());
            }
        }
    }

    private void buildSettingsTabWidgets(int boxX, int boxY) {
        int startY = boxY + 36;

        int sellM   = ClientEconomyState.getSellMultiplier();
        int buyM    = ClientEconomyState.getBuyMultiplier();
        int unlockM = ClientEconomyState.getUnlockMultiplier();

        // Sell Multiplier [-] [+]
        addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            sendUpdateSettings(Math.max(1, sellM - 1), buyM, unlockM);
        }).bounds(boxX + 175, startY, 20, 18).build());

        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            sendUpdateSettings(sellM + 1, buyM, unlockM);
        }).bounds(boxX + 200, startY, 20, 18).build());

        // Buy Multiplier [-] [+]
        addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            sendUpdateSettings(sellM, Math.max(1, buyM - 1), unlockM);
        }).bounds(boxX + 175, startY + 30, 20, 18).build());

        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            sendUpdateSettings(sellM, buyM + 1, unlockM);
        }).bounds(boxX + 200, startY + 30, 20, 18).build());

        // Unlock Multiplier [-] [+]
        addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            sendUpdateSettings(sellM, buyM, Math.max(1, unlockM - 1));
        }).bounds(boxX + 175, startY + 60, 20, 18).build());

        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            sendUpdateSettings(sellM, buyM, unlockM + 1);
        }).bounds(boxX + 200, startY + 60, 20, 18).build());
    }

    private void sendUpdateSettings(int sellM, int buyM, int unlockM) {
        ClientPlayNetworking.send(new RequestUpdateSettingsPayload(sellM, buyM, unlockM));
        ClientEconomyState.setMultipliers(sellM, buyM, unlockM);
        rebuildWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Translucent background
        graphics.fill(0, 0, this.width, this.height, 0xA0000000);

        int boxX = (this.width  - BOX_WIDTH)  / 2;
        int boxY = (this.height - BOX_HEIGHT) / 2;

        // Container border + dark fill
        graphics.fill(boxX - BORDER, boxY - BORDER,
                      boxX + BOX_WIDTH + BORDER, boxY + BOX_HEIGHT + BORDER, 0xFF888888);
        graphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, 0xF0101010);

        // Header Title
        String tabTitle = (activeTab == Tab.SHOP) ? "Marketplace Shop" :
                          (activeTab == Tab.SETTINGS) ? "Economy Settings & Price Multipliers" : "Professions & Careers";
        graphics.centeredText(this.getFont(), tabTitle, this.width / 2, boxY + 8, 0xFFDD55);
        graphics.fill(boxX + 8, boxY + 19, boxX + BOX_WIDTH - 8, boxY + 20, 0xFF444444);

        if (activeTab == Tab.SHOP) {
            renderShopRows(graphics, boxX, boxY);
        } else if (activeTab == Tab.SETTINGS) {
            renderSettingsTab(graphics, boxX, boxY);
        } else if (activeTab == Tab.PROFESSION) {
            renderProfessionTab(graphics, boxX, boxY);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void renderShopRows(GuiGraphicsExtractor graphics, int boxX, int boxY) {
        List<ShopItem> items = ShopTable.getItems();
        int startY = boxY + 22;
        int rowHeight = 32;

        int visibleCount = Math.min(4, items.size() - scrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            ShopItem shopItem = items.get(scrollOffset + i);
            int rowY = startY + (i * rowHeight);

            // Row separator
            graphics.fill(boxX + 6, rowY + rowHeight - 1, boxX + BOX_WIDTH - 30, rowY + rowHeight, 0xFF222222);

            // Render Item Stack Icon
            Item mcItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", shopItem.id()));
            if (mcItem != null && mcItem != Items.AIR) {
                graphics.item(new ItemStack(mcItem), boxX + 10, rowY + 6);
            }

            // Render Item Name
            graphics.text(this.getFont(), shopItem.displayName(), boxX + 32, rowY + 4, 0xFFFFFF);

            // Render Status & Base Price info
            boolean isUnlocked = ClientEconomyState.isUnlocked(shopItem.id());
            if (isUnlocked) {
                graphics.text(this.getFont(), "§aUnlocked §7(Base: $" + shopItem.basePrice() + ")", boxX + 32, rowY + 16, 0xAAAAAA);
            } else {
                graphics.text(this.getFont(), "§cLocked §7(Base: $" + shopItem.basePrice() + ")", boxX + 32, rowY + 16, 0xAAAAAA);
            }
        }
    }

    private void renderSettingsTab(GuiGraphicsExtractor graphics, int boxX, int boxY) {
        int startY = boxY + 38;

        graphics.text(this.getFont(), "Sell Price Multiplier (Base × Sell):", boxX + 12, startY + 4, 0xFFFFFF);
        graphics.text(this.getFont(), "§e" + ClientEconomyState.getSellMultiplier() + "x", boxX + 226, startY + 4, 0xFFDD55);

        graphics.text(this.getFont(), "Buy Price Multiplier (Base × Buy):", boxX + 12, startY + 34, 0xFFFFFF);
        graphics.text(this.getFont(), "§e" + ClientEconomyState.getBuyMultiplier() + "x", boxX + 226, startY + 34, 0xFFDD55);

        graphics.text(this.getFont(), "Unlock Multiplier (Base × Unlock):", boxX + 12, startY + 64, 0xFFFFFF);
        graphics.text(this.getFont(), "§e" + ClientEconomyState.getUnlockMultiplier() + "x", boxX + 226, startY + 64, 0xFFDD55);
    }

    private void renderProfessionTab(GuiGraphicsExtractor graphics, int boxX, int boxY) {
        String profession = ClientEconomyState.getProfession();
        graphics.centeredText(this.getFont(), "Current Profession: §e" + profession, this.width / 2, boxY + 45, 0xFFFFFF);
        graphics.centeredText(this.getFont(), "§7Select a career path to unlock economic perks.", this.width / 2, boxY + 70, 0xAAAAAA);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (activeTab == Tab.SHOP) {
            List<ShopItem> items = ShopTable.getItems();
            int maxOffset = Math.max(0, items.size() - 4);
            if (verticalAmount < 0 && scrollOffset < maxOffset) {
                scrollOffset++;
                rebuildWidgets();
                return true;
            } else if (verticalAmount > 0 && scrollOffset > 0) {
                scrollOffset--;
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 78) { // GLFW_KEY_N
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
