package com.saunhardy.createwebhooks.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class TrainDerailConfig {
    public final ModConfigSpec.BooleanValue enabled;
    public final ModConfigSpec.IntValue cooldownSeconds;
    public final ModConfigSpec.ConfigValue<String> webhookUrl;

    TrainDerailConfig(ModConfigSpec.Builder builder) {
        builder.push("trainDerail");

        enabled = builder
                .comment("Enable train derailment notifications for non-crash derailments (requires Create mod)")
                .define("enabled", true);

        cooldownSeconds = builder
                .comment("Cooldown in seconds before the same train can trigger another derailment notification (0 to disable)")
                .defineInRange("cooldownSeconds", 60, 0, 3600);

        webhookUrl = builder
                .comment("Override webhook URL for train derailment notifications (leave empty to use the global webhook)")
                .define("webhookUrl", "");

        builder.pop();
    }
}
