package com.example.excelreader.models;

import java.util.Map;

public record CellChange(String cellReference, String oldValue, String newValue, Map<String, String> extraValues) {
}
