package com.example.advancedeconomics;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record EconomyStatePayload(int money, int investment, int loan, String profession, int professionBonus, String dailyDeal, DailyQuestState dailyQuest, FeatureSettingsState featureSettings, List<Entry> entries, List<String> gambleHistory) implements CustomPacketPayload {
    public static final Type<EconomyStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("advanced-economics", "economy_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EconomyStatePayload> CODEC = StreamCodec.ofMember(EconomyStatePayload::write, EconomyStatePayload::read);

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(money);
        buffer.writeVarInt(investment);
        buffer.writeVarInt(loan);
        buffer.writeUtf(profession);
        buffer.writeVarInt(professionBonus);
        buffer.writeUtf(dailyDeal);
        buffer.writeUtf(dailyQuest.itemId);
        buffer.writeUtf(dailyQuest.displayName);
        buffer.writeVarInt(dailyQuest.required);
        buffer.writeVarInt(dailyQuest.progress);
        buffer.writeVarInt(dailyQuest.reward);
        buffer.writeBoolean(dailyQuest.claimed);
        buffer.writeBoolean(dailyQuest.hidden);
        buffer.writeBoolean(featureSettings.shopSell);
        buffer.writeBoolean(featureSettings.wallet);
        buffer.writeBoolean(featureSettings.investments);
        buffer.writeBoolean(featureSettings.gambling);
        buffer.writeBoolean(featureSettings.quests);
        buffer.writeBoolean(featureSettings.professions);
        buffer.writeBoolean(featureSettings.teleports);
        buffer.writeBoolean(featureSettings.xpTrading);
        buffer.writeBoolean(featureSettings.itemServices);
        buffer.writeBoolean(featureSettings.pets);
        buffer.writeBoolean(featureSettings.insurance);
        buffer.writeBoolean(featureSettings.loans);
        buffer.writeCollection(entries, (entryBuffer, entry) -> {
            entryBuffer.writeUtf(entry.itemId);
            entryBuffer.writeUtf(entry.displayName);
            entryBuffer.writeUtf(entry.primaryName);
            entryBuffer.writeVarInt(entry.sellPrice);
            entryBuffer.writeVarInt(entry.buyPrice);
            entryBuffer.writeVarInt(entry.unlockPrice);
            entryBuffer.writeBoolean(entry.unlocked);
            entryBuffer.writeUtf(entry.unlockSource);
            entryBuffer.writeVarInt(entry.inventoryCount);
        });
        buffer.writeCollection(gambleHistory, (historyBuffer, message) -> historyBuffer.writeUtf(message));
    }

    private static EconomyStatePayload read(RegistryFriendlyByteBuf buffer) {
        int money = buffer.readVarInt();
        int investment = buffer.readVarInt();
        int loan = buffer.readVarInt();
        String profession = buffer.readUtf();
        int professionBonus = buffer.readVarInt();
        String dailyDeal = buffer.readUtf();
        DailyQuestState dailyQuest = new DailyQuestState(
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
        FeatureSettingsState featureSettings = new FeatureSettingsState(
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
        List<Entry> entries = buffer.readCollection(ArrayList::new, entryBuffer -> new Entry(
                entryBuffer.readUtf(),
                entryBuffer.readUtf(),
                entryBuffer.readUtf(),
                entryBuffer.readVarInt(),
                entryBuffer.readVarInt(),
                entryBuffer.readVarInt(),
                entryBuffer.readBoolean(),
                entryBuffer.readUtf(),
                entryBuffer.readVarInt()
        ));
        List<String> gambleHistory = buffer.readCollection(ArrayList::new, historyBuffer -> historyBuffer.readUtf());
        return new EconomyStatePayload(money, investment, loan, profession, professionBonus, dailyDeal, dailyQuest, featureSettings, entries, gambleHistory);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(
            String itemId,
            String displayName,
            String primaryName,
            int sellPrice,
            int buyPrice,
            int unlockPrice,
            boolean unlocked,
            String unlockSource,
            int inventoryCount
    ) {
    }

    public record DailyQuestState(
            String itemId,
            String displayName,
            int required,
            int progress,
            int reward,
            boolean claimed,
            boolean hidden
    ) {
    }

    public record FeatureSettingsState(
            boolean shopSell,
            boolean wallet,
            boolean investments,
            boolean gambling,
            boolean quests,
            boolean professions,
            boolean teleports,
            boolean xpTrading,
            boolean itemServices,
            boolean pets,
            boolean insurance,
            boolean loans
    ) {
    }
}
