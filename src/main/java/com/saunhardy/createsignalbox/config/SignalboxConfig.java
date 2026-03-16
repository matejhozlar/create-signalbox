package com.saunhardy.createsignalbox.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class SignalboxConfig {
    public static final WebhookConfig WEBHOOK;
    public static final TrainCrashConfig TRAIN_CRASH;
    public static final TrainDerailConfig TRAIN_DERAIL;
    public static final TrainLifecycleConfig TRAIN_LIFECYCLE;
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        WEBHOOK = new WebhookConfig(builder);

        builder.push("events");
        TRAIN_CRASH = new TrainCrashConfig(builder);
        TRAIN_DERAIL = new TrainDerailConfig(builder);
        TRAIN_LIFECYCLE = new TrainLifecycleConfig(builder);
        builder.pop();

        SPEC = builder.build();
    }
}
