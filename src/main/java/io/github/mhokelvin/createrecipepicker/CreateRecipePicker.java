package io.github.mhokelvin.createrecipepicker;

import io.github.mhokelvin.createrecipepicker.network.RecipePickerNetworking;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CreateRecipePicker.MODID)
public class CreateRecipePicker {
    public static final String MODID = "createrecipepicker";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateRecipePicker(FMLJavaModLoadingContext context) {
        LOGGER.info("Create Recipe Picker loading");
        IEventBus modBus = context.getModEventBus();
        modBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(RecipePickerNetworking::register);
    }
}
