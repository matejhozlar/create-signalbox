package com.saunhardy.createwebhooks.events;

import com.google.gson.Gson;
import com.saunhardy.createwebhooks.config.WebhooksConfig;
import com.saunhardy.createwebhooks.webhook.WebhookSender;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TrainLifecycleHandler {
    private static final Gson GSON = new Gson();

    public static void reportCreated(UUID trainId, String trainName, int carriageCount,
                                     @Nullable UUID owner, @Nullable String ownerName) {
        if (!WebhooksConfig.TRAIN_LIFECYCLE.creationEnabled.get()) return;

        String webhookUrl = resolveWebhookUrl();
        if (webhookUrl == null) return;

        String json;
        if (WebhooksConfig.WEBHOOK.useDiscordFormat.get()) {
            json = buildDiscordPayload("Train Created", 0x57F287, trainId, trainName,
                    carriageCount, owner, ownerName);
        } else {
            json = buildRawPayload("train_created", trainId, trainName,
                    carriageCount, owner, ownerName);
        }

        WebhookSender.send(webhookUrl, json, String.format("train created: %s (%s)", trainName, trainId));
    }

    public static void reportRemoved(UUID trainId, String trainName,
                                     @Nullable UUID owner, @Nullable String ownerName) {
        if (!WebhooksConfig.TRAIN_LIFECYCLE.deletionEnabled.get()) return;

        String webhookUrl = resolveWebhookUrl();
        if (webhookUrl == null) return;

        String json;
        if (WebhooksConfig.WEBHOOK.useDiscordFormat.get()) {
            json = buildDiscordPayload("Train Removed", 0xED4245, trainId, trainName,
                    0, owner, ownerName);
        } else {
            json = buildRawPayload("train_removed", trainId, trainName,
                    0, owner, ownerName);
        }

        WebhookSender.send(webhookUrl, json, String.format("train removed: %s (%s)", trainName, trainId));
    }

    @Nullable
    private static String resolveWebhookUrl() {
        String webhookUrl = WebhooksConfig.TRAIN_LIFECYCLE.webhookUrl.get();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            webhookUrl = WebhooksConfig.WEBHOOK.webhookUrl.get();
        }
        if (webhookUrl == null || webhookUrl.isBlank()) return null;
        return webhookUrl;
    }

    private static String buildDiscordPayload(String title, int color, UUID trainId,
                                              String trainName, int carriageCount,
                                              @Nullable UUID owner, @Nullable String ownerName) {
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", title);
        embed.put("color", color);

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(inlineField("Train", trainName));

        if (owner != null) {
            String ownerText = ownerName != null ? ownerName : owner.toString();
            fields.add(inlineField("Owner", ownerText));
        }

        if (carriageCount > 0) {
            fields.add(inlineField("Carriages", String.valueOf(carriageCount)));
        }

        embed.put("fields", fields);
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

    private static String buildRawPayload(String event, UUID trainId, String trainName,
                                          int carriageCount,
                                          @Nullable UUID owner, @Nullable String ownerName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", event);
        payload.put("trainId", trainId.toString());
        payload.put("trainName", trainName);
        payload.put("timestamp", System.currentTimeMillis());

        if (carriageCount > 0) {
            payload.put("carriageCount", carriageCount);
        }

        if (owner != null) {
            payload.put("owner", owner.toString());
            if (ownerName != null) payload.put("ownerName", ownerName);
        }

        String serverName = WebhooksConfig.WEBHOOK.serverName.get();
        if (serverName != null && !serverName.isBlank()) {
            payload.put("serverName", serverName);
        }

        return GSON.toJson(payload);
    }

    private static Map<String, Object> inlineField(String name, String value) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("name", name);
        f.put("value", value);
        f.put("inline", true);
        return f;
    }
}
