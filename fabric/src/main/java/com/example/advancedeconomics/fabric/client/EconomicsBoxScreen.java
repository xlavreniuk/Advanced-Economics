package com.example.advancedeconomics.fabric.client;

import com.example.advancedeconomics.fabric.network.*;
import com.example.advancedeconomics.shop.ShopItem;
import com.example.advancedeconomics.shop.ShopTable;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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

/**
 * Advanced Economics Box Screen (v0.10).
 *
 * Rules:
 * - Locked Item: ONLY the [Unlock $X] button is visible.
 * - Unlocked Item: BOTH [Sell $X] and [Buy $X] buttons appear.
 * - Unlocks are FOREVER once obtained in inventory or unlocked via 10x cost.
 * - Interactive Draggable Web-Style Scrollbar on the right.
 */
public class EconomicsBoxScreen extends Screen {

    public enum Tab {
        SHOP,
        PROFESSION,
        SETTINGS
    }

    private static final int BOX_WIDTH   = 295;
    private static final int BOX_HEIGHT  = 175;
    private static final int BORDER      = 2;
    private static final int BTN_WIDTH   = 68;
    private static final int BTN_HEIGHT  = 18;
    private static final int BTN_GAP     = 3;
    private static final int BTN_MARGIN  = 4;
    private static final int CLOSE_WIDTH = 18;

    private Tab activeTab = Tab.SHOP;
    private int scrollOffset = 0;
    private boolean isDraggingScrollbar = false;

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

        for (ShopItem item : all) {
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
        List<ShopItem> sortedItems = getSortedShopItems();
        int maxOffset = Math.max(0, sortedItems.size() - 4);
        scrollOffset = Math.clamp(scrollOffset, 0, maxOffset);

        int startY = boxY + 23;
        int rowHeight = 33;

        int visibleCount = Math.min(4, sortedItems.size() - scrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            final ShopItem shopItem = sortedItems.get(scrollOffset + i);
            int rowY = startY + (i * rowHeight);

            boolean isUnlocked = ClientEconomyState.isUnlocked(shopItem.id());
            long sellPrice   = shopItem.basePrice() * ClientEconomyState.getSellMultiplier();
            long buyPrice    = shopItem.basePrice() * ClientEconomyState.getBuyMultiplier();
            long unlockPrice = shopItem.basePrice() * ClientEconomyState.getUnlockMultiplier();

            if (isUnlocked) {
                // UNLOCKED: BOTH [Sell $X] and [Buy $X] buttons appear
                addRenderableWidget(Button.builder(Component.literal("Sell $" + sellPrice), btn -> {
                    ClientPlayNetworking.send(new RequestSellPayload(shopItem.id()));
                }).bounds(boxX + 132, rowY + 7, 56, 18).build());

                addRenderableWidget(Button.builder(Component.literal("Buy $" + buyPrice), btn -> {
                    ClientPlayNetworking.send(new RequestBuyPayload(shopItem.id()));
                }).bounds(boxX + 192, rowY + 7, 58, 18).build());
            } else {
                // LOCKED: ONLY [Unlock $X] button is visible
                addRenderableWidget(Button.builder(Component.literal("Unlock $" + unlockPrice), btn -> {
                    ClientPlayNetworking.send(new RequestUnlockPayload(shopItem.id()));
                }).bounds(boxX + 176, rowY + 7, 74, 18).build());
            }
        }
    }

