package com.saunhardy.createsignalbox.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class TrainCrashConfig {
    public final ModConfigSpec.BooleanValue enabled;

    TrainCrashConfig(ModConfigSpec.Builder builder) {
        builder.push("trainCrash");

        enabled = builder
                .comment("Enable train crash notifications (requires Create mod)")
                .define("enabled", true);

        builder.pop();
    }
}
