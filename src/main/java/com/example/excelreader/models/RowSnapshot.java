package com.example.excelreader.models;

import java.util.Map;

public record RowSnapshot(String cellReference, String value, Map<String, String> extraValues) {
}
