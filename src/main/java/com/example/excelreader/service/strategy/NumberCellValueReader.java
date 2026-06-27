package com.example.excelreader.service.strategy;

import com.example.excelreader.CellDataType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;

public class NumberCellValueReader implements CellValueReaderStrategy {

    @Override
    public CellDataType supportedType() {
        return CellDataType.NUMERO;
    }

    @Override
    public String read(Cell cell, CellValue cellValue) {
        if (cellValue.getCellType() != CellType.NUMERIC) {
            throw new IllegalArgumentException("La celda seleccionada no contiene un numero.");
        }

        double numericValue = cellValue.getNumberValue();
        if (numericValue == Math.rint(numericValue)) {
            return String.valueOf((long) numericValue);
        }
        return String.valueOf(numericValue);
    }
}
