package com.example.advancedeconomics.fabric.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * Advanced Economics UI Screen (v0.8)
 *
 * Button row is flush with the visual box border (border is 2px outside boxX).
 *
 *  [Shop] [Profession]                 [✕]   <- all flush with visual border edges
 *  ┌────────────────────────────────────┐
 *  │          (empty container)         │
 *  └────────────────────────────────────┘
 */
public class EconomicsBoxScreen extends Screen {

    private static final int BOX_WIDTH   = 260;
    private static final int BOX_HEIGHT  = 160;
    private static final int BORDER      = 2;   // border drawn outside boxX
    private static final int BTN_WIDTH   = 80;
    private static final int BTN_HEIGHT  = 20;
    private static final int BTN_GAP     = 4;
    private static final int BTN_MARGIN  = 4;   // gap between button row bottom and box top
    private static final int CLOSE_WIDTH = 20;

    public EconomicsBoxScreen() {
        super(Component.literal("Advanced Economics"));
    }

    @Override
    protected void init() {
        super.init();

        int boxX  = (this.width  - BOX_WIDTH)  / 2;
        int boxY  = (this.height - BOX_HEIGHT) / 2;
        int btnY  = boxY - BTN_HEIGHT - BTN_MARGIN;

        // Visual border left edge = boxX - BORDER
        // Visual border right edge = boxX + BOX_WIDTH + BORDER
        int visualLeft  = boxX - BORDER;
        int visualRight = boxX + BOX_WIDTH + BORDER;
        int visualWidth = visualRight - visualLeft;  // = BOX_WIDTH + 2*BORDER

        // [Shop] — flush with visual left border
        addRenderableWidget(Button.builder(Component.literal("Shop"), btn -> {
            // future: switch to shop tab
        }).bounds(visualLeft, btnY, BTN_WIDTH, BTN_HEIGHT).build());

        // [Profession] — right after Shop + gap
        addRenderableWidget(Button.builder(Component.literal("Profession"), btn -> {
            // future: switch to profession tab
        }).bounds(visualLeft + BTN_WIDTH + BTN_GAP, btnY, BTN_WIDTH, BTN_HEIGHT).build());

        // [✕] — flush with visual right border
        addRenderableWidget(Button.builder(Component.literal("✕"), btn -> this.onClose())
                .bounds(visualRight - CLOSE_WIDTH, btnY, CLOSE_WIDTH, BTN_HEIGHT)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Dark translucent full-screen dim (no extractBackground — causes double-blur crash)
        graphics.fill(0, 0, this.width, this.height, 0xA0000000);

        int boxX = (this.width  - BOX_WIDTH)  / 2;
        int boxY = (this.height - BOX_HEIGHT) / 2;

        // Border + empty dark container
        graphics.fill(boxX - BORDER, boxY - BORDER,
                      boxX + BOX_WIDTH + BORDER, boxY + BOX_HEIGHT + BORDER, 0xFF888888);
        graphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, 0xF0101010);

        // Widgets (buttons) on top
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
