package com.example.advancedeconomics.fabric.network;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Clientbound Request Payload to set active player Profession.
 */
public record RequestSetProfessionPayload(String professionId) implements CustomPacketPayload {

    public static final Type<RequestSetProfessionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AdvancedEconomicsCommon.MOD_ID, "request_set_profession")
    );

    public static final StreamCodec<FriendlyByteBuf, RequestSetProfessionPayload> CODEC = CustomPacketPayload.codec(
            RequestSetProfessionPayload::write,
            RequestSetProfessionPayload::new
    );

    public RequestSetProfessionPayload(FriendlyByteBuf buf) {
        this(buf.readUtf());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(professionId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
