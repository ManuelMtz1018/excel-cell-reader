package com.example.excelreader.service;

import com.example.excelreader.CellDataType;

import java.io.File;
import java.io.IOException;

public interface ExcelReader {

    String readCell(File excelFile, String cellReference, CellDataType expectedType) throws IOException;

    CellPosition parseCellReference(String cellReference);

    record CellPosition(int rowIndex, int columnIndex) {
    }
}
