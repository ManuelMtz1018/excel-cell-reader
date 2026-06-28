package com.example.excelreader.service;

import com.example.excelreader.CellDataType;
import com.example.excelreader.models.*;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class CellMonitorService {

    private final ExcelReader excelReader;
    private final NotificationServiceRegistry notificationServiceRegistry;
    private final Map<String, Map<String, RowSnapshot>> previousValuesByDocument = new LinkedHashMap<>();
    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> monitorTask;

    public CellMonitorService(ExcelReader excelReader,
                              NotificationServiceRegistry notificationServiceRegistry) {
        this.excelReader = excelReader;
        this.notificationServiceRegistry = notificationServiceRegistry;
    }

    public void start(MonitorConfig config, Consumer<String> onMessage, Consumer<Exception> onError) {
        stop();
        previousValuesByDocument.clear();
        executorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "excel-column-monitor");
            thread.setDaemon(true);
            return thread;
        });

        monitorTask = executorService.scheduleAtFixedRate(() -> {
            try {
                checkDocuments(config, onMessage);
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
        previousValuesByDocument.clear();
    }

    private void checkDocuments(MonitorConfig config, Consumer<String> onMessage) throws Exception {
        StringBuilder result = new StringBuilder();
        NotificationService notificationService = notificationServiceRegistry.get(config.messageChannel());

        for (DocumentMonitorConfig documentConfig : config.documents()) {
            Map<String, RowSnapshot> currentValues = readDocumentValues(documentConfig);
            Map<String, RowSnapshot> previousValues = previousValuesByDocument.get(documentConfig.documentKey());

            if (previousValues == null) {
                previousValuesByDocument.put(documentConfig.documentKey(), currentValues);
                appendLine(result, "Monitoreo iniciado para " + documentConfig.displayName()
                        + ". Celdas leidas: " + currentValues.size() + ".");
                continue;
            }

            List<CellChange> changes = findChanges(previousValues, currentValues);
            previousValuesByDocument.put(documentConfig.documentKey(), currentValues);

            if (changes.isEmpty()) {
                appendLine(result, "Sin cambios en " + documentConfig.displayName() + ".");
                continue;
            }

            String message = buildChangeMessage(documentConfig, changes);
            notificationService.sendMessage(message);
            appendLine(result, message + System.lineSeparator()
                    + "Mensaje enviado por " + config.messageChannel() + ".");
        }

        onMessage.accept(result.toString().trim());
    }

    private Map<String, RowSnapshot> readDocumentValues(DocumentMonitorConfig documentConfig) throws Exception {
        List<ExcelReader.ColumnCellValue> values = excelReader.readColumnRange(
                documentConfig.excelFile(),
                documentConfig.columnReference(),
                documentConfig.startRow(),
                documentConfig.endRow(),
                documentConfig.expectedType()
        );

        Map<Integer, ExtraColumnValues> extraValuesByRow = readExtraValues(documentConfig);
        Map<String, RowSnapshot> valuesByCell = new LinkedHashMap<>();
        for (ExcelReader.ColumnCellValue value : values) {
            int rowNumber = extractRowNumber(value.cellReference());
            ExtraColumnValues extraValues = extraValuesByRow.getOrDefault(rowNumber, ExtraColumnValues.empty());
            valuesByCell.put(value.cellReference(), new RowSnapshot(value.cellReference(), value.value(), extraValues.values()));
        }
        return valuesByCell;
    }

    private Map<Integer, ExtraColumnValues> readExtraValues(DocumentMonitorConfig documentConfig) throws Exception {
        Map<Integer, ExtraColumnValues> extraValuesByRow = new LinkedHashMap<>();
        addExtraValues(extraValuesByRow, documentConfig.extraColumn1(), documentConfig.extraLabel1(), documentConfig);
        addExtraValues(extraValuesByRow, documentConfig.extraColumn2(), documentConfig.extraLabel2(), documentConfig);
        return extraValuesByRow;
    }

    private void addExtraValues(Map<Integer, ExtraColumnValues> extraValuesByRow,
                                String columnReference,
                                String label,
                                DocumentMonitorConfig documentConfig) throws Exception {
        if (isBlank(columnReference)) {
            return;
        }

        List<ExcelReader.ColumnCellValue> values = excelReader.readColumnRangeAsText(
                documentConfig.excelFile(),
                columnReference,
                documentConfig.startRow(),
                documentConfig.endRow()
        );
        String readableLabel = isBlank(label) ? columnReference.trim().toUpperCase() : label.trim();
        for (ExcelReader.ColumnCellValue value : values) {
            int rowNumber = extractRowNumber(value.cellReference());
            extraValuesByRow.computeIfAbsent(rowNumber, row -> new ExtraColumnValues())
                    .put(readableLabel, value.value());
        }
    }

    private List<CellChange> findChanges(Map<String, RowSnapshot> previousValues, Map<String, RowSnapshot> currentValues) {
        return currentValues.entrySet().stream()
                .filter(entry -> previousValues.get(entry.getKey()) == null
                        || !Objects.equals(previousValues.get(entry.getKey()).value(), entry.getValue().value()))
                .map(entry -> {
                    RowSnapshot previousValue = previousValues.get(entry.getKey());
                    return new CellChange(
                            entry.getKey(),
                            previousValue == null ? "" : previousValue.value(),
                            entry.getValue().value(),
                            entry.getValue().extraValues()
                    );
                })
                .toList();
    }

    private String buildChangeMessage(DocumentMonitorConfig documentConfig, List<CellChange> changes) {
        StringBuilder message = new StringBuilder();
        appendLine(message, "Archivo: " + documentConfig.displayName());
        appendLine(message, "Columna " + documentConfig.columnReference().trim().toUpperCase()
                + " actualizada. Cambios detectados: " + changes.size() + ".");
        appendLine(message, "\n");
        for (CellChange change : changes) {
            appendLine(message, change.cellReference() + ": " + readableValue(change.oldValue())
                    + " -> " + readableValue(change.newValue()));
            for (Map.Entry<String, String> extraValue : change.extraValues().entrySet()) {
                appendLine(message, extraValue.getKey() + ": " + readableValue(extraValue.getValue()));
            }
            appendLine(message, "\n");
        }
        return message.toString().trim();
    }

    private int extractRowNumber(String cellReference) {
        return Integer.parseInt(cellReference.replaceAll("[^0-9]", ""));
    }

    private String readableValue(String value) {
        return value == null || value.isBlank() ? "(vacio)" : value;
    }

    private void appendLine(StringBuilder builder, String value) {
        if (!builder.isEmpty()) {
            builder.append(System.lineSeparator());
        }
        builder.append(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /*
    private static class ExtraColumnValues {

        private final Map<String, String> values = new LinkedHashMap<>();

        static ExtraColumnValues empty() {
            return new ExtraColumnValues();
        }

        void put(String label, String value) {
            values.put(label, value);
        }

        Map<String, String> values() {
            return new LinkedHashMap<>(values);
        }
    }*/
}
