package io.github.mhokelvin.createrecipepicker.mixin;

import io.github.mhokelvin.createrecipepicker.CreateRecipePicker;
import io.github.mhokelvin.createrecipepicker.RecipePickerState;
import com.simibubi.create.AllRecipeTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = AllRecipeTypes.class, remap = false)
public class AllRecipeTypesMixin {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "find", at = @At("RETURN"), cancellable = true)
    private <C extends Container, T extends Recipe<C>> void createrecipepicker$applyPreference(
            C inv, Level world, CallbackInfoReturnable<Optional<T>> cir) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        if (inv.isEmpty()) return;
        ItemStack input = inv.getItem(0);
        if (input.isEmpty()) return;

        AllRecipeTypes self = (AllRecipeTypes) (Object) this;
        ResourceLocation typeId = self.getId();
        ResourceLocation inputId = BuiltInRegistries.ITEM.getKey(input.getItem());

        RecipePickerState state = RecipePickerState.get(serverLevel.getServer().overworld());
        ResourceLocation preferredId = state.getPreference(typeId, inputId);
        if (preferredId == null) return;

        Optional<? extends Recipe<?>> looked = serverLevel.getRecipeManager().byKey(preferredId);
        if (looked.isEmpty()) return;
        Recipe recipe = looked.get();
        if (recipe.getType() != self.getType()) return;
        if (!recipe.matches(inv, world)) return;

        CreateRecipePicker.LOGGER.debug("override {} input={} -> {}", typeId, inputId, preferredId);
        cir.setReturnValue(Optional.of((T) recipe));
    }
}
