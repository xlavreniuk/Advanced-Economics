package com.example.advancedeconomics.fabric.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Text Widget for rendering player profession centered above the 3D entity model in InventoryScreen.
 */
public class ProfessionLabelWidget extends AbstractWidget {

    public ProfessionLabelWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.active = false; // Pure visual label, non-clickable
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;
        if (font == null) return;

        String profession = ClientEconomyState.getProfession();
        int textColor = "None".equalsIgnoreCase(profession) ? 0xAAAAAA : 0xFFDD55;

        int centerX = this.getX() + this.getWidth() / 2;
        int centerY = this.getY();

        graphics.centeredText(font, profession, centerX, centerY, textColor);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
