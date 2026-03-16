package com.saunhardy.createsignalbox.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class WebhookConfig {
    public final ModConfigSpec.ConfigValue<String> webhookUrl;
    public final ModConfigSpec.BooleanValue useDiscordFormat;
    public final ModConfigSpec.IntValue timeoutMs;
    public final ModConfigSpec.ConfigValue<String> serverName;

    WebhookConfig(ModConfigSpec.Builder builder) {
        builder.push("webhook");

        webhookUrl = builder
                .comment("The URL to send notifications to (Discord webhook URL or custom API endpoint)")
                .define("webhookUrl", "");

        useDiscordFormat = builder
                .comment("When true, formats payloads as Discord webhook embeds. When false, sends raw JSON.")
                .define("useDiscordFormat", true);

        timeoutMs = builder
                .comment("HTTP request timeout in milliseconds")
                .defineInRange("timeoutMs", 5000, 1000, 30000);

        serverName = builder
                .comment("Optional server name included in notifications (leave empty to omit)")
                .define("serverName", "");

        builder.pop();
    }
}
