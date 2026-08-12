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
 *
 * Shop Tab Logic:
 * - Sell button is AVAILABLE EVERYWHERE for all items!
 * - Buy button becomes available once an item is obtained or unlocked.
 * - Unlock button (10x cost) unlocks the Buy button for items not yet obtained.
 */
public class EconomicsBoxScreen extends Screen {

    public enum Tab {
        SHOP,
        PROFESSION,
        SETTINGS
    }

    private static final int BOX_WIDTH   = 285;
    private static final int BOX_HEIGHT  = 175;
    private static final int BORDER      = 2;
    private static final int BTN_WIDTH   = 68;
    private static final int BTN_HEIGHT  = 18;
    private static final int BTN_GAP     = 3;
    private static final int BTN_MARGIN  = 4;
    private static final int CLOSE_WIDTH = 18;

    private Tab activeTab = Tab.SHOP;
    private int scrollOffset = 0;

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
        int rowHeight = 33;

        // Scroll Up / Down buttons
        addRenderableWidget(Button.builder(Component.literal("▲"), btn -> {
            if (scrollOffset > 0) {
                scrollOffset--;
                rebuildWidgets();
            }
        }).bounds(boxX + BOX_WIDTH - 22, startY, 18, 16).build());

        addRenderableWidget(Button.builder(Component.literal("▼"), btn -> {
            if (scrollOffset < maxOffset) {
                scrollOffset++;
                rebuildWidgets();
            }
        }).bounds(boxX + BOX_WIDTH - 22, startY + 124, 18, 16).build());

        // Build 4 visible item rows
        int visibleCount = Math.min(4, items.size() - scrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            final ShopItem shopItem = items.get(scrollOffset + i);
            int rowY = startY + (i * rowHeight);

            boolean isUnlocked = ClientEconomyState.isUnlocked(shopItem.id());
            long sellPrice   = shopItem.basePrice() * ClientEconomyState.getSellMultiplier();
            long buyPrice    = shopItem.basePrice() * ClientEconomyState.getBuyMultiplier();
            long unlockPrice = shopItem.basePrice() * ClientEconomyState.getUnlockMultiplier();

            // 1. SELL BUTTON — AVAILABLE EVERYWHERE for all items!
            addRenderableWidget(Button.builder(Component.literal("Sell $" + sellPrice), btn -> {
                ClientPlayNetworking.send(new RequestSellPayload(shopItem.id()));
            }).bounds(boxX + 138, rowY + 7, 56, 18).build());

            // 2. BUY / UNLOCK BUTTON
            if (isUnlocked) {
                // Buy button available when item is unlocked / obtained
                addRenderableWidget(Button.builder(Component.literal("Buy $" + buyPrice), btn -> {
                    ClientPlayNetworking.send(new RequestBuyPayload(shopItem.id()));
                }).bounds(boxX + 198, rowY + 7, 58, 18).build());
            } else {
                // Unlock button (10x price) to unlock buying
                addRenderableWidget(Button.builder(Component.literal("Unlock $" + unlockPrice), btn -> {
                    ClientPlayNetworking.send(new RequestUnlockPayload(shopItem.id()));
                }).bounds(boxX + 198, rowY + 7, 66, 18).build());
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
        }).bounds(boxX + 215, startY, 20, 18).build());

        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            sendUpdateSettings(sellM + 1, buyM, unlockM);
        }).bounds(boxX + 240, startY, 20, 18).build());

        // Buy Multiplier [-] [+]
        addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            sendUpdateSettings(sellM, Math.max(1, buyM - 1), unlockM);
        }).bounds(boxX + 215, startY + 30, 20, 18).build());

        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            sendUpdateSettings(sellM, buyM + 1, unlockM);
        }).bounds(boxX + 240, startY + 30, 20, 18).build());

        // Unlock Multiplier [-] [+]
        addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            sendUpdateSettings(sellM, buyM, Math.max(1, unlockM - 1));
        }).bounds(boxX + 215, startY + 60, 20, 18).build());

        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            sendUpdateSettings(sellM, buyM, unlockM + 1);
        }).bounds(boxX + 240, startY + 60, 20, 18).build());
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
        graphics.centeredText(this.getFont(), Component.literal(tabTitle), this.width / 2, boxY + 7, 0xFFDD55);
        graphics.fill(boxX + 8, boxY + 18, boxX + BOX_WIDTH - 8, boxY + 19, 0xFF444444);

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
        int rowHeight = 33;

        int visibleCount = Math.min(4, items.size() - scrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            ShopItem shopItem = items.get(scrollOffset + i);
            int rowY = startY + (i * rowHeight);

            // Row separator line
            graphics.fill(boxX + 6, rowY + rowHeight - 1, boxX + BOX_WIDTH - 26, rowY + rowHeight, 0xFF252525);

            // Render Item Stack Icon
            Item mcItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", shopItem.id()));
            if (mcItem != null && mcItem != Items.AIR) {
                graphics.item(new ItemStack(mcItem), boxX + 8, rowY + 7);
            }

            // Render Item Name (Component.literal with drop shadow enabled)
            graphics.text(this.getFont(), Component.literal(shopItem.displayName()), boxX + 28, rowY + 5, 0xFFFFFF, true);

            // Render Status text (Unlocked vs Locked)
            boolean isUnlocked = ClientEconomyState.isUnlocked(shopItem.id());
            String statusStr = isUnlocked ? "§aUnlocked" : "§cLocked";
            graphics.text(this.getFont(), Component.literal(statusStr), boxX + 28, rowY + 17, 0xAAAAAA, true);
        }
    }

    private void renderSettingsTab(GuiGraphicsExtractor graphics, int boxX, int boxY) {
        int startY = boxY + 38;

        graphics.text(this.getFont(), Component.literal("Sell Price Multiplier (Base × Sell):"), boxX + 12, startY + 4, 0xFFFFFF, true);
        graphics.text(this.getFont(), Component.literal(ClientEconomyState.getSellMultiplier() + "x"), boxX + 265, startY + 4, 0xFFDD55, true);

        graphics.text(this.getFont(), Component.literal("Buy Price Multiplier (Base × Buy):"), boxX + 12, startY + 34, 0xFFFFFF, true);
        graphics.text(this.getFont(), Component.literal(ClientEconomyState.getBuyMultiplier() + "x"), boxX + 265, startY + 34, 0xFFDD55, true);

        graphics.text(this.getFont(), Component.literal("Unlock Multiplier (Base × Unlock):"), boxX + 12, startY + 64, 0xFFFFFF, true);
        graphics.text(this.getFont(), Component.literal(ClientEconomyState.getUnlockMultiplier() + "x"), boxX + 265, startY + 64, 0xFFDD55, true);
    }

    private void renderProfessionTab(GuiGraphicsExtractor graphics, int boxX, int boxY) {
        String profession = ClientEconomyState.getProfession();
        graphics.centeredText(this.getFont(), Component.literal("Current Profession: " + profession), this.width / 2, boxY + 45, 0xFFFFFF);
        graphics.centeredText(this.getFont(), Component.literal("Select a career path to unlock economic perks."), this.width / 2, boxY + 70, 0xAAAAAA);
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
