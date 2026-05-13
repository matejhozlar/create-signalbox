package com.saunhardy.createwebhooks.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class TrainLifecycleConfig {
    public final ModConfigSpec.BooleanValue creationEnabled;
    public final ModConfigSpec.BooleanValue deletionEnabled;
    public final ModConfigSpec.ConfigValue<String> webhookUrl;

    TrainLifecycleConfig(ModConfigSpec.Builder builder) {
        builder.push("trainLifecycle");

        creationEnabled = builder
                .comment("Enable notifications when a train is assembled (requires Create mod)")
                .define("creationEnabled", true);

        deletionEnabled = builder
                .comment("Enable notifications when a train is disassembled (requires Create mod)")
                .define("deletionEnabled", true);

        webhookUrl = builder
                .comment("Override webhook URL for train lifecycle notifications (leave empty to use the global webhook)")
                .define("webhookUrl", "");

        builder.pop();
    }
}