    private void buildSettingsTabWidgets(int boxX, int boxY) {
        int startY = boxY + 36;

        int sellM   = ClientEconomyState.getSellMultiplier();
        int buyM    = ClientEconomyState.getBuyMultiplier();
        int unlockM = ClientEconomyState.getUnlockMultiplier();

        addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            sendUpdateSettings(Math.max(1, sellM - 1), buyM, unlockM);
        }).bounds(boxX + 225, startY, 20, 18).build());

        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            sendUpdateSettings(sellM + 1, buyM, unlockM);
        }).bounds(boxX + 250, startY, 20, 18).build());

        addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            sendUpdateSettings(sellM, Math.max(1, buyM - 1), unlockM);
        }).bounds(boxX + 225, startY + 30, 20, 18).build());

        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            sendUpdateSettings(sellM, buyM + 1, unlockM);
        }).bounds(boxX + 250, startY + 30, 20, 18).build());

        addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            sendUpdateSettings(sellM, buyM, Math.max(1, unlockM - 1));
        }).bounds(boxX + 225, startY + 60, 20, 18).build());

        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            sendUpdateSettings(sellM, buyM, unlockM + 1);
        }).bounds(boxX + 250, startY + 60, 20, 18).build());
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

        // Outer container border
        graphics.fill(boxX - BORDER, boxY - BORDER,
                      boxX + BOX_WIDTH + BORDER, boxY + BOX_HEIGHT + BORDER, 0xFF777777);
        graphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, 0xF0101010);

        // Header Title
        String tabTitle = (activeTab == Tab.SHOP) ? "Marketplace Shop" :
                          (activeTab == Tab.SETTINGS) ? "Economy Settings & Price Multipliers" : "Professions & Careers";
        graphics.centeredText(this.getFont(), Component.literal(tabTitle), this.width / 2, boxY + 6, 0xFFDD55);
        graphics.fill(boxX + 6, boxY + 18, boxX + BOX_WIDTH - 6, boxY + 19, 0xFF444444);

        if (activeTab == Tab.SHOP) {
            // Draw inner scroll container viewport
            graphics.fill(boxX + 5, boxY + 22, boxX + BOX_WIDTH - 20, boxY + BOX_HEIGHT - 6, 0xFF0A0A0A);
            renderShopRows(graphics, boxX, boxY);
            renderDraggableScrollbar(graphics, boxX, boxY);
        } else if (activeTab == Tab.SETTINGS) {
            renderSettingsTab(graphics, boxX, boxY);
        } else if (activeTab == Tab.PROFESSION) {
            renderProfessionTab(graphics, boxX, boxY);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void renderShopRows(GuiGraphicsExtractor graphics, int boxX, int boxY) {
        List<ShopItem> sortedItems = getSortedShopItems();
        int startY = boxY + 23;
        int rowHeight = 33;

        int visibleCount = Math.min(4, sortedItems.size() - scrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            ShopItem shopItem = sortedItems.get(scrollOffset + i);
            int rowY = startY + (i * rowHeight);
            boolean isUnlocked = ClientEconomyState.isUnlocked(shopItem.id());

            // Tint
            int bgTint = isUnlocked ? 0xFF181818 : 0xFF101010;
            graphics.fill(boxX + 6, rowY + 1, boxX + BOX_WIDTH - 22, rowY + rowHeight - 2, bgTint);
            graphics.fill(boxX + 6, rowY + rowHeight - 2, boxX + BOX_WIDTH - 22, rowY + rowHeight - 1, 0xFF282828);

            // Item Icon
            Item mcItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", shopItem.id()));
            if (mcItem != null && mcItem != Items.AIR) {
                graphics.item(new ItemStack(mcItem), boxX + 8, rowY + 7);
            }

            // Unlocked items = Bright White (0xFFFFFF); Locked items = Dimmed Grey (0x777777)
            int nameColor = isUnlocked ? 0xFFFFFF : 0x777777;
            graphics.text(this.getFont(), Component.literal(shopItem.displayName()), boxX + 28, rowY + 5, nameColor, true);

            // Status indicator
            String statusStr = isUnlocked ? "§a✔ Unlocked" : "§7🔒 Locked";
            graphics.text(this.getFont(), Component.literal(statusStr), boxX + 28, rowY + 17, 0xAAAAAA, true);
        }
    }

    /**
     * Render Web-style Draggable Scrollbar Track & Thumb.
     */
    private void renderDraggableScrollbar(GuiGraphicsExtractor graphics, int boxX, int boxY) {
        List<ShopItem> sortedItems = getSortedShopItems();
        int totalItems = sortedItems.size();
        int maxOffset = Math.max(0, totalItems - 4);
        if (maxOffset <= 0) return;

        int trackX = boxX + BOX_WIDTH - 18;
        int trackY = boxY + 22;
        int trackW = 12;
        int trackH = BOX_HEIGHT - 28;

        // Background track
        graphics.fill(trackX, trackY, trackX + trackW, trackY + trackH, 0xFF151515);

        // Thumb height & position
        int thumbH = Math.max(20, (trackH * 4) / totalItems);
        int thumbY = trackY + ((trackH - thumbH) * scrollOffset) / maxOffset;

        int thumbColor = isDraggingScrollbar ? 0xFF999999 : 0xFF555555;

        // Outer thumb border & fill
        graphics.fill(trackX + 1, thumbY, trackX + trackW - 1, thumbY + thumbH, 0xFF777777);
        graphics.fill(trackX + 2, thumbY + 1, trackX + trackW - 2, thumbY + thumbH - 1, thumbColor);
    }

    private void renderSettingsTab(GuiGraphicsExtractor graphics, int boxX, int boxY) {
        int startY = boxY + 38;

        graphics.text(this.getFont(), Component.literal("Sell Price Multiplier (Base × Sell):"), boxX + 12, startY + 4, 0xFFFFFF, true);
        graphics.text(this.getFont(), Component.literal(ClientEconomyState.getSellMultiplier() + "x"), boxX + 275, startY + 4, 0xFFDD55, true);

        graphics.text(this.getFont(), Component.literal("Buy Price Multiplier (Base × Buy):"), boxX + 12, startY + 34, 0xFFFFFF, true);
        graphics.text(this.getFont(), Component.literal(ClientEconomyState.getBuyMultiplier() + "x"), boxX + 275, startY + 34, 0xFFDD55, true);

        graphics.text(this.getFont(), Component.literal("Unlock Multiplier (Base × Unlock):"), boxX + 12, startY + 64, 0xFFFFFF, true);
        graphics.text(this.getFont(), Component.literal(ClientEconomyState.getUnlockMultiplier() + "x"), boxX + 275, startY + 64, 0xFFDD55, true);
    }

    private void renderProfessionTab(GuiGraphicsExtractor graphics, int boxX, int boxY) {
        String profession = ClientEconomyState.getProfession();
        graphics.centeredText(this.getFont(), Component.literal("Current Profession: " + profession), this.width / 2, boxY + 45, 0xFFFFFF);
        graphics.centeredText(this.getFont(), Component.literal("Select a career path to unlock economic perks."), this.width / 2, boxY + 70, 0xAAAAAA);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (activeTab == Tab.SHOP) {
            int boxX = (this.width  - BOX_WIDTH)  / 2;
            int boxY = (this.height - BOX_HEIGHT) / 2;

            int trackX = boxX + BOX_WIDTH - 18;
            int trackY = boxY + 22;
            int trackW = 12;
            int trackH = BOX_HEIGHT - 28;

            double mx = event.x();
            double my = event.y();

            if (mx >= trackX && mx <= trackX + trackW && my >= trackY && my <= trackY + trackH) {
                isDraggingScrollbar = true;
                updateScrollFromMouseY((int) my, trackY, trackH);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (activeTab == Tab.SHOP && isDraggingScrollbar) {
            int boxY = (this.height - BOX_HEIGHT) / 2;
            int trackY = boxY + 22;
            int trackH = BOX_HEIGHT - 28;
            updateScrollFromMouseY((int) event.y(), trackY, trackH);
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

        int thumbH = Math.max(20, (trackH * 4) / totalItems);
        float ratio = (float) (mouseY - trackY - (thumbH / 2)) / (trackH - thumbH);
        int newOffset = Math.clamp(Math.round(ratio * maxOffset), 0, maxOffset);

        if (newOffset != scrollOffset) {
            scrollOffset = newOffset;
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
