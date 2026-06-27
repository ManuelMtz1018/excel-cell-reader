package com.example.excelreader.service.strategy;

import com.example.excelreader.CellDataType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;

public interface CellValueReaderStrategy {

    CellDataType supportedType();

    String read(Cell cell, CellValue cellValue);
}
