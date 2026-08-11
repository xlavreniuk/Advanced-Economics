package com.example.advancedeconomics.fabric.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * Advanced Economics UI Screen (v0.6)
 * Layout:
 *   [  Shop  ] [  Profession  ]   <- buttons above container
 *   +---------------------------+
 *   |                           |  <- empty container
 *   +---------------------------+
 *
 * Press N or ESC to close.
 */
public class EconomicsBoxScreen extends Screen {

    // Layout constants
    private static final int BOX_WIDTH    = 260;
    private static final int BOX_HEIGHT   = 160;
    private static final int BTN_WIDTH    = 110;
    private static final int BTN_HEIGHT   = 20;
    private static final int BTN_GAP      = 10;  // gap between buttons
    private static final int BTN_MARGIN   = 6;   // gap between buttons and box top

    // Track which tab is active for future use
    private boolean shopActive = true;

    public EconomicsBoxScreen() {
        super(Component.literal("Advanced Economics"));
    }

    @Override
    protected void init() {
        super.init();

        int boxX = (this.width - BOX_WIDTH) / 2;
        int boxY = (this.height - BOX_HEIGHT) / 2;

        // Buttons sit just above the box
        int totalBtnWidth = BTN_WIDTH * 2 + BTN_GAP;
        int btnStartX = (this.width - totalBtnWidth) / 2;
        int btnY = boxY - BTN_HEIGHT - BTN_MARGIN;

        // Shop button
        addRenderableWidget(Button.builder(Component.literal("Shop"), btn -> {
            shopActive = true;
        }).bounds(btnStartX, btnY, BTN_WIDTH, BTN_HEIGHT).build());

        // Profession button
        addRenderableWidget(Button.builder(Component.literal("Profession"), btn -> {
            shopActive = false;
        }).bounds(btnStartX + BTN_WIDTH + BTN_GAP, btnY, BTN_WIDTH, BTN_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Translucent full-screen dim (no extractBackground — causes double-blur crash in 26.2)
        graphics.fill(0, 0, this.width, this.height, 0xA0000000);

        int boxX = (this.width - BOX_WIDTH) / 2;
        int boxY = (this.height - BOX_HEIGHT) / 2;

        // Container border + empty dark fill
        graphics.fill(boxX - 2, boxY - 2, boxX + BOX_WIDTH + 2, boxY + BOX_HEIGHT + 2, 0xFF888888);
        graphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, 0xF0101010);

        // Render buttons and other widgets on top
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
