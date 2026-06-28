package com.example.excelreader;

import com.example.excelreader.service.MessageChannel;

import java.util.List;

public record AppConfig(
        List<ExcelDocumentConfig> documents,
        MonitorConfig monitor,
        MessagingConfig messaging,
        TelegramConfig telegram,
        WhatsAppConfig whatsApp
) {

    public record ExcelDocumentConfig(
            String filePath,
            String columnReference,
            int startRow,
            int endRow,
            CellDataType dataType,
            String extraColumn1,
            String extraLabel1,
            String extraColumn2,
            String extraLabel2
    ) {
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
