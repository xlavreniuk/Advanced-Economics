package com.example.advancedeconomics;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;

public class AdvancedEconomicsClient implements ClientModInitializer {
    private static final String OLD_DEFAULT_OPEN_KEY = "key.keyboard.m";
    private static final String DEFAULT_OPEN_KEY = "key.keyboard.n";
    private static KeyMapping openEconomyScreen;
    private static EconomyStatePayload latestState = new EconomyStatePayload(
            0,
            0,
            0,
            "",
            0,
            "",
            new EconomyStatePayload.DailyQuestState("", "", 0, 0, 0, false, false),
            new EconomyStatePayload.FeatureSettingsState(true, true, true, true, true, true, true, true, true, true, true, true),
            java.util.List.of(),
            java.util.List.of()
    );

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.parse("advanced-economics:keys"));
        openEconomyScreen = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.advanced-economics.open_economy",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_N,
                category
        ));
        migrateOldDefaultOpenKey();

        ClientPlayNetworking.registerGlobalReceiver(EconomyStatePayload.TYPE, (payload, context) -> latestState = payload);
        ClientTickEvents.END_CLIENT_TICK.register(AdvancedEconomicsClient::openScreenWhenRequested);
    }

    public static EconomyStatePayload latestState() {
        return latestState;
    }

    public static boolean isOpenEconomyKey(KeyEvent event) {
        return openEconomyScreen != null && openEconomyScreen.matches(event);
    }

    private static void openScreenWhenRequested(Minecraft minecraft) {
        while (openEconomyScreen.consumeClick()) {
            if (minecraft.screen instanceof EconomyScreen) {
                minecraft.setScreen(null);
            } else if (minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new EconomyScreen());
            }
        }
    }

    private static void migrateOldDefaultOpenKey() {
        if (!OLD_DEFAULT_OPEN_KEY.equals(openEconomyScreen.saveString())) {
            return;
        }

        openEconomyScreen.setKey(InputConstants.getKey(DEFAULT_OPEN_KEY));
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
    }
}
