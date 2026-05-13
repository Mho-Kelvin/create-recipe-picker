package io.github.mhokelvin.createrecipepicker.network;

import io.github.mhokelvin.createrecipepicker.RecipePickerState;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PacketRoundTripTest {

    private static final ResourceLocation CRUSHING = new ResourceLocation("create:crushing");
    private static final ResourceLocation MILLING = new ResourceLocation("create:milling");
    private static final ResourceLocation WHEAT_SEEDS = new ResourceLocation("minecraft:wheat_seeds");
    private static final ResourceLocation BLACKSTONE = new ResourceLocation("minecraft:blackstone");
    private static final ResourceLocation RECIPE_A = new ResourceLocation("create:crushing/wheat_seeds_to_flour");
    private static final ResourceLocation RECIPE_B = new ResourceLocation("examplepack:milling/blackstone_dust");

    @Test
    void setPreferenceRoundTrip() {
        SetPreferencePacket original = new SetPreferencePacket(CRUSHING, WHEAT_SEEDS, RECIPE_A);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        SetPreferencePacket.encode(original, buf);
        SetPreferencePacket decoded = SetPreferencePacket.decode(buf);
        assertEquals(original, decoded);
    }

    @Test
    void clearPreferenceRoundTrip() {
        ClearPreferencePacket original = new ClearPreferencePacket(MILLING, BLACKSTONE);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ClearPreferencePacket.encode(original, buf);
        ClearPreferencePacket decoded = ClearPreferencePacket.decode(buf);
        assertEquals(original, decoded);
    }

    @Test
    void syncPreferencesEmptyRoundTrip() {
        SyncPreferencesPacket original = new SyncPreferencesPacket(Map.of());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        SyncPreferencesPacket.encode(original, buf);
        SyncPreferencesPacket decoded = SyncPreferencesPacket.decode(buf);
        assertEquals(0, decoded.preferences().size());
    }

    @Test
    void syncPreferencesMultiEntryRoundTrip() {
        Map<RecipePickerState.Key, ResourceLocation> prefs = new LinkedHashMap<>();
        prefs.put(new RecipePickerState.Key(CRUSHING, WHEAT_SEEDS), RECIPE_A);
        prefs.put(new RecipePickerState.Key(MILLING, BLACKSTONE), RECIPE_B);

        SyncPreferencesPacket original = new SyncPreferencesPacket(prefs);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        SyncPreferencesPacket.encode(original, buf);
        SyncPreferencesPacket decoded = SyncPreferencesPacket.decode(buf);

        assertEquals(2, decoded.preferences().size());
        assertEquals(RECIPE_A, decoded.preferences().get(new RecipePickerState.Key(CRUSHING, WHEAT_SEEDS)));
        assertEquals(RECIPE_B, decoded.preferences().get(new RecipePickerState.Key(MILLING, BLACKSTONE)));
    }
}
