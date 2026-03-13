package com.saunhardy.createsignalbox;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> WEBHOOK_URL = BUILDER
            .comment("The URL to send notifications to (Discord webhook URL or custom API endpoint)")
            .define("webhookUrl", "");

    public static final ModConfigSpec.BooleanValue USE_DISCORD_FORMAT = BUILDER
            .comment("When true, formats payloads as Discord webhook embeds. When false, sends raw JSON.")
            .define("useDiscordFormat", true);

    public static final ModConfigSpec.IntValue TIMEOUT_MS = BUILDER
            .comment("HTTP request timeout in milliseconds")
            .defineInRange("timeoutMs", 5000, 1000, 30000);

    public static final ModConfigSpec.ConfigValue<String> SERVER_NAME = BUILDER
            .comment("Optional server name included in notifications (leave empty to omit)")
            .define("serverName", "");

    public static final ModConfigSpec.BooleanValue TRAIN_CRASH_ENABLED = BUILDER
            .comment("Enable train crash notifications (requires Create mod)")
            .define("trainCrashEnabled", true);

    static final ModConfigSpec SPEC = BUILDER.build();
}
