package com.example.excelreader.service;

public enum MessageChannel {
    TELEGRAM("Telegram"),
    WHATSAPP("WhatsApp");

    private final String displayName;

    MessageChannel(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
