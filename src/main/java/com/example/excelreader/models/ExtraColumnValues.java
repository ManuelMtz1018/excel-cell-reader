package com.example.excelreader.models;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExtraColumnValues {

    private final Map<String, String> values = new LinkedHashMap<>();

    public static ExtraColumnValues empty() {
        return new ExtraColumnValues();
    }

    public void put(String label, String value) {
        values.put(label, value);
    }

    public Map<String, String> values() {
        return new LinkedHashMap<>(values);
    }
}
