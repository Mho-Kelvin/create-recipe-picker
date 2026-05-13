package com.example.createrecipepicker;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CreateRecipePicker.MODID)
public class CreateRecipePicker {
    public static final String MODID = "createrecipepicker";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateRecipePicker(FMLJavaModLoadingContext context) {
        LOGGER.info("Create Recipe Picker loading");
    }
}
