package com.example.excelreader.service;

import com.example.excelreader.CellDataType;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface ExcelReader {

    String readCell(File excelFile, String cellReference, CellDataType expectedType) throws IOException;

    List<ColumnCellValue> readColumnRange(File excelFile,
                                          String columnReference,
                                          int startRow,
                                          int endRow,
                                          CellDataType expectedType) throws IOException;

    List<ColumnCellValue> readColumnRangeAsText(File excelFile,
                                                String columnReference,
                                                int startRow,
                                                int endRow) throws IOException;

    CellPosition parseCellReference(String cellReference);

    int parseColumnReference(String columnReference);

    record CellPosition(int rowIndex, int columnIndex) {
    }

    record ColumnCellValue(String cellReference, String value) {
    }
}
