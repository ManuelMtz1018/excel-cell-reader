package com.example.excelreader.service;

import com.example.excelreader.AppConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

public class WhatsAppNotificationService implements NotificationService {

    private static final Logger log = LogManager.getLogger(WhatsAppNotificationService.class);
    private final AppConfig.WhatsAppConfig config;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public WhatsAppNotificationService(AppConfig.WhatsAppConfig config) {
        this.config = config;
    }

    @Override
    public MessageChannel channel() {
        return MessageChannel.WHATSAPP;
    }

    @Override
    public void sendMessage(String message) throws IOException, InterruptedException {
        validateConfig();

        String url = "https://graph.facebook.com/" + config.graphApiVersion().trim()
                + "/" + config.phoneNumberId().trim() + "/messages";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + config.accessToken().trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildPayload(message)))
                .build();

        log.info("Enviando mensaje a WhatsApp para {}", config.to());
        log.debug("Mensaje enviado a WhatsApp: {}", message);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("WhatsApp respondio con estado {}", response.statusCode());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("WhatsApp respondio con estado " + response.statusCode() + ".");
        }
    }

    private String buildPayload(String message) {
        return "{"
                + "\"messaging_product\":\"whatsapp\","
                + "\"to\":\"" + escapeJson(config.to().trim()) + "\","
                + "\"timestamp\":\"" + Instant.now().getEpochSecond() + "\","
                + "\"type\":\"text\","
                + "\"text\":{\"body\":\"" + escapeJson(message) + "\"}"
                + "}";
    }

    private void validateConfig() {
        if (isBlank(config.accessToken())) {
            throw new IllegalArgumentException("Configura whatsapp.access-token en application.properties.");
        }
        if (isBlank(config.phoneNumberId())) {
            throw new IllegalArgumentException("Configura whatsapp.phone-number-id en application.properties.");
        }
        if (isBlank(config.to())) {
            throw new IllegalArgumentException("Configura whatsapp.to en application.properties.");
        }
        if (isBlank(config.graphApiVersion())) {
            throw new IllegalArgumentException("Configura whatsapp.graph-api-version en application.properties.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (current < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) current));
                    } else {
                        escaped.append(current);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
