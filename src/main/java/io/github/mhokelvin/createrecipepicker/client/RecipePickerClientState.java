package io.github.mhokelvin.createrecipepicker.client;

import io.github.mhokelvin.createrecipepicker.RecipePickerState;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class RecipePickerClientState {

    private static volatile Map<RecipePickerState.Key, ResourceLocation> preferences = Map.of();

    private RecipePickerClientState() {}

    public static Map<RecipePickerState.Key, ResourceLocation> all() {
        return preferences;
    }

    @Nullable
    public static ResourceLocation getPreference(ResourceLocation typeId, ResourceLocation inputItemId) {
        return preferences.get(new RecipePickerState.Key(typeId, inputItemId));
    }

    public static void setPreferences(Map<RecipePickerState.Key, ResourceLocation> newPrefs) {
        preferences = Map.copyOf(newPrefs);
    }
}
