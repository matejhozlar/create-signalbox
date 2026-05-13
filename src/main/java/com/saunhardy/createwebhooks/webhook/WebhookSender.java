package com.saunhardy.createwebhooks.webhook;

import com.mojang.logging.LogUtils;
import com.saunhardy.createwebhooks.config.WebhooksConfig;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class WebhookSender {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "CreateWebhooks-Webhook");
        t.setDaemon(true);
        return t;
    });

    public static void send(String webhookUrl, String json, String eventDescription) {
        send(webhookUrl, json, eventDescription, null);
    }

    public static void send(String webhookUrl, String json, String eventDescription,
                            @Nullable Consumer<Boolean> callback) {
        EXECUTOR.submit(() -> {
            boolean success = false;
            try {
                URL url = URI.create(webhookUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                try {
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(WebhooksConfig.WEBHOOK.timeoutMs.get());
                    conn.setReadTimeout(WebhooksConfig.WEBHOOK.timeoutMs.get());

                    try (var os = conn.getOutputStream()) {
                        os.write(json.getBytes(StandardCharsets.UTF_8));
                    }

                    int responseCode = conn.getResponseCode();
                    if (responseCode >= 200 && responseCode < 300) {
                        LOGGER.info("Webhook sent: {}", eventDescription);
                        success = true;
                    } else {
                        LOGGER.warn("Webhook failed with status {}: {}", responseCode, eventDescription);
                    }
                } finally {
                    conn.disconnect();
                }
            } catch (Exception e) {
                LOGGER.error("Failed to send webhook ({}): {}", eventDescription, e.getMessage());
            }

            if (callback != null) {
                callback.accept(success);
            }
        });
    }
}
