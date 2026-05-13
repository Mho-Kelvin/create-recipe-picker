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

public record ClearPreferencePacket(
        ResourceLocation typeId,
        ResourceLocation inputItemId) {

    public static void encode(ClearPreferencePacket pkt, FriendlyByteBuf buf) {
        buf.writeResourceLocation(pkt.typeId);
        buf.writeResourceLocation(pkt.inputItemId);
    }

    public static ClearPreferencePacket decode(FriendlyByteBuf buf) {
        return new ClearPreferencePacket(
                buf.readResourceLocation(),
                buf.readResourceLocation());
    }

    public static void handle(ClearPreferencePacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
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
            state.clearPreference(pkt.typeId, pkt.inputItemId);
            CreateRecipePicker.LOGGER.info("[{}] cleared {} {}",
                    sender.getName().getString(), pkt.typeId, pkt.inputItemId);
            RecipePickerNetworking.broadcastSync(sender.server);
        });
        ctx.setPacketHandled(true);
    }
}
