package io.github.mhokelvin.createrecipepicker;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipePickerStateTest {

    private static final ResourceLocation CRUSHING = new ResourceLocation("create:crushing");
    private static final ResourceLocation WHEAT_SEEDS = new ResourceLocation("minecraft:wheat_seeds");
    private static final ResourceLocation RECIPE_A = new ResourceLocation("create:crushing/wheat_seeds_to_flour");
    private static final ResourceLocation RECIPE_B = new ResourceLocation("examplepack:crushing/wheat_seeds_to_dust");

    @Test
    void emptyStateReturnsNullForAnyKey() {
        RecipePickerState state = new RecipePickerState();
        assertNull(state.getPreference(CRUSHING, WHEAT_SEEDS));
    }

    @Test
    void setThenGetReturnsStoredValue() {
        RecipePickerState state = new RecipePickerState();
        state.setPreference(CRUSHING, WHEAT_SEEDS, RECIPE_A);
        assertEquals(RECIPE_A, state.getPreference(CRUSHING, WHEAT_SEEDS));
    }

    @Test
    void setOverwritesPreviousValue() {
        RecipePickerState state = new RecipePickerState();
        state.setPreference(CRUSHING, WHEAT_SEEDS, RECIPE_A);
        state.setPreference(CRUSHING, WHEAT_SEEDS, RECIPE_B);
        assertEquals(RECIPE_B, state.getPreference(CRUSHING, WHEAT_SEEDS));
    }

    @Test
    void clearRemovesEntry() {
        RecipePickerState state = new RecipePickerState();
        state.setPreference(CRUSHING, WHEAT_SEEDS, RECIPE_A);
        state.clearPreference(CRUSHING, WHEAT_SEEDS);
        assertNull(state.getPreference(CRUSHING, WHEAT_SEEDS));
    }

    @Test
    void saveLoadRoundTrip() {
        RecipePickerState original = new RecipePickerState();
        original.setPreference(CRUSHING, WHEAT_SEEDS, RECIPE_A);
        ResourceLocation millingType = new ResourceLocation("create:milling");
        ResourceLocation cocoa = new ResourceLocation("minecraft:cocoa_beans");
        ResourceLocation cocoaRecipe = new ResourceLocation("create:milling/cocoa_to_powder");
        original.setPreference(millingType, cocoa, cocoaRecipe);

        CompoundTag tag = original.save(new CompoundTag());
        RecipePickerState reloaded = RecipePickerState.load(tag);

        assertEquals(RECIPE_A, reloaded.getPreference(CRUSHING, WHEAT_SEEDS));
        assertEquals(cocoaRecipe, reloaded.getPreference(millingType, cocoa));
        assertEquals(2, reloaded.all().size());
    }

    @Test
    void loadSkipsMalformedEntries() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();

        CompoundTag good = new CompoundTag();
        good.putString("type", CRUSHING.toString());
        good.putString("input", WHEAT_SEEDS.toString());
        good.putString("recipe", RECIPE_A.toString());
        list.add(good);

        CompoundTag badType = new CompoundTag();
        badType.putString("type", "::not a resource location::");
        badType.putString("input", WHEAT_SEEDS.toString());
        badType.putString("recipe", RECIPE_A.toString());
        list.add(badType);

        CompoundTag wrongShape = new CompoundTag();
        wrongShape.put("type", StringTag.valueOf("nested-but-as-tag"));
        list.add(wrongShape);

        tag.put("preferences", list);

        RecipePickerState reloaded = RecipePickerState.load(tag);
        assertEquals(1, reloaded.all().size());
        assertEquals(RECIPE_A, reloaded.getPreference(CRUSHING, WHEAT_SEEDS));
    }

    @Test
    void pruneMissingRemovesEntriesWithMissingRecipeIds() {
        RecipePickerState state = new RecipePickerState();
        state.setPreference(CRUSHING, WHEAT_SEEDS, RECIPE_A);
        state.setPreference(CRUSHING, new ResourceLocation("minecraft:cocoa_beans"), RECIPE_B);

        int removed = state.pruneMissing(Set.of(RECIPE_A));

        assertEquals(1, removed);
        assertEquals(1, state.all().size());
        assertEquals(RECIPE_A, state.getPreference(CRUSHING, WHEAT_SEEDS));
        assertNull(state.getPreference(CRUSHING, new ResourceLocation("minecraft:cocoa_beans")));
    }

    @Test
    void pruneMissingKeepsAllWhenAllValid() {
        RecipePickerState state = new RecipePickerState();
        state.setPreference(CRUSHING, WHEAT_SEEDS, RECIPE_A);
        state.setPreference(CRUSHING, new ResourceLocation("minecraft:cocoa_beans"), RECIPE_B);

        int removed = state.pruneMissing(Set.of(RECIPE_A, RECIPE_B));

        assertEquals(0, removed);
        assertEquals(2, state.all().size());
    }

    @Test
    void pruneMissingClearsAllWhenNoneValid() {
        RecipePickerState state = new RecipePickerState();
        state.setPreference(CRUSHING, WHEAT_SEEDS, RECIPE_A);
        state.setPreference(CRUSHING, new ResourceLocation("minecraft:cocoa_beans"), RECIPE_B);

        int removed = state.pruneMissing(Set.of());

        assertEquals(2, removed);
        assertEquals(0, state.all().size());
    }

    @Test
    void pruneMissingOnEmptyStateIsNoOp() {
        RecipePickerState state = new RecipePickerState();
        int removed = state.pruneMissing(Set.of(RECIPE_A));
        assertEquals(0, removed);
    }

    @Test
    void allIsUnmodifiable() {
        RecipePickerState state = new RecipePickerState();
        state.setPreference(CRUSHING, WHEAT_SEEDS, RECIPE_A);
        assertTrue(
                throwsUnsupported(() -> state.all().clear()),
                "RecipePickerState.all() must return an unmodifiable map");
    }

    private static boolean throwsUnsupported(Runnable r) {
        try {
            r.run();
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        }
    }
}
