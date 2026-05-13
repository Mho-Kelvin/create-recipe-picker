package io.github.mhokelvin.createrecipepicker.network;

import io.github.mhokelvin.createrecipepicker.RecipePickerState;
import io.github.mhokelvin.createrecipepicker.client.RecipePickerClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record SyncPreferencesPacket(Map<RecipePickerState.Key, ResourceLocation> preferences) {

    public static void encode(SyncPreferencesPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.preferences.size());
        pkt.preferences.forEach((key, recipe) -> {
            buf.writeResourceLocation(key.typeId());
            buf.writeResourceLocation(key.inputItemId());
            buf.writeResourceLocation(recipe);
        });
    }

    public static SyncPreferencesPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<RecipePickerState.Key, ResourceLocation> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            ResourceLocation typeId = buf.readResourceLocation();
            ResourceLocation inputId = buf.readResourceLocation();
            ResourceLocation recipeId = buf.readResourceLocation();
            map.put(new RecipePickerState.Key(typeId, inputId), recipeId);
        }
        return new SyncPreferencesPacket(map);
    }

    public static void handle(SyncPreferencesPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> RecipePickerClientState.setPreferences(pkt.preferences)));
        ctx.setPacketHandled(true);
    }
}
