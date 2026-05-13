package io.github.mhokelvin.createrecipepicker.network;

import io.github.mhokelvin.createrecipepicker.CreateRecipePicker;
import io.github.mhokelvin.createrecipepicker.RecipePickerState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetPreferencePacket(
        ResourceLocation typeId,
        ResourceLocation inputItemId,
        ResourceLocation recipeId) {

    public static void encode(SetPreferencePacket pkt, FriendlyByteBuf buf) {
        buf.writeResourceLocation(pkt.typeId);
        buf.writeResourceLocation(pkt.inputItemId);
        buf.writeResourceLocation(pkt.recipeId);
    }

    public static SetPreferencePacket decode(FriendlyByteBuf buf) {
        return new SetPreferencePacket(
                buf.readResourceLocation(),
                buf.readResourceLocation(),
                buf.readResourceLocation());
    }

    public static void handle(SetPreferencePacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            if (!sender.server.isSingleplayer() && !sender.hasPermissions(2)) {
                sender.sendSystemMessage(Component.translatable("createrecipepicker.error.not_op")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            RecipePickerState state = RecipePickerState.get(sender.server.overworld());
            state.setPreference(pkt.typeId, pkt.inputItemId, pkt.recipeId);
            CreateRecipePicker.LOGGER.info("[{}] set {} {} -> {}",
                    sender.getName().getString(), pkt.typeId, pkt.inputItemId, pkt.recipeId);
            RecipePickerNetworking.broadcastSync(sender.server);
        });
        ctx.setPacketHandled(true);
    }
}
