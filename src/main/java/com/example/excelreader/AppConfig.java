package com.example.excelreader;

import com.example.excelreader.service.MessageChannel;

public record AppConfig(
        ExcelConfig excel,
        MonitorConfig monitor,
        MessagingConfig messaging,
        TelegramConfig telegram,
        WhatsAppConfig whatsApp
) {

    public record ExcelConfig(String filePath, String cellReference, CellDataType dataType) {
    }

    public record MonitorConfig(int intervalSeconds) {
    }

    public record MessagingConfig(MessageChannel channel) {
    }

    public record TelegramConfig(String botToken, String chatId) {
    }

    public record WhatsAppConfig(String accessToken, String phoneNumberId, String to, String graphApiVersion) {
    }
}
