package com.example.advancedeconomics.fabric.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * Centered Minecraft GUI Screen for Advanced Economics (v0.5).
 * Fix: do NOT call extractBackground() — it triggers a second blur pass per frame
 * which crashes with "Can only blur once per frame". Draw our own overlay instead.
 */
public class EconomicsBoxScreen extends Screen {

    public EconomicsBoxScreen() {
        super(Component.literal("Advanced Economics"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Draw our own translucent dark overlay — do NOT call extractBackground() or
        // extractBlurredBackground() as those trigger a second blur and crash MC 26.2.
        graphics.fill(0, 0, this.width, this.height, 0xA0000000);

        int boxWidth = 260;
        int boxHeight = 150;
        int x = (this.width - boxWidth) / 2;
        int y = (this.height - boxHeight) / 2;

        // Border + dark box
        graphics.fill(x - 2, y - 2, x + boxWidth + 2, y + boxHeight + 2, 0xFFCCCCCC);
        graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xF0101010);

        // Green accent line under title
        graphics.fill(x + 10, y + 34, x + boxWidth - 10, y + 35, 0xFF55FF55);

        // Title
        graphics.centeredText(this.getFont(), "Advanced Economics v0.5",
                this.width / 2, y + 16, 0x55FF55);

        // Info lines
        graphics.centeredText(this.getFont(), "UI Lib Integration: Active",
                this.width / 2, y + 48, 0xFFFFFF);
        graphics.centeredText(this.getFont(), "Market Status: Online",
                this.width / 2, y + 66, 0xFFFF55);
        graphics.centeredText(this.getFont(), "[Live Reload Mode Active]",
                this.width / 2, y + 84, 0xAAAAAA);

        // Bottom separator + close hint
        graphics.fill(x + 10, y + boxHeight - 32, x + boxWidth - 10, y + boxHeight - 31, 0xFF333333);
        graphics.centeredText(this.getFont(), "Press  N  or  ESC  to close",
                this.width / 2, y + boxHeight - 22, 0x888888);

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
