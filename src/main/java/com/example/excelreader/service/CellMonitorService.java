package com.example.excelreader.service;

import com.example.excelreader.CellDataType;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class CellMonitorService {

    private final ExcelReader excelReader;
    private final NotificationServiceRegistry notificationServiceRegistry;
    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> monitorTask;
    private String previousValue;

    public CellMonitorService(ExcelReader excelReader,
                              NotificationServiceRegistry notificationServiceRegistry) {
        this.excelReader = excelReader;
        this.notificationServiceRegistry = notificationServiceRegistry;
    }

    public void start(MonitorConfig config, Consumer<String> onMessage, Consumer<Exception> onError) {
        stop();
        previousValue = null;
        executorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "excel-cell-monitor");
            thread.setDaemon(true);
            return thread;
        });

        monitorTask = executorService.scheduleAtFixedRate(() -> {
            try {
                checkCell(config, onMessage);
            } catch (Exception exception) {
                onError.accept(exception);
            }
        }, 0, config.intervalSeconds(), TimeUnit.SECONDS);
    }

    public void stop() {
        if (monitorTask != null) {
            monitorTask.cancel(true);
            monitorTask = null;
        }
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        previousValue = null;
    }

    private void checkCell(MonitorConfig config, Consumer<String> onMessage) throws Exception {
        String currentValue = excelReader.readCell(
                config.excelFile(),
                config.cellReference(),
                config.expectedType()
        );

        if (previousValue == null) {
            previousValue = currentValue;
            onMessage.accept("Monitoreo iniciado. Valor actual: " + currentValue);
            return;
        }

        if (!Objects.equals(currentValue, previousValue)) {
            String oldValue = previousValue;
            previousValue = currentValue;
            String message = "La celda " + config.cellReference().trim().toUpperCase()
                    + " se actualizo." + System.lineSeparator()
                    + "Valor anterior: " + oldValue + System.lineSeparator()
                    + "Valor nuevo: " + currentValue;

            NotificationService notificationService = notificationServiceRegistry.get(config.messageChannel());
            notificationService.sendMessage(message);
            onMessage.accept(message + System.lineSeparator()
                    + "Mensaje enviado por " + config.messageChannel() + ".");
        }
    }

    public record MonitorConfig(
            File excelFile,
            String cellReference,
            CellDataType expectedType,
            int intervalSeconds,
            MessageChannel messageChannel
    ) {
    }
}
