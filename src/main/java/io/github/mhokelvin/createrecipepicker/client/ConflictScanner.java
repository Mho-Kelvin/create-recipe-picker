package io.github.mhokelvin.createrecipepicker.client;

import io.github.mhokelvin.createrecipepicker.CreateRecipePicker;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ConflictScanner {

    private static final List<ResourceLocation> TARGET_TYPE_IDS = List.of(
            new ResourceLocation("create:crushing"),
            new ResourceLocation("create:milling"),
            new ResourceLocation("create:haunting"),
            new ResourceLocation("create:splashing"));

    public record Conflict(
            ResourceLocation typeId,
            ResourceLocation inputItemId,
            Item inputItem,
            List<Recipe<?>> alternatives) {}

    public record OutputEntry(ItemStack stack, float chance) {}

    private static volatile List<Conflict> conflicts = List.of();

    private ConflictScanner() {}

    public static List<Conflict> get() {
        return conflicts;
    }

    public static void rebuild(RecipeManager recipeManager) {
        RegistryAccess registries = Minecraft.getInstance().level == null
                ? RegistryAccess.EMPTY
                : Minecraft.getInstance().level.registryAccess();

        List<Conflict> result = new ArrayList<>();
        int rawGroups = 0;
        int duplicatesRemoved = 0;

        for (ResourceLocation typeId : TARGET_TYPE_IDS) {
            RecipeType<?> type = BuiltInRegistries.RECIPE_TYPE.get(typeId);
            if (type == null) continue;

            Map<ResourceLocation, List<Recipe<?>>> byInput = new LinkedHashMap<>();

            @SuppressWarnings({"rawtypes", "unchecked"})
            List<? extends Recipe<?>> all = recipeManager.getAllRecipesFor((RecipeType) type);
            for (Recipe<?> recipe : all) {
                if (recipe.getIngredients().isEmpty()) continue;
                Ingredient first = recipe.getIngredients().get(0);
                Set<Item> seen = new HashSet<>();
                for (ItemStack matching : first.getItems()) {
                    Item item = matching.getItem();
                    if (!seen.add(item)) continue;
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
                    byInput.computeIfAbsent(itemId, k -> new ArrayList<>()).add(recipe);
                }
            }

            for (Map.Entry<ResourceLocation, List<Recipe<?>>> e : byInput.entrySet()) {
                if (e.getValue().size() < 2) continue;
                rawGroups++;

                // Dedup by output signature. Two recipes that produce identical items at
                // identical counts and chances are indistinguishable to the player.
                Map<String, Recipe<?>> bySig = new LinkedHashMap<>();
                for (Recipe<?> r : e.getValue()) {
                    bySig.putIfAbsent(outputSignature(r, registries), r);
                }
                duplicatesRemoved += e.getValue().size() - bySig.size();

                if (bySig.size() < 2) continue;

                Item inputItem = BuiltInRegistries.ITEM.get(e.getKey());
                result.add(new Conflict(typeId, e.getKey(), inputItem, List.copyOf(bySig.values())));
            }
        }

        conflicts = Collections.unmodifiableList(result);
        CreateRecipePicker.LOGGER.info(
                "ConflictScanner: {} conflict(s) ({} raw groups, {} duplicate recipes dropped)",
                conflicts.size(), rawGroups, duplicatesRemoved);
    }

    public static List<OutputEntry> outputsOf(Recipe<?> recipe, RegistryAccess registries) {
        if (recipe instanceof ProcessingRecipe<?> proc) {
            List<OutputEntry> result = new ArrayList<>();
            for (ProcessingOutput out : proc.getRollableResults()) {
                ItemStack stack = out.getStack();
                if (stack.isEmpty()) continue;
                result.add(new OutputEntry(stack, out.getChance()));
            }
            if (!result.isEmpty()) return result;
        }
        ItemStack primary = recipe.getResultItem(registries);
        return primary.isEmpty() ? List.of() : List.of(new OutputEntry(primary, 1f));
    }

    private static String outputSignature(Recipe<?> recipe, RegistryAccess registries) {
        List<String> parts = new ArrayList<>();
        for (OutputEntry e : outputsOf(recipe, registries)) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(e.stack().getItem());
            int chanceMilli = Math.round(e.chance() * 1000);
            parts.add(itemId + "x" + e.stack().getCount() + "@" + chanceMilli);
        }
        Collections.sort(parts);
        return String.join("|", parts);
    }
}
