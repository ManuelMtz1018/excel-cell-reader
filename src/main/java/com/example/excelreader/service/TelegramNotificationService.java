package com.example.excelreader.service;

import com.example.excelreader.AppConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class TelegramNotificationService implements NotificationService {

    private static final Logger log = LogManager.getLogger(TelegramNotificationService.class);
    private final AppConfig.TelegramConfig config;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public TelegramNotificationService(AppConfig.TelegramConfig config) {
        this.config = config;
    }

    @Override
    public MessageChannel channel() {
        return MessageChannel.TELEGRAM;
    }

    @Override
    public void sendMessage(String message) throws IOException, InterruptedException {
        validateConfig();

        String body = "chat_id=" + encode(config.chatId().trim()) + "&text=" + encode(message);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.telegram.org/bot" + config.botToken().trim() + "/sendMessage"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        log.info("Enviando mensaje a Telegram para chat id {}", config.chatId());
        log.debug("Mensaje enviado a Telegram: {}", message);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("Telegram respondio con estado {}", response.statusCode());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Telegram respondio con estado " + response.statusCode() + ".");
        }
    }

    private void validateConfig() {
        if (isBlank(config.botToken())) {
            throw new IllegalArgumentException("Configura telegram.bot.token en application.properties.");
        }
        if (isBlank(config.chatId())) {
            throw new IllegalArgumentException("Configura telegram.chat.id en application.properties.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
