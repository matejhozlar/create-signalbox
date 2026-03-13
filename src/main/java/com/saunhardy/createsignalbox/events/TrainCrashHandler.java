package com.saunhardy.createsignalbox.events;

import com.google.gson.Gson;
import com.mojang.logging.LogUtils;
import com.saunhardy.createsignalbox.Config;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrainCrashHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "Signalbox-Webhook");
        t.setDaemon(true);
        return t;
    });

    public record PlayerInfo(UUID uuid, @Nullable String name, boolean isDriver) {}

    public static void reportCrash(UUID trainId, String trainName, double speed,
                                   int carriageCount, double[] position, String dimension,
                                   @Nullable UUID owner, @Nullable String ownerName,
                                   @Nullable UUID driverUuid,
                                   List<PlayerInfo> passengers,
                                   @Nullable UUID backwardsDriverUuid,
                                   @Nullable String backwardsDriverName) {
        if (!Config.TRAIN_CRASH_ENABLED.get()) return;

        String webhookUrl = Config.WEBHOOK_URL.get();
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        EXECUTOR.submit(() -> {
            try {
                String json;
                if (Config.USE_DISCORD_FORMAT.get()) {
                    json = buildDiscordPayload(trainId, trainName, speed, carriageCount,
                            position, dimension, owner, ownerName, driverUuid, passengers,
                            backwardsDriverUuid, backwardsDriverName);
                } else {
                    json = buildRawPayload(trainId, trainName, speed, carriageCount,
                            position, dimension, owner, ownerName, driverUuid, passengers,
                            backwardsDriverUuid, backwardsDriverName);
                }

                URL url = URI.create(webhookUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                try {
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(Config.TIMEOUT_MS.get());
                    conn.setReadTimeout(Config.TIMEOUT_MS.get());

                    try (var os = conn.getOutputStream()) {
                        os.write(json.getBytes());
                    }

                    int responseCode = conn.getResponseCode();
                    if (responseCode >= 200 && responseCode < 300) {
                        LOGGER.info("Train crash reported: {} ({})", trainName, trainId);
                    } else {
                        LOGGER.warn("Train crash report failed with status {}", responseCode);
                    }
                } finally {
                    conn.disconnect();
                }
            } catch (Exception e) {
                LOGGER.error("Failed to report train crash: {}", e.getMessage());
            }
        });
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

        // Add footer with server name if configured
        String serverName = Config.SERVER_NAME.get();
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

        String serverName = Config.SERVER_NAME.get();
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
