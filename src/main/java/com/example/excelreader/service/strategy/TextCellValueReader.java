package com.example.excelreader.service.strategy;

import com.example.excelreader.CellDataType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;

public class TextCellValueReader implements CellValueReaderStrategy {

    @Override
    public CellDataType supportedType() {
        return CellDataType.TEXTO;
    }

    @Override
    public String read(Cell cell, CellValue cellValue) {
        if (cellValue.getCellType() != CellType.STRING) {
            throw new IllegalArgumentException("La celda seleccionada no contiene texto.");
        }
        return cellValue.getStringValue();
    }
}
