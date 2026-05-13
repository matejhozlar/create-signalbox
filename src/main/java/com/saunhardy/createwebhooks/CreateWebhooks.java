package com.saunhardy.createwebhooks;

import com.mojang.logging.LogUtils;
import com.saunhardy.createwebhooks.commands.WebhooksCommand;
import com.saunhardy.createwebhooks.config.WebhooksConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(CreateWebhooks.MODID)
public class CreateWebhooks {
    public static final String MODID = "createwebhooks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateWebhooks(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, WebhooksConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        WebhooksCommand.register(event.getDispatcher());
    }
}
