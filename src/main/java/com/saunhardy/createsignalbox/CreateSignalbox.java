package com.saunhardy.createsignalbox;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(CreateSignalbox.MODID)
public class CreateSignalbox {
    public static final String MODID = "createsignalbox";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateSignalbox(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
