package com.example.excelreader.models;

import com.example.excelreader.CellDataType;
import java.io.File;

public record DocumentMonitorConfig(
        File excelFile,
        String columnReference,
        int startRow,
        int endRow,
        CellDataType expectedType,
        String extraColumn1,
        String extraLabel1,
        String extraColumn2,
        String extraLabel2
) {
    public String displayName() {
        return excelFile.getName();
    }

    public String documentKey() {
        return excelFile.getAbsolutePath();
    }
}
