package com.saunhardy.createwebhooks.events;

import com.google.gson.Gson;
import com.saunhardy.createwebhooks.config.WebhooksConfig;
import com.saunhardy.createwebhooks.webhook.WebhookSender;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TrainCrashHandler {
    private static final Gson GSON = new Gson();
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();

    public record PlayerInfo(UUID uuid, @Nullable String name, boolean isDriver) {}

    public static void reportCrash(UUID trainId, String trainName, double speed,
                                   int carriageCount, double[] position, String dimension,
                                   @Nullable UUID owner, @Nullable String ownerName,
                                   @Nullable UUID driverUuid,
                                   List<PlayerInfo> passengers,
                                   @Nullable UUID backwardsDriverUuid,
                                   @Nullable String backwardsDriverName) {
        if (!WebhooksConfig.TRAIN_CRASH.enabled.get()) return;

        String webhookUrl = WebhooksConfig.TRAIN_CRASH.webhookUrl.get();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            webhookUrl = WebhooksConfig.WEBHOOK.webhookUrl.get();
        }
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        int cooldown = WebhooksConfig.TRAIN_CRASH.cooldownSeconds.get();
        if (cooldown > 0) {
            long now = System.currentTimeMillis();
            Long lastReport = COOLDOWNS.get(trainId);
            if (lastReport != null && (now - lastReport) < cooldown * 1000L) return;
            COOLDOWNS.put(trainId, now);
        }

        String json;
        if (WebhooksConfig.WEBHOOK.useDiscordFormat.get()) {
            json = buildDiscordPayload(trainId, trainName, speed, carriageCount,
                    position, dimension, owner, ownerName, driverUuid, passengers,
                    backwardsDriverUuid, backwardsDriverName);
        } else {
            json = buildRawPayload(trainId, trainName, speed, carriageCount,
                    position, dimension, owner, ownerName, driverUuid, passengers,
                    backwardsDriverUuid, backwardsDriverName);
        }

        WebhookSender.send(webhookUrl, json, String.format("train crash: %s (%s)", trainName, trainId));
    }

    private static String buildDiscordPayload(UUID trainId, String trainName, double speed,
                                              int carriageCount, double[] position, String dimension,
                                              @Nullable UUID owner, @Nullable String ownerName,
                                              @Nullable UUID driverUuid,
                                              List<PlayerInfo> passengers,
                                              @Nullable UUID backwardsDriverUuid,
                                              @Nullable String backwardsDriverName) {
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", "Train Crash Report");
        embed.put("color", 0xFF0000);

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

        // Driver info
        PlayerInfo driver = null;
        for (PlayerInfo p : passengers) {
            if (p.isDriver()) {
                driver = p;
                break;
            }
        }
        if (driver != null) {
            String driverText = driver.name() != null ? driver.name() : driver.uuid().toString();
            fields.add(inlineField("Driver", driverText));
        }

        // Passenger list (non-driver)
        List<String> passengerNames = new ArrayList<>();
        for (PlayerInfo p : passengers) {
            if (!p.isDriver()) {
                passengerNames.add(p.name() != null ? p.name() : p.uuid().toString());
            }
        }
        if (!passengerNames.isEmpty()) {
            fields.add(field("Passengers", String.join(", ", passengerNames)));
        }

        if (backwardsDriverName != null) {
            fields.add(inlineField("Backwards Driver", backwardsDriverName));
        } else if (backwardsDriverUuid != null) {
            fields.add(inlineField("Backwards Driver", backwardsDriverUuid.toString()));
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

    private static String buildRawPayload(UUID trainId, String trainName, double speed,
                                          int carriageCount, double[] position, String dimension,
                                          @Nullable UUID owner, @Nullable String ownerName,
                                          @Nullable UUID driverUuid,
                                          List<PlayerInfo> passengers,
                                          @Nullable UUID backwardsDriverUuid,
                                          @Nullable String backwardsDriverName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "train_crash");
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

        if (driverUuid != null) {
            payload.put("driverUuid", driverUuid.toString());
        }

        List<Map<String, Object>> passengerList = new ArrayList<>();
        for (PlayerInfo p : passengers) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("uuid", p.uuid().toString());
            if (p.name() != null) entry.put("name", p.name());
            entry.put("isDriver", p.isDriver());
            passengerList.add(entry);
        }
        if (!passengerList.isEmpty()) {
            payload.put("passengers", passengerList);
        }

        if (backwardsDriverUuid != null) {
            Map<String, Object> bd = new LinkedHashMap<>();
            bd.put("uuid", backwardsDriverUuid.toString());
            if (backwardsDriverName != null) bd.put("name", backwardsDriverName);
            payload.put("backwardsDriver", bd);
        }

        String serverName = WebhooksConfig.WEBHOOK.serverName.get();
        if (serverName != null && !serverName.isBlank()) {
            payload.put("serverName", serverName);
        }

        return GSON.toJson(payload);
    }

    private static Map<String, Object> field(String name, String value) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("name", name);
        f.put("value", value);
        f.put("inline", false);
        return f;
    }

    private static Map<String, Object> inlineField(String name, String value) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("name", name);
        f.put("value", value);
        f.put("inline", true);
        return f;
    }
}
