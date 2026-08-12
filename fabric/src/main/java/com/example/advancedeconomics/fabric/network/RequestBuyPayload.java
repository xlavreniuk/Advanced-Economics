package com.example.advancedeconomics.fabric.network;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestBuyPayload(String itemId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestBuyPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AdvancedEconomicsCommon.MOD_ID, "request_buy"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestBuyPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, RequestBuyPayload::itemId,
            RequestBuyPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
