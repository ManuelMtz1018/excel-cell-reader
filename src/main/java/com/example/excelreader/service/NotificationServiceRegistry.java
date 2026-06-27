package com.example.excelreader.service;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class NotificationServiceRegistry {

    private final Map<MessageChannel, NotificationService> services = new EnumMap<>(MessageChannel.class);

    public NotificationServiceRegistry(Collection<NotificationService> notificationServices) {
        for (NotificationService notificationService : notificationServices) {
            services.put(notificationService.channel(), notificationService);
        }
    }

    public NotificationService get(MessageChannel channel) {
        NotificationService notificationService = services.get(channel);
        if (notificationService == null) {
            throw new IllegalArgumentException("No hay servicio configurado para " + channel + ".");
        }
        return notificationService;
    }
}
