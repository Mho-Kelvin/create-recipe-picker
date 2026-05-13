package io.github.mhokelvin.createrecipepicker.command;

import io.github.mhokelvin.createrecipepicker.CreateRecipePicker;
import io.github.mhokelvin.createrecipepicker.RecipePickerState;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;

// Temporary developer-only command for verifying the mixin pipeline before
// the real GUI lands. Remove once /recipepicker (step 5 of HANDOFF.md) exists.
@Mod.EventBusSubscriber(modid = CreateRecipePicker.MODID)
public final class RecipePickerDebugCommand {

    private RecipePickerDebugCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("crp-debug")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("type", ResourceLocationArgument.id())
                                        .then(Commands.argument("input", ResourceLocationArgument.id())
                                                .then(Commands.argument("recipe", ResourceLocationArgument.id())
                                                        .executes(RecipePickerDebugCommand::runSet)))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("type", ResourceLocationArgument.id())
                                        .then(Commands.argument("input", ResourceLocationArgument.id())
                                                .executes(RecipePickerDebugCommand::runClear))))
                        .then(Commands.literal("list")
                                .executes(RecipePickerDebugCommand::runList))
                        .then(Commands.literal("candidates")
                                .then(Commands.argument("type", ResourceLocationArgument.id())
                                        .then(Commands.argument("input", ResourceLocationArgument.id())
                                                .executes(RecipePickerDebugCommand::runCandidates)))));
    }

    private static int runSet(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        ResourceLocation type = ResourceLocationArgument.getId(ctx, "type");
        ResourceLocation input = ResourceLocationArgument.getId(ctx, "input");
        ResourceLocation recipe = ResourceLocationArgument.getId(ctx, "recipe");
        ServerLevel overworld = ctx.getSource().getServer().overworld();
        RecipePickerState.get(overworld).setPreference(type, input, recipe);
        ctx.getSource().sendSuccess(
                () -> Component.literal("crp-debug: set " + type + " " + input + " -> " + recipe),
                true);
        return 1;
    }

    private static int runClear(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        ResourceLocation type = ResourceLocationArgument.getId(ctx, "type");
        ResourceLocation input = ResourceLocationArgument.getId(ctx, "input");
        ServerLevel overworld = ctx.getSource().getServer().overworld();
        RecipePickerState.get(overworld).clearPreference(type, input);
        ctx.getSource().sendSuccess(
                () -> Component.literal("crp-debug: cleared " + type + " " + input),
                true);
        return 1;
    }

    private static int runCandidates(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        ResourceLocation typeId = ResourceLocationArgument.getId(ctx, "type");
        ResourceLocation inputId = ResourceLocationArgument.getId(ctx, "input");
        ServerLevel level = ctx.getSource().getServer().overworld();

        RecipeType<?> type = BuiltInRegistries.RECIPE_TYPE.get(typeId);
        if (type == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown recipe type: " + typeId));
            return 0;
        }
        Item item = BuiltInRegistries.ITEM.get(inputId);
        if (item == net.minecraft.world.item.Items.AIR) {
            ctx.getSource().sendFailure(Component.literal("Unknown item: " + inputId));
            return 0;
        }
        ItemStack testStack = new ItemStack(item);

        List<? extends Recipe<?>> all = level.getRecipeManager().getAllRecipesFor((RecipeType) type);
        List<ResourceLocation> matching = all.stream()
                .filter(r -> ingredientsAccept(r, testStack))
                .map(Recipe::getId)
                .toList();

        if (matching.isEmpty()) {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("crp-debug: no " + typeId + " recipes accept " + inputId
                            + " (scanned " + all.size() + " total)"),
                    false);
            return 0;
        }
        ctx.getSource().sendSuccess(
                () -> Component.literal("crp-debug: " + matching.size() + " candidate(s) for "
                        + typeId + " of " + inputId + ":"),
                false);
        for (ResourceLocation id : matching) {
            ctx.getSource().sendSuccess(() -> Component.literal("  " + id), false);
        }
        return matching.size();
    }

    private static boolean ingredientsAccept(Recipe<?> recipe, ItemStack stack) {
        for (Ingredient ing : recipe.getIngredients()) {
            if (ing.test(stack)) return true;
        }
        return false;
    }

    private static int runList(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        ServerLevel overworld = ctx.getSource().getServer().overworld();
        Map<RecipePickerState.Key, ResourceLocation> all = RecipePickerState.get(overworld).all();
        if (all.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("crp-debug: no preferences set"), false);
            return 0;
        }
        ctx.getSource().sendSuccess(
                () -> Component.literal("crp-debug: " + all.size() + " preference(s):"),
                false);
        for (Map.Entry<RecipePickerState.Key, ResourceLocation> e : all.entrySet()) {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("  " + e.getKey().typeId() + " " + e.getKey().inputItemId() + " -> " + e.getValue()),
                    false);
        }
        return all.size();
    }
}
