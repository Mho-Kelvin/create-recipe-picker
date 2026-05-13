package io.github.mhokelvin.createrecipepicker;

import io.github.mhokelvin.createrecipepicker.network.RecipePickerNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = CreateRecipePicker.MODID)
public final class RecipePickerLifecycle {

    private RecipePickerLifecycle() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        prune(event.getServer());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        // Fires once with null player after /reload (server-wide sync), then once
        // per player. Only run pruning on the server-wide pass.
        if (event.getPlayer() != null) return;
        prune(event.getPlayerList().getServer());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            RecipePickerNetworking.sendSyncTo(sp);
        }
    }

    private static void prune(MinecraftServer server) {
        Set<ResourceLocation> validIds = server.getRecipeManager().getRecipes().stream()
                .map(Recipe::getId)
                .collect(Collectors.toSet());
        RecipePickerState state = RecipePickerState.get(server.overworld());
        int removed = state.pruneMissing(validIds);
        if (removed > 0) {
            CreateRecipePicker.LOGGER.info("Pruned {} stale recipe preference(s)", removed);
            RecipePickerNetworking.broadcastSync(server);
        }
    }
}
