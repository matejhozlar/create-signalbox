package com.saunhardy.createwebhooks.commands;

import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.saunhardy.createwebhooks.config.WebhooksConfig;
import com.saunhardy.createwebhooks.webhook.WebhookSender;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WebhooksCommand {
    private static final Gson GSON = new Gson();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("webhooks")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("test")
                        .executes(WebhooksCommand::executeTest)));
    }

    private static int executeTest(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        String webhookUrl = WebhooksConfig.WEBHOOK.webhookUrl.get();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            source.sendFailure(Component.literal("No webhook URL configured. Set one in the config first."));
            return 0;
        }

        String json;
        if (WebhooksConfig.WEBHOOK.useDiscordFormat.get()) {
            json = buildTestDiscordPayload();
        } else {
            json = buildTestRawPayload();
        }

        source.sendSuccess(() -> Component.literal("Sending test webhook..."), false);

        WebhookSender.send(webhookUrl, json, "test notification", success -> {
            source.getServer().execute(() -> {
                if (success) {
                    source.sendSuccess(() -> Component.literal("Test webhook sent successfully!"), false);
                } else {
                    source.sendFailure(Component.literal("Test webhook failed. Check server logs for details."));
                }
            });
        });

        return 1;
    }

    private static String buildTestDiscordPayload() {
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", "Test Notification");
        embed.put("description", "This is a test notification from Create: Webhooks. If you see this, your webhook is configured correctly!");
        embed.put("color", 0x5865F2);
        embed.put("timestamp", DateTimeFormatter.ISO_INSTANT.format(
                Instant.now().atOffset(ZoneOffset.UTC)));

        String serverName = WebhooksConfig.WEBHOOK.serverName.get();
        if (serverName != null && !serverName.isBlank()) {
            Map<String, String> footer = new LinkedHashMap<>();
            footer.put("text", serverName);
            embed.put("footer", footer);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("embeds", List.of(embed));
        return GSON.toJson(payload);
    }

    private static String buildTestRawPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "test");
        payload.put("message", "This is a test notification from Create: Webhooks.");
        payload.put("timestamp", System.currentTimeMillis());

        String serverName = WebhooksConfig.WEBHOOK.serverName.get();
        if (serverName != null && !serverName.isBlank()) {
            payload.put("serverName", serverName);
        }

        return GSON.toJson(payload);
    }
}
