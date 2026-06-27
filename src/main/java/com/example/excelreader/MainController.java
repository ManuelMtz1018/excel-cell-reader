package com.example.excelreader;

import com.example.excelreader.service.AppConfigService;
import com.example.excelreader.service.CellMonitorService;
import com.example.excelreader.service.ExcelReader;
import com.example.excelreader.service.ExcelReaderService;
import com.example.excelreader.service.MessageChannel;
import com.example.excelreader.service.NotificationServiceRegistry;
import com.example.excelreader.service.TelegramNotificationService;
import com.example.excelreader.service.WhatsAppNotificationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.List;

public class MainController {

    @FXML
    private TextField filePathField;

    @FXML
    private TextField cellReferenceField;

    @FXML
    private ComboBox<CellDataType> dataTypeComboBox;

    @FXML
    private TextField intervalField;

    @FXML
    private ComboBox<MessageChannel> messageChannelComboBox;

    @FXML
    private Button startMonitorButton;

    @FXML
    private Button stopMonitorButton;

    @FXML
    private TextArea resultArea;

    @FXML
    private Label statusLabel;

    private final ExcelReader excelReaderService;
    private final AppConfigService appConfigService;
    private CellMonitorService cellMonitorService;
    private File selectedFile;

    public MainController() {
        excelReaderService = new ExcelReaderService();
        appConfigService = new AppConfigService();
    }
    @FXML
    private void initialize() {
        dataTypeComboBox.getItems().setAll(CellDataType.values());
        messageChannelComboBox.getItems().setAll(MessageChannel.values());
        stopMonitorButton.setDisable(true);
        AppConfig config = appConfigService.load();
        cellMonitorService = new CellMonitorService(
                excelReaderService,
                new NotificationServiceRegistry(List.of(
                        new TelegramNotificationService(config.telegram()),
                        new WhatsAppNotificationService(config.whatsApp())
                ))
        );
        applyConfig(config);
        resultArea.setText("Selecciona un archivo Excel, escribe una celda y elige el tipo de dato esperado.");
    }

    private void applyConfig(AppConfig config) {
        if (config.excel().filePath() != null && !config.excel().filePath().isBlank()) {
            selectedFile = new File(config.excel().filePath());
            filePathField.setText(selectedFile.getAbsolutePath());
        }
        cellReferenceField.setText(config.excel().cellReference());
        dataTypeComboBox.setValue(config.excel().dataType());
        intervalField.setText(String.valueOf(config.monitor().intervalSeconds()));
        messageChannelComboBox.setValue(config.messaging().channel());
    }

    @FXML
    private void onSelectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo Excel");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos Excel (*.xlsx, *.xls)", "*.xlsx", "*.xls")
        );

        Window window = filePathField.getScene().getWindow();
        File file = fileChooser.showOpenDialog(window);
        if (file != null) {
            selectedFile = file;
            filePathField.setText(file.getAbsolutePath());
            showInfo("Archivo seleccionado correctamente.");
        }
    }

    @FXML
    private void onReadCell() {
        try {
            validateForm();

            String value = excelReaderService.readCell(
                    selectedFile,
                    cellReferenceField.getText(),
                    dataTypeComboBox.getValue()
            );

            String cellReference = cellReferenceField.getText().trim().toUpperCase();
            resultArea.setText("Celda: " + cellReference + System.lineSeparator()
                    + "Tipo esperado: " + dataTypeComboBox.getValue() + System.lineSeparator()
                    + "Valor leido: " + value);
            showInfo("Lectura completada.");
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void onStartMonitoring() {
        try {
            validateForm();
            CellMonitorService.MonitorConfig config = new CellMonitorService.MonitorConfig(
                    selectedFile,
                    cellReferenceField.getText(),
                    dataTypeComboBox.getValue(),
                    parseIntervalSeconds(),
                    readMessageChannel()
            );

            cellMonitorService.start(
                    config,
                    message -> Platform.runLater(() -> {
                        resultArea.setText(message);
                        showInfo("Monitoreando cada " + config.intervalSeconds() + " segundos.");
                    }),
                    exception -> Platform.runLater(() -> showError(exception.getMessage()))
            );

            startMonitorButton.setDisable(true);
            stopMonitorButton.setDisable(false);
            showInfo("Monitoreo iniciado.");
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void onStopMonitoring() {
        stopMonitoring();
        showInfo("Monitoreo detenido.");
    }

    public void stopMonitoring() {
        cellMonitorService.stop();
        if (startMonitorButton != null && stopMonitorButton != null) {
            startMonitorButton.setDisable(false);
            stopMonitorButton.setDisable(true);
        }
    }

    private void validateForm() {
        if (selectedFile == null) {
            throw new IllegalArgumentException("Selecciona un archivo Excel antes de leer una celda.");
        }

        if (cellReferenceField.getText() == null || cellReferenceField.getText().isBlank()) {
            throw new IllegalArgumentException("Escribe una celda, por ejemplo A1, B2 o C10.");
        }

        // Reutiliza la validacion del servicio para evitar reglas duplicadas.
        excelReaderService.parseCellReference(cellReferenceField.getText());

        if (dataTypeComboBox.getValue() == null) {
            throw new IllegalArgumentException("Selecciona el tipo de dato esperado.");
        }
    }

    private int parseIntervalSeconds() {
        String value = readRequiredText(intervalField, "Escribe el intervalo de lectura en segundos.");
        try {
            int interval = Integer.parseInt(value);
            if (interval < 1) {
                throw new IllegalArgumentException("El intervalo debe ser de al menos 1 segundo.");
            }
            return interval;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("El intervalo debe ser un numero entero.");
        }
    }

    private MessageChannel readMessageChannel() {
        MessageChannel messageChannel = messageChannelComboBox.getValue();
        if (messageChannel == null) {
            throw new IllegalArgumentException("Selecciona si el mensaje se enviara por Telegram o WhatsApp.");
        }
        return messageChannel;
    }

    private String readRequiredText(TextField textField, String errorMessage) {
        String value = textField.getText();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }

    private void showInfo(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().remove("error-text");
        resultArea.getStyleClass().remove("error-area");
    }

    private void showError(String message) {
        String readableMessage = message == null ? "Ocurrio un error inesperado." : message;
        statusLabel.setText("Error");
        if (!statusLabel.getStyleClass().contains("error-text")) {
            statusLabel.getStyleClass().add("error-text");
        }
        if (!resultArea.getStyleClass().contains("error-area")) {
            resultArea.getStyleClass().add("error-area");
        }
        resultArea.setText(readableMessage);
    }
}
