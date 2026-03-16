package com.saunhardy.createsignalbox.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class SignalboxConfig {
    public static final WebhookConfig WEBHOOK;
    public static final TrainCrashConfig TRAIN_CRASH;
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        WEBHOOK = new WebhookConfig(builder);

        builder.push("events");
        TRAIN_CRASH = new TrainCrashConfig(builder);
        builder.pop();

        SPEC = builder.build();
    }
}
