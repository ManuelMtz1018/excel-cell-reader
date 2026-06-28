package com.example.excelreader;

import com.example.excelreader.models.DocumentMonitorConfig;
import com.example.excelreader.models.MonitorConfig;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    private static final Logger log = LogManager.getLogger(MainController.class);
    @FXML
    private TextField filePathField1;
    @FXML
    private TextField columnReferenceField1;
    @FXML
    private TextField startRowField1;
    @FXML
    private TextField endRowField1;
    @FXML
    private ComboBox<CellDataType> dataTypeComboBox1;
    @FXML
    private TextField extraColumnField1A;
    @FXML
    private TextField extraLabelField1A;
    @FXML
    private TextField extraColumnField1B;
    @FXML
    private TextField extraLabelField1B;

    @FXML
    private TextField filePathField2;
    @FXML
    private TextField columnReferenceField2;
    @FXML
    private TextField startRowField2;
    @FXML
    private TextField endRowField2;
    @FXML
    private ComboBox<CellDataType> dataTypeComboBox2;
    @FXML
    private TextField extraColumnField2A;
    @FXML
    private TextField extraLabelField2A;
    @FXML
    private TextField extraColumnField2B;
    @FXML
    private TextField extraLabelField2B;

    @FXML
    private TextField filePathField3;
    @FXML
    private TextField columnReferenceField3;
    @FXML
    private TextField startRowField3;
    @FXML
    private TextField endRowField3;
    @FXML
    private ComboBox<CellDataType> dataTypeComboBox3;
    @FXML
    private TextField extraColumnField3A;
    @FXML
    private TextField extraLabelField3A;
    @FXML
    private TextField extraColumnField3B;
    @FXML
    private TextField extraLabelField3B;

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
    private final File[] selectedFiles = new File[3];
    private CellMonitorService cellMonitorService;

    public MainController() {
        excelReaderService = new ExcelReaderService();
        appConfigService = new AppConfigService();
    }

    @FXML
    private void initialize() {
        System.out.println("MainController initialized.");
        /*
        filePathField1.setEditable(true);
        filePathField2.setEditable(true);
        filePathField3.setEditable(true);
         */
        for (DocumentForm form : documentForms()) {
            form.dataTypeComboBox().getItems().setAll(CellDataType.values());
        }
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
        resultArea.setText("Configura una columna y un rango de filas para leer hasta 3 archivos Excel.");
    }

    private void applyConfig(AppConfig config) {
        List<DocumentForm> forms = documentForms();
        List<AppConfig.ExcelDocumentConfig> documents = config.documents();

        for (int index = 0; index < forms.size() && index < documents.size(); index++) {
            applyDocumentConfig(index, forms.get(index), documents.get(index));
        }
        intervalField.setText(String.valueOf(config.monitor().intervalSeconds()));
        messageChannelComboBox.setValue(config.messaging().channel());
    }

    private void applyDocumentConfig(int index, DocumentForm form, AppConfig.ExcelDocumentConfig config) {
        if (config.filePath() != null && !config.filePath().isBlank()) {
            selectedFiles[index] = new File(config.filePath());
            form.filePathField().setText(selectedFiles[index].getAbsolutePath());
            selectedFiles[index]=null;
        }
        form.columnReferenceField().setText(config.columnReference());
        form.startRowField().setText(String.valueOf(config.startRow()));
        form.endRowField().setText(String.valueOf(config.endRow()));
        form.dataTypeComboBox().setValue(config.dataType());
        form.extraColumnFieldA().setText(config.extraColumn1());
        form.extraLabelFieldA().setText(config.extraLabel1());
        form.extraColumnFieldB().setText(config.extraColumn2());
        form.extraLabelFieldB().setText(config.extraLabel2());
    }

    @FXML
    private void onSelectFile1() {
        selectFile(0);
    }

    @FXML
    private void onSelectFile2() {
        selectFile(1);
    }

    @FXML
    private void onSelectFile3() {
        selectFile(2);
    }

    private void selectFile(int index) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo Excel");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos Excel (*.xlsx, *.xls)", "*.xlsx", "*.xls")
        );

        Window window = documentForms().get(index).filePathField().getScene().getWindow();
        File file = fileChooser.showOpenDialog(window);
        if (file != null) {
            selectedFiles[index] = file;
            documentForms().get(index).filePathField().setText(file.getAbsolutePath());
            showInfo("Archivo " + (index + 1) + " seleccionado correctamente.");
        }
    }

    @FXML
    private void onReadColumns() {
        try {
            List<DocumentMonitorConfig> documents = readConfiguredDocuments();
            StringBuilder result = new StringBuilder();

            for (DocumentMonitorConfig document : documents) {
                List<ExcelReader.ColumnCellValue> values = excelReaderService.readColumnRange(
                        document.excelFile(),
                        document.columnReference(),
                        document.startRow(),
                        document.endRow(),
                        document.expectedType()
                );

                appendLine(result, "Archivo: " + document.excelFile().getName());
                appendLine(result, "Columna: " + document.columnReference().trim().toUpperCase()
                        + " | Filas: " + document.startRow() + "-" + document.endRow());
                for (ExcelReader.ColumnCellValue value : values) {
                    appendLine(result, value.cellReference() + ": " + readableValue(value.value()));
                }
                appendExtraPreview(result, document);
                appendLine(result, "");
            }

            resultArea.setText(result.toString().trim());
            showInfo("Lectura completada.");
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void onStartMonitoring() {
        try {
            List<DocumentMonitorConfig> documents = readConfiguredDocuments();
            MonitorConfig config = new MonitorConfig(
                    documents,
                    parseIntervalSeconds(),
                    readMessageChannel()
            );

            cellMonitorService.start(
                    config,
                    message -> Platform.runLater(() -> {
                        resultArea.setText(message);
                        showInfo("Monitoreando " + documents.size()
                                + " archivo(s) cada " + config.intervalSeconds() + " segundos.");
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

    private List<DocumentMonitorConfig> readConfiguredDocuments() {
        System.out.println("Reading configured documents...");
        List<DocumentMonitorConfig> documents = new ArrayList<>();
        List<DocumentForm> forms = documentForms();
        for (int index = 0; index < forms.size(); index++) {
            System.out.println("File name: "+forms.get(index).filePathField().getText());
            if (isDocumentEmpty(index, forms.get(index)) || forms.get(index).filePathField().getText().isEmpty())
                continue;
            documents.add(readDocumentConfig(index, forms.get(index)));
        }

        if (documents.isEmpty()) {
            throw new IllegalArgumentException("Configura al menos un archivo para leer.");
        }
        return documents;
    }

    private boolean isDocumentEmpty(int index, DocumentForm form) {
        return selectedFiles[index] == null
                && isBlank(form.filePathField().getText())
                && isBlank(form.columnReferenceField().getText());
    }

    private DocumentMonitorConfig readDocumentConfig(int index, DocumentForm form) {
        File file = selectedFiles[index];

        if(form.filePathField().getText() == null  ){
            System.out.println("filePathField is null for document " + (index + 1));
            return null;
        }

        if (file == null && !isBlank(form.filePathField().getText())) {
            file = new File(form.filePathField().getText().trim());
            selectedFiles[index] = file;
        }
        if (file == null) {
            //selectedFiles[index] = null;
            throw new IllegalArgumentException("Selecciona el archivo del documento " + (index + 1) + ".");
        }

        String columnReference = readRequiredText(
                form.columnReferenceField(),
                "Escribe la columna del documento " + (index + 1) + ", por ejemplo A, B o AA."
        );
        excelReaderService.parseColumnReference(columnReference);

        int startRow = parsePositiveInt(
                form.startRowField(),
                "Escribe la fila inicial del documento " + (index + 1) + "."
        );
        int endRow = parsePositiveInt(
                form.endRowField(),
                "Escribe la fila final del documento " + (index + 1) + "."
        );
        if (endRow < startRow) {
            throw new IllegalArgumentException("La fila final del documento " + (index + 1)
                    + " debe ser mayor o igual a la inicial.");
        }
        if (form.dataTypeComboBox().getValue() == null) {
            throw new IllegalArgumentException("Selecciona el tipo de dato del documento " + (index + 1) + ".");
        }
        validateExtraColumn(form.extraColumnFieldA(), index);
        validateExtraColumn(form.extraColumnFieldB(), index);

        return new DocumentMonitorConfig(
                file,
                columnReference,
                startRow,
                endRow,
                form.dataTypeComboBox().getValue(),
                readOptionalText(form.extraColumnFieldA()),
                readOptionalText(form.extraLabelFieldA()),
                readOptionalText(form.extraColumnFieldB()),
                readOptionalText(form.extraLabelFieldB())
        );
    }

    private void validateExtraColumn(TextField textField, int index) {
        String value = textField.getText();
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            excelReaderService.parseColumnReference(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("La columna extra del documento " + (index + 1)
                    + " debe tener formato valido, por ejemplo B, C o AA.");
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

    private int parsePositiveInt(TextField textField, String errorMessage) {
        String value = readRequiredText(textField, errorMessage);
        try {
            int parsedValue = Integer.parseInt(value);
            if (parsedValue < 1) {
                throw new IllegalArgumentException("El valor debe ser de al menos 1.");
            }
            return parsedValue;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("El valor debe ser un numero entero.");
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

    private String readOptionalText(TextField textField) {
        String value = textField.getText();
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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

    private void appendExtraPreview(StringBuilder result, DocumentMonitorConfig document) throws Exception {
        appendExtraColumnPreview(result, document, document.extraColumn1(), document.extraLabel1());
        appendExtraColumnPreview(result, document, document.extraColumn2(), document.extraLabel2());
    }

    private void appendExtraColumnPreview(StringBuilder result,
                                          DocumentMonitorConfig document,
                                          String columnReference,
                                          String label) throws Exception {
        if (isBlank(columnReference)) {
            return;
        }

        List<ExcelReader.ColumnCellValue> values = excelReaderService.readColumnRangeAsText(
                document.excelFile(),
                columnReference,
                document.startRow(),
                document.endRow()
        );
        appendLine(result, contextLabel(label, columnReference) + " (" + columnReference.trim().toUpperCase() + ")");
        for (ExcelReader.ColumnCellValue value : values) {
            appendLine(result, value.cellReference() + ": " + readableValue(value.value()));
        }
    }

    private String contextLabel(String label, String columnReference) {
        return isBlank(label) ? columnReference.trim().toUpperCase() : label.trim();
    }

    private List<DocumentForm> documentForms() {
        return List.of(
                new DocumentForm(filePathField1, columnReferenceField1, startRowField1, endRowField1, dataTypeComboBox1,
                        extraColumnField1A, extraLabelField1A, extraColumnField1B, extraLabelField1B),
                new DocumentForm(filePathField2, columnReferenceField2, startRowField2, endRowField2, dataTypeComboBox2,
                        extraColumnField2A, extraLabelField2A, extraColumnField2B, extraLabelField2B),
                new DocumentForm(filePathField3, columnReferenceField3, startRowField3, endRowField3, dataTypeComboBox3,
                        extraColumnField3A, extraLabelField3A, extraColumnField3B, extraLabelField3B)
        );
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

    private record DocumentForm(
            TextField filePathField,
            TextField columnReferenceField,
            TextField startRowField,
            TextField endRowField,
            ComboBox<CellDataType> dataTypeComboBox,
            TextField extraColumnFieldA,
            TextField extraLabelFieldA,
            TextField extraColumnFieldB,
            TextField extraLabelFieldB
    ) {
    }
}
