package com.saunhardy.createsignalbox;

import com.mojang.logging.LogUtils;
import com.saunhardy.createsignalbox.commands.SignalboxCommand;
import com.saunhardy.createsignalbox.config.SignalboxConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(CreateSignalbox.MODID)
public class CreateSignalbox {
    public static final String MODID = "createsignalbox";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateSignalbox(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, SignalboxConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        SignalboxCommand.register(event.getDispatcher());
    }
}
