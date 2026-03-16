package com.saunhardy.createsignalbox.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class TrainCrashConfig {
    public final ModConfigSpec.BooleanValue enabled;
    public final ModConfigSpec.IntValue cooldownSeconds;

    TrainCrashConfig(ModConfigSpec.Builder builder) {
        builder.push("trainCrash");

        enabled = builder
                .comment("Enable train crash notifications (requires Create mod)")
                .define("enabled", true);

        cooldownSeconds = builder
                .comment("Cooldown in seconds before the same train can trigger another notification (0 to disable)")
                .defineInRange("cooldownSeconds", 60, 0, 3600);

        builder.pop();
    }
}
