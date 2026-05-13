package io.github.mhokelvin.createrecipepicker.network;

import io.github.mhokelvin.createrecipepicker.CreateRecipePicker;
import io.github.mhokelvin.createrecipepicker.RecipePickerState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class RecipePickerNetworking {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CreateRecipePicker.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private RecipePickerNetworking() {}

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, SyncPreferencesPacket.class,
                SyncPreferencesPacket::encode,
                SyncPreferencesPacket::decode,
                SyncPreferencesPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, SetPreferencePacket.class,
                SetPreferencePacket::encode,
                SetPreferencePacket::decode,
                SetPreferencePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, ClearPreferencePacket.class,
                ClearPreferencePacket::encode,
                ClearPreferencePacket::decode,
                ClearPreferencePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendSyncTo(ServerPlayer player) {
        RecipePickerState state = RecipePickerState.get(player.server.overworld());
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncPreferencesPacket(state.all()));
    }

    public static void broadcastSync(MinecraftServer server) {
        RecipePickerState state = RecipePickerState.get(server.overworld());
        CHANNEL.send(
                PacketDistributor.ALL.noArg(),
                new SyncPreferencesPacket(state.all()));
    }
}
