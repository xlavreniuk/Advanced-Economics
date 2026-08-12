package com.example.advancedeconomics.fabric.network;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-Client Payload for syncing player unlocked item IDs (comma separated).
 */
public record SyncUnlocksPayload(String commaSeparatedUnlocks) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncUnlocksPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AdvancedEconomicsCommon.MOD_ID, "sync_unlocks"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncUnlocksPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SyncUnlocksPayload::commaSeparatedUnlocks,
            SyncUnlocksPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
