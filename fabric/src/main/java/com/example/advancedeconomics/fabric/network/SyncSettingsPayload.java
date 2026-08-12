package com.example.advancedeconomics.fabric.network;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-Client Payload for syncing economy multipliers and feature toggles.
 */
public record SyncSettingsPayload(
        int sellMultiplier,
        int buyMultiplier,
        int unlockMultiplier,
        boolean allowSelling,
        boolean allowBuying,
        boolean allowUnlocking,
        boolean enableProfessions,
        boolean enableXpLeveling
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncSettingsPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AdvancedEconomicsCommon.MOD_ID, "sync_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSettingsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncSettingsPayload::sellMultiplier,
            ByteBufCodecs.VAR_INT, SyncSettingsPayload::buyMultiplier,
            ByteBufCodecs.VAR_INT, SyncSettingsPayload::unlockMultiplier,
            ByteBufCodecs.BOOL, SyncSettingsPayload::allowSelling,
            ByteBufCodecs.BOOL, SyncSettingsPayload::allowBuying,
            ByteBufCodecs.BOOL, SyncSettingsPayload::allowUnlocking,
            ByteBufCodecs.BOOL, SyncSettingsPayload::enableProfessions,
            ByteBufCodecs.BOOL, SyncSettingsPayload::enableXpLeveling,
            SyncSettingsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
