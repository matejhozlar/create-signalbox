package com.saunhardy.createsignalbox.events;

import com.google.gson.Gson;
import com.saunhardy.createsignalbox.config.SignalboxConfig;
import com.saunhardy.createsignalbox.webhook.WebhookSender;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TrainDerailHandler {
    private static final Gson GSON = new Gson();
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();
    private static final Set<UUID> CRASHED = ConcurrentHashMap.newKeySet();

    public static void markCrashed(UUID trainId) {
        CRASHED.add(trainId);
    }

    public static boolean consumeCrashed(UUID trainId) {
        return CRASHED.remove(trainId);
    }

    public static void reportDerailment(UUID trainId, String trainName, double speed,
                                        int carriageCount, @Nullable double[] position,
                                        @Nullable String dimension,
                                        @Nullable UUID owner, @Nullable String ownerName) {
        if (!SignalboxConfig.TRAIN_DERAIL.enabled.get()) return;

        String webhookUrl = SignalboxConfig.TRAIN_DERAIL.webhookUrl.get();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            webhookUrl = SignalboxConfig.WEBHOOK.webhookUrl.get();
        }
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        int cooldown = SignalboxConfig.TRAIN_DERAIL.cooldownSeconds.get();
        if (cooldown > 0) {
            long now = System.currentTimeMillis();
            Long lastReport = COOLDOWNS.get(trainId);
            if (lastReport != null && (now - lastReport) < cooldown * 1000L) return;
            COOLDOWNS.put(trainId, now);
        }

        String json;
        if (SignalboxConfig.WEBHOOK.useDiscordFormat.get()) {
            json = buildDiscordPayload(trainName, speed, carriageCount, position, dimension,
                    owner, ownerName);
        } else {
            json = buildRawPayload(trainId, trainName, speed, carriageCount, position, dimension,
                    owner, ownerName);
        }

        WebhookSender.send(webhookUrl, json, String.format("train derailed: %s (%s)", trainName, trainId));
    }

    private static String buildDiscordPayload(String trainName, double speed,
                                              int carriageCount, @Nullable double[] position,
                                              @Nullable String dimension,
                                              @Nullable UUID owner, @Nullable String ownerName) {
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", "Train Derailed");
        embed.put("color", 0xFEE75C);

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(inlineField("Train", trainName));
        fields.add(inlineField("Speed", String.format("%.2f m/t", speed)));
        fields.add(inlineField("Carriages", String.valueOf(carriageCount)));

        if (dimension != null) {
            fields.add(inlineField("Dimension", dimension));
        }

        if (position != null) {
            fields.add(inlineField("Position",
                    String.format("%.0f, %.0f, %.0f", position[0], position[1], position[2])));
        }

        if (owner != null) {
            String ownerText = ownerName != null ? ownerName : owner.toString();
            fields.add(inlineField("Owner", ownerText));
        }

        embed.put("fields", fields);
        embed.put("timestamp", DateTimeFormatter.ISO_INSTANT.format(
                Instant.now().atOffset(ZoneOffset.UTC)));

        String serverName = SignalboxConfig.WEBHOOK.serverName.get();
        if (serverName != null && !serverName.isBlank()) {
            Map<String, String> footer = new LinkedHashMap<>();
            footer.put("text", serverName);
            embed.put("footer", footer);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("embeds", List.of(embed));
        return GSON.toJson(payload);
    }

    private static String buildRawPayload(UUID trainId, String trainName, double speed,
                                          int carriageCount, @Nullable double[] position,
                                          @Nullable String dimension,
                                          @Nullable UUID owner, @Nullable String ownerName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "train_derail");
        payload.put("trainId", trainId.toString());
        payload.put("trainName", trainName);
        payload.put("speed", speed);
        payload.put("carriageCount", carriageCount);
        payload.put("timestamp", System.currentTimeMillis());

        if (position != null) {
            Map<String, Double> pos = new LinkedHashMap<>();
            pos.put("x", position[0]);
            pos.put("y", position[1]);
            pos.put("z", position[2]);
            payload.put("position", pos);
        }

        if (dimension != null) {
            payload.put("dimension", dimension);
        }

        if (owner != null) {
            payload.put("owner", owner.toString());
            if (ownerName != null) payload.put("ownerName", ownerName);
        }

        String serverName = SignalboxConfig.WEBHOOK.serverName.get();
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
