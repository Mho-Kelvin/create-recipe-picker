package io.github.mhokelvin.createrecipepicker;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RecipePickerState extends SavedData {
    public static final String FILE_ID = "createrecipepicker";

    private final Map<Key, ResourceLocation> preferences = new HashMap<>();

    // Caller is responsible for passing the overworld so preferences are world-global,
    // not per-dimension.
    public static RecipePickerState get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(RecipePickerState::load, RecipePickerState::new, FILE_ID);
    }

    public RecipePickerState() {}

    @Nullable
    public ResourceLocation getPreference(ResourceLocation typeId, ResourceLocation inputItemId) {
        return preferences.get(new Key(typeId, inputItemId));
    }

    public void setPreference(ResourceLocation typeId, ResourceLocation inputItemId, ResourceLocation recipeId) {
        preferences.put(new Key(typeId, inputItemId), recipeId);
        setDirty();
    }

    public void clearPreference(ResourceLocation typeId, ResourceLocation inputItemId) {
        if (preferences.remove(new Key(typeId, inputItemId)) != null) {
            setDirty();
        }
    }

    public Map<Key, ResourceLocation> all() {
        return Collections.unmodifiableMap(preferences);
    }

    public int pruneMissing(Set<ResourceLocation> validRecipeIds) {
        int before = preferences.size();
        boolean changed = preferences.entrySet().removeIf(e -> !validRecipeIds.contains(e.getValue()));
        if (changed) setDirty();
        return before - preferences.size();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<Key, ResourceLocation> e : preferences.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("type", e.getKey().typeId().toString());
            entry.putString("input", e.getKey().inputItemId().toString());
            entry.putString("recipe", e.getValue().toString());
            list.add(entry);
        }
        tag.put("preferences", list);
        return tag;
    }

    public static RecipePickerState load(CompoundTag tag) {
        RecipePickerState state = new RecipePickerState();
        ListTag list = tag.getList("preferences", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String typeStr = entry.getString("type");
            String inputStr = entry.getString("input");
            String recipeStr = entry.getString("recipe");
            // ResourceLocation.tryParse("") returns a valid ResourceLocation with an
            // empty path (MC's path validator accepts zero-length paths). Guard explicitly
            // so missing NBT fields don't get loaded as a (minecraft:, minecraft:) key.
            if (typeStr.isEmpty() || inputStr.isEmpty() || recipeStr.isEmpty()) continue;
            ResourceLocation type = ResourceLocation.tryParse(typeStr);
            ResourceLocation input = ResourceLocation.tryParse(inputStr);
            ResourceLocation recipe = ResourceLocation.tryParse(recipeStr);
            if (type != null && input != null && recipe != null) {
                state.preferences.put(new Key(type, input), recipe);
            }
        }
        return state;
    }

    public record Key(ResourceLocation typeId, ResourceLocation inputItemId) {}
}
