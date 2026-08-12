package com.example.advancedeconomics.fabric.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Text Widget for rendering player profession in crisp white text above the 3D entity model in InventoryScreen.
 */
public class ProfessionLabelWidget extends AbstractWidget {

    public ProfessionLabelWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.active = false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;
        if (font == null) return;

        String profession = ClientEconomyState.getProfession();
        int centerX = this.getX() + this.getWidth() / 2;
        int centerY = this.getY();

        // Crisp white text as requested (e.g. "None")
        graphics.centeredText(font, profession, centerX, centerY, 0xFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
