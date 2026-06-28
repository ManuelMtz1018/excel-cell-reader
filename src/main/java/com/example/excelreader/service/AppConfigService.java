package com.example.excelreader.service;

import com.example.excelreader.AppConfig;
import com.example.excelreader.CellDataType;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppConfigService {

    private static final Logger log = LogManager.getLogger(AppConfigService.class);
    private static final String CLASSPATH_CONFIG = "/application.properties";
    private static final String EXTERNAL_CONFIG_PROPERTY = "excel.reader.config";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    public AppConfig load() {
        Properties properties = new Properties();
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        loadClasspathConfig(properties);
        loadExternalConfig(properties);
        resolvePlaceholders(properties, dotenv);

        return new AppConfig(
                loadDocumentConfigs(properties),
                new AppConfig.MonitorConfig(parsePositiveInt(properties, "monitor.interval.seconds", 5)),
                new AppConfig.MessagingConfig(
                        parseMessageChannel(properties.getProperty("messaging.channel"))
                ),
                new AppConfig.TelegramConfig(
                        properties.getProperty("telegram.bot.token", ""),
                        properties.getProperty("telegram.chat.id", "")
                ),
                new AppConfig.WhatsAppConfig(
                        properties.getProperty("whatsapp.access-token", ""),
                        properties.getProperty("whatsapp.phone-number-id", ""),
                        properties.getProperty("whatsapp.to", ""),
                        properties.getProperty("whatsapp.graph-api-version", "v22.0")
                )
        );
    }

    private List<AppConfig.ExcelDocumentConfig> loadDocumentConfigs(Properties properties) {
        List<AppConfig.ExcelDocumentConfig> documents = new ArrayList<>();
        for (int index = 1; index <= 3; index++) {
            documents.add(new AppConfig.ExcelDocumentConfig(
                    propertyWithFallback(properties, "document." + index + ".file.path", index == 1 ? "excel.file.path" : ""),
                    documentColumn(properties, index),
                    documentStartRow(properties, index),
                    documentEndRow(properties, index),
                    parseDataType(propertyWithFallback(properties, "document." + index + ".data-type", index == 1 ? "excel.cell.data-type" : "")),
                    properties.getProperty("document." + index + ".extra-column-1", ""),
                    propertyOrDefault(properties, "document." + index + ".extra-label-1", "Estado"),
                    properties.getProperty("document." + index + ".extra-column-2", ""),
                    propertyOrDefault(properties, "document." + index + ".extra-label-2", "Ubicacion")
            ));
        }
        return documents;
    }

    private String propertyOrDefault(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key, "");
        return value.isBlank() ? defaultValue : value;
    }

    private String documentColumn(Properties properties, int index) {
        String value = properties.getProperty("document." + index + ".column", "");
        if (!value.isBlank() || index != 1) {
            return value;
        }

        return extractColumn(properties.getProperty("excel.cell.reference", ""));
    }

    private int documentStartRow(Properties properties, int index) {
        int configuredValue = parsePositiveInt(properties, "document." + index + ".start-row", 1);
        if (configuredValue != 1 || index != 1) {
            return configuredValue;
        }

        return extractRow(properties.getProperty("excel.cell.reference", ""), configuredValue);
    }

    private int documentEndRow(Properties properties, int index) {
        int configuredValue = parsePositiveInt(properties, "document." + index + ".end-row", 1);
        if (configuredValue != 1 || index != 1) {
            return configuredValue;
        }

        return extractRow(properties.getProperty("excel.cell.reference", ""), configuredValue);
    }

    private String extractColumn(String cellReference) {
        if (cellReference == null || cellReference.isBlank()) {
            return "";
        }
        return cellReference.trim().replaceAll("[^A-Za-z]", "").toUpperCase();
    }

    private int extractRow(String cellReference, int defaultValue) {
        if (cellReference == null || cellReference.isBlank()) {
            return defaultValue;
        }

        String row = cellReference.trim().replaceAll("[^0-9]", "");
        if (row.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(row);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private String propertyWithFallback(Properties properties, String primaryKey, String fallbackKey) {
        String value = properties.getProperty(primaryKey, "");
        if (!value.isBlank() || fallbackKey.isBlank()) {
            return value;
        }
        return properties.getProperty(fallbackKey, "");
    }

    private void loadClasspathConfig(Properties properties) {
        try (InputStream inputStream = AppConfigService.class.getResourceAsStream(CLASSPATH_CONFIG)) {
            if (inputStream == null) {
                log.warn("No se encontro {}", CLASSPATH_CONFIG);
                return;
            }
            properties.load(inputStream);
        } catch (IOException exception) {
            log.warn("No se pudo cargar {}", CLASSPATH_CONFIG, exception);
        }
    }

    private void loadExternalConfig(Properties properties) {
        String configuredPath = System.getProperty(
                EXTERNAL_CONFIG_PROPERTY,
                properties.getProperty("config.external.path", "")
        );
        if (configuredPath.isBlank()) {
            return;
        }

        Path externalPath = Path.of(configuredPath);
        if (!Files.exists(externalPath)) {
            log.info("Configuracion externa no encontrada: {}", externalPath.toAbsolutePath());
            return;
        }

        try (InputStream inputStream = Files.newInputStream(externalPath)) {
            Properties externalProperties = new Properties();
            externalProperties.load(inputStream);
            properties.putAll(externalProperties);
            log.info("Configuracion externa cargada desde {}", externalPath.toAbsolutePath());
        } catch (IOException exception) {
            log.warn("No se pudo cargar la configuracion externa {}", externalPath.toAbsolutePath(), exception);
        }
    }

    private void resolvePlaceholders(Properties properties, Dotenv dotenv) {
        for (String key : properties.stringPropertyNames()) {
            properties.setProperty(key, resolveValue(properties.getProperty(key), dotenv));
        }
    }

    private String resolveValue(String value, Dotenv dotenv) {
        if (value == null || value.isBlank()) {
            return value;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        StringBuffer resolvedValue = new StringBuffer();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String replacement = resolveVariable(variableName, dotenv);
            if (replacement == null) {
                log.warn("Variable de entorno no encontrada: {}", variableName);
                replacement = "";
            }
            matcher.appendReplacement(resolvedValue, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(resolvedValue);
        return resolvedValue.toString();
    }

    private String resolveVariable(String variableName, Dotenv dotenv) {
        String systemPropertyValue = System.getProperty(variableName);
        if (systemPropertyValue != null) {
            return systemPropertyValue;
        }

        String environmentValue = System.getenv(variableName);
        if (environmentValue != null) {
            return environmentValue;
        }

        return dotenv.get(variableName);
    }

    private CellDataType parseDataType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return CellDataType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            log.warn("Tipo de dato invalido en configuracion: {}", value);
            return null;
        }
    }

    private MessageChannel parseMessageChannel(String value) {
        if (value == null || value.isBlank()) {
            return MessageChannel.TELEGRAM;
        }

        try {
            return MessageChannel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            log.warn("Canal de mensajeria invalido en configuracion: {}", value);
            return MessageChannel.TELEGRAM;
        }
    }

    private int parsePositiveInt(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            int parsedValue = Integer.parseInt(value.trim());
            return parsedValue > 0 ? parsedValue : defaultValue;
        } catch (NumberFormatException exception) {
            log.warn("Numero invalido para {}: {}", key, value);
            return defaultValue;
        }
    }
}
