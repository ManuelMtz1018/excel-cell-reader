package com.example.excelreader.service.strategy;

import com.example.excelreader.CellDataType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;

public class BooleanCellValueReader implements CellValueReaderStrategy {

    @Override
    public CellDataType supportedType() {
        return CellDataType.BOOLEANO;
    }

    @Override
    public String read(Cell cell, CellValue cellValue) {
        if (cellValue.getCellType() != CellType.BOOLEAN) {
            throw new IllegalArgumentException("La celda seleccionada no contiene un valor booleano.");
        }
        return String.valueOf(cellValue.getBooleanValue());
    }
}
