package com.example.excelreader.service.strategy;

import com.example.excelreader.CellDataType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CellValueReaderRegistry {

    private final Map<CellDataType, CellValueReaderStrategy> readersByType;

    public CellValueReaderRegistry() {
        this(List.of(
                new TextCellValueReader(),
                new NumberCellValueReader(),
                new BooleanCellValueReader(),
                new DateCellValueReader()
        ));
    }

    public CellValueReaderRegistry(List<CellValueReaderStrategy> strategies) {
        readersByType = new EnumMap<>(CellDataType.class);
        for (CellValueReaderStrategy strategy : strategies) {
            readersByType.put(strategy.supportedType(), strategy);
        }
    }

    public CellValueReaderStrategy getReader(CellDataType dataType) {
        if (dataType == null) {
            throw new IllegalArgumentException("Selecciona el tipo de dato esperado.");
        }

        CellValueReaderStrategy strategy = readersByType.get(dataType);
        if (strategy == null) {
            throw new IllegalArgumentException("No existe lector configurado para " + dataType + ".");
        }
        return strategy;
    }
}
