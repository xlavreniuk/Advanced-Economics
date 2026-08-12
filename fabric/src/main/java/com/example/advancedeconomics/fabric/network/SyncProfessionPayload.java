package com.example.advancedeconomics.fabric.network;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-Client Network Payload for syncing player profession.
 */
public record SyncProfessionPayload(String profession) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncProfessionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AdvancedEconomicsCommon.MOD_ID, "sync_profession"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncProfessionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SyncProfessionPayload::profession,
            SyncProfessionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
