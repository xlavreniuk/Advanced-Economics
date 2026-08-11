package com.example.advancedeconomics.fabric.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * Centered Minecraft GUI Screen for Advanced Economics (v0.3).
 * Uses MC 26.2's updated GuiGraphicsExtractor / extractRenderState API.
 * Press 'N' or ESC to close.
 */
public class EconomicsBoxScreen extends Screen {

    public EconomicsBoxScreen() {
        super(Component.literal("Advanced Economics"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Translucent background
        this.extractBackground(graphics, mouseX, mouseY, delta);

        int boxWidth = 240;
        int boxHeight = 140;
        int x = (this.width - boxWidth) / 2;
        int y = (this.height - boxHeight) / 2;

        // White border + dark fill
        graphics.fill(x - 2, y - 2, x + boxWidth + 2, y + boxHeight + 2, 0xFFCCCCCC);
        graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xF0101010);

        // Green accent line under title
        graphics.fill(x + 10, y + 32, x + boxWidth - 10, y + 33, 0xFF55FF55);

        // Title
        graphics.centeredText(this.getFont(), "Advanced Economics v0.3",
                this.width / 2, y + 14, 0xFFFFFF);

        // Info lines
        graphics.centeredText(this.getFont(), "UI Lib Integration: Active",
                this.width / 2, y + 44, 0x88FF88);
        graphics.centeredText(this.getFont(), "Market Status: Online",
                this.width / 2, y + 62, 0xFFFF55);
        graphics.centeredText(this.getFont(), "[Live Reload Mode Active]",
                this.width / 2, y + 80, 0xAAAAAA);

        // Bottom separator
        graphics.fill(x + 10, y + boxHeight - 30, x + boxWidth - 10, y + boxHeight - 29, 0xFF555555);

        // Close hint
        graphics.centeredText(this.getFont(), "Press  N  or  ESC  to close",
                this.width / 2, y + boxHeight - 20, 0xCCCCCC);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // GLFW_KEY_N = 78
        if (event.key() == 78) {
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
