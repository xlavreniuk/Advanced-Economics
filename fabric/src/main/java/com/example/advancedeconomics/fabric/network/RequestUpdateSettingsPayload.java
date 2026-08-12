package com.example.advancedeconomics.fabric.network;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-Server Payload for updating economy settings and feature toggles.
 */
public record RequestUpdateSettingsPayload(
        int sellMultiplier,
        int buyMultiplier,
        int unlockMultiplier,
        boolean allowSelling,
        boolean allowBuying,
        boolean allowUnlocking,
        boolean enableProfessions,
        boolean enableXpLeveling
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestUpdateSettingsPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AdvancedEconomicsCommon.MOD_ID, "request_update_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestUpdateSettingsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RequestUpdateSettingsPayload::sellMultiplier,
            ByteBufCodecs.VAR_INT, RequestUpdateSettingsPayload::buyMultiplier,
            ByteBufCodecs.VAR_INT, RequestUpdateSettingsPayload::unlockMultiplier,
            ByteBufCodecs.BOOL, RequestUpdateSettingsPayload::allowSelling,
            ByteBufCodecs.BOOL, RequestUpdateSettingsPayload::allowBuying,
            ByteBufCodecs.BOOL, RequestUpdateSettingsPayload::allowUnlocking,
            ByteBufCodecs.BOOL, RequestUpdateSettingsPayload::enableProfessions,
            ByteBufCodecs.BOOL, RequestUpdateSettingsPayload::enableXpLeveling,
            RequestUpdateSettingsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
