package com.example.advancedeconomics.fabric.network;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-Client Network Payload for syncing player balance.
 */
public record SyncBalancePayload(long balance) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncBalancePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AdvancedEconomicsCommon.MOD_ID, "sync_balance"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBalancePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, SyncBalancePayload::balance,
            SyncBalancePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
