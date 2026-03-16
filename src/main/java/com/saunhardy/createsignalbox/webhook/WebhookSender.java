package com.saunhardy.createsignalbox.webhook;

import com.mojang.logging.LogUtils;
import com.saunhardy.createsignalbox.config.SignalboxConfig;
import org.slf4j.Logger;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebhookSender {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "Signalbox-Webhook");
        t.setDaemon(true);
        return t;
    });

    public static void send(String webhookUrl, String json, String eventDescription) {
        EXECUTOR.submit(() -> {
            try {
                URL url = URI.create(webhookUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                try {
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(SignalboxConfig.WEBHOOK.timeoutMs.get());
                    conn.setReadTimeout(SignalboxConfig.WEBHOOK.timeoutMs.get());

                    try (var os = conn.getOutputStream()) {
                        os.write(json.getBytes(StandardCharsets.UTF_8));
                    }

                    int responseCode = conn.getResponseCode();
                    if (responseCode >= 200 && responseCode < 300) {
                        LOGGER.info("Webhook sent: {}", eventDescription);
                    } else {
                        LOGGER.warn("Webhook failed with status {}: {}", responseCode, eventDescription);
                    }
                } finally {
                    conn.disconnect();
                }
            } catch (Exception e) {
                LOGGER.error("Failed to send webhook ({}): {}", eventDescription, e.getMessage());
            }
        });
    }
}
