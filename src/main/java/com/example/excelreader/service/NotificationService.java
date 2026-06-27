package com.example.excelreader.service;

public interface NotificationService {

    MessageChannel channel();

    void sendMessage(String message) throws Exception;
}
