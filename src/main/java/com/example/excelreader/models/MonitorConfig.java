package com.example.excelreader.models;

import com.example.excelreader.service.MessageChannel;
import java.util.List;

public record MonitorConfig(
        List<DocumentMonitorConfig> documents,
        int intervalSeconds,
        MessageChannel messageChannel
) {
}
