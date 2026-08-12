package com.example.advancedeconomics.fabric.client;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

/**
 * Fabric Client Mod Initializer for Advanced Economics (v0.8).
 * - Registers 'N' keybinding to open the economics screen.
 * - Injects "$ balance" label + "Economics" button into the inventory screen,
 *   using reflection to get the real leftPos/topPos so it stays correct
 *   even when the recipe book panel is open.
 */
public class EconomicsFabricClient implements ClientModInitializer {

    private static KeyMapping openBoxKey;

    // Cached reflection fields — resolved once at init time
    private static Field fLeftPos;
    private static Field fTopPos;
    private static Field fImageWidth;

    static {
        try {
            fLeftPos    = AbstractContainerScreen.class.getDeclaredField("leftPos");
            fTopPos     = AbstractContainerScreen.class.getDeclaredField("topPos");
            fImageWidth = AbstractContainerScreen.class.getDeclaredField("imageWidth");
            fLeftPos.setAccessible(true);
            fTopPos.setAccessible(true);
            fImageWidth.setAccessible(true);
        } catch (Exception e) {
            AdvancedEconomicsCommon.LOGGER.error("[AE] Failed to cache reflection fields: {}", e.getMessage());
        }
    }

    @Override
    public void onInitializeClient() {
        try {
            AdvancedEconomicsCommon.LOGGER.info("=============================================");
            AdvancedEconomicsCommon.LOGGER.info("Advanced Economics Fabric Client v0.8");
            AdvancedEconomicsCommon.LOGGER.info("=============================================");

            // Register 'N' keybinding
            openBoxKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.advanced_economics.open_box",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_N,
                    KeyMapping.Category.MISC
            ));

            // Tick: open/close with N (only in-game)
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                try {
                    if (client.level == null) return;
                    while (openBoxKey.consumeClick()) {
                        if (client.gui.screen() instanceof EconomicsBoxScreen) {
                            client.gui.setScreen(null);
                        } else if (client.gui.screen() == null) {
                            client.gui.setScreen(new EconomicsBoxScreen());
                        }
                    }
                } catch (Throwable t) {
                    AdvancedEconomicsCommon.LOGGER.error("[AE] Key tick error: {}", t.getMessage(), t);
                }
            });

            // Inject widgets into inventory screen.
            // AFTER_INIT fires on every rebuildWidgets() call — including when the
            // recipe book panel opens/closes — so the button stays correctly anchored.
            ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
                if (!(screen instanceof InventoryScreen inv)) return;

                // Use reflection to get the real leftPos/topPos/imageWidth, which
                // shift left when the recipe book panel is toggled open.
                int leftPos;
                int topPos;
                int imageWidth;
                try {
                    leftPos    = (int) fLeftPos.get(inv);
                    topPos     = (int) fTopPos.get(inv);
                    imageWidth = (int) fImageWidth.get(inv);
                } catch (Exception e) {
                    // Fallback: standard inventory is 176×166, centred
                    leftPos    = (scaledWidth  - 176) / 2;
                    topPos     = (scaledHeight - 166) / 2;
                    imageWidth = 176;
                }

                int btnHeight  = 14;
                int ecoWidth   = 60;  // "Economics" button width
                int balWidth   = 52;  // "$ 0" balance box width
                int gap        = 2;
                int rowY       = topPos - btnHeight - 2;

                // "Economics" button — flush with right edge of inventory panel
                int ecoX = leftPos + imageWidth - ecoWidth;

                // "$ 0" balance box — immediately to the left of Economics button
                int balX = ecoX - gap - balWidth;

                // Balance display (disabled button used as a label)
                Screens.getWidgets(screen).add(
                        Button.builder(Component.literal("$ 0"), btn -> {})
                                .bounds(balX, rowY, balWidth, btnHeight)
                                .build()
                );

                // Economics button
                Screens.getWidgets(screen).add(
                        Button.builder(Component.literal("Economics"), btn ->
                                client.gui.setScreen(new EconomicsBoxScreen())
                        ).bounds(ecoX, rowY, ecoWidth, btnHeight).build()
                );
            });

            AdvancedEconomicsCommon.LOGGER.info("Ready. Press 'N' or use inventory button.");

        } catch (Throwable t) {
            AdvancedEconomicsCommon.LOGGER.error("[AE] Client init failed: {}", t.getMessage(), t);
        }
    }
}
