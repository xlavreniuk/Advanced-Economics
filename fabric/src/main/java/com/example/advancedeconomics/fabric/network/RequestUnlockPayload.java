package com.example.advancedeconomics.fabric.network;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestUnlockPayload(String itemId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestUnlockPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AdvancedEconomicsCommon.MOD_ID, "request_unlock"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestUnlockPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, RequestUnlockPayload::itemId,
            RequestUnlockPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
