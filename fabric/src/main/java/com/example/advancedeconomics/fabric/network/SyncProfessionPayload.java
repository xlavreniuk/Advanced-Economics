package com.example.advancedeconomics.fabric.network;

import com.example.advancedeconomics.AdvancedEconomicsCommon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sync Profession State Payload (v0.23).
 * Syncs profession name, level, current XP, and max XP to client.
 */
public record SyncProfessionPayload(String profession, int level, long xp, long maxXp) implements CustomPacketPayload {

    public static final Type<SyncProfessionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AdvancedEconomicsCommon.MOD_ID, "sync_profession")
    );

    public static final StreamCodec<FriendlyByteBuf, SyncProfessionPayload> CODEC = CustomPacketPayload.codec(
            SyncProfessionPayload::write,
            SyncProfessionPayload::new
    );

    public SyncProfessionPayload(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readVarInt(), buf.readVarLong(), buf.readVarLong());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(profession);
        buf.writeVarInt(level);
        buf.writeVarLong(xp);
        buf.writeVarLong(maxXp);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
