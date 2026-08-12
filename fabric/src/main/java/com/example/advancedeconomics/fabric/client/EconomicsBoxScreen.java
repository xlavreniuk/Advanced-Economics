package com.example.advancedeconomics.fabric.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * Advanced Economics UI Screen (v0.10).
 * Clean container GUI with interactive Shop & Profession tabs.
 */
public class EconomicsBoxScreen extends Screen {

    public enum Tab {
        SHOP,
        PROFESSION
    }

    private static final int BOX_WIDTH   = 260;
    private static final int BOX_HEIGHT  = 160;
    private static final int BORDER      = 2;
    private static final int BTN_WIDTH   = 80;
    private static final int BTN_HEIGHT  = 20;
    private static final int BTN_GAP     = 4;
    private static final int BTN_MARGIN  = 4;
    private static final int CLOSE_WIDTH = 20;

    private Tab activeTab = Tab.SHOP;

    public EconomicsBoxScreen() {
        this(Tab.SHOP);
    }

    public EconomicsBoxScreen(Tab initialTab) {
        super(Component.literal("Advanced Economics"));
        if (initialTab != null) {
            this.activeTab = initialTab;
        }
    }

    public Tab getActiveTab() {
        return activeTab;
    }

    public void setActiveTab(Tab tab) {
        if (tab != null) {
            this.activeTab = tab;
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

        // [Shop] tab button
        addRenderableWidget(Button.builder(Component.literal("Shop"), btn -> {
            this.activeTab = Tab.SHOP;
        }).bounds(visualLeft, btnY, BTN_WIDTH, BTN_HEIGHT).build());

        // [Profession] tab button
        addRenderableWidget(Button.builder(Component.literal("Profession"), btn -> {
            this.activeTab = Tab.PROFESSION;
        }).bounds(visualLeft + BTN_WIDTH + BTN_GAP, btnY, BTN_WIDTH, BTN_HEIGHT).build());

        // [✕] Close button
        addRenderableWidget(Button.builder(Component.literal("✕"), btn -> this.onClose())
                .bounds(visualRight - CLOSE_WIDTH, btnY, CLOSE_WIDTH, BTN_HEIGHT)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Translucent background
        graphics.fill(0, 0, this.width, this.height, 0xA0000000);

        int boxX = (this.width  - BOX_WIDTH)  / 2;
        int boxY = (this.height - BOX_HEIGHT) / 2;

        // Border + container box
        graphics.fill(boxX - BORDER, boxY - BORDER,
                      boxX + BOX_WIDTH + BORDER, boxY + BOX_HEIGHT + BORDER, 0xFF888888);
        graphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, 0xF0101010);

        // Display tab title inside container
        String tabTitle = (activeTab == Tab.SHOP) ? "Marketplace Shop" : "Professions & Careers";
        graphics.centeredText(this.getFont(), tabTitle, this.width / 2, boxY + 12, 0xFFDD55);
        graphics.fill(boxX + 10, boxY + 26, boxX + BOX_WIDTH - 10, boxY + 27, 0xFF555555);

        // Render buttons on top
        super.extractRenderState(graphics, mouseX, mouseY, delta);
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
