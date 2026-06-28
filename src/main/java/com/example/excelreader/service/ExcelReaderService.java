package com.example.excelreader.service;

import com.example.excelreader.CellDataType;
import com.example.excelreader.service.strategy.CellValueReaderRegistry;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcelReaderService implements ExcelReader {

    private static final Pattern CELL_REFERENCE_PATTERN = Pattern.compile("^([A-Za-z]+)([1-9][0-9]*)$");
    private static final Pattern COLUMN_REFERENCE_PATTERN = Pattern.compile("^[A-Za-z]+$");

    private final CellValueReaderRegistry readerRegistry;

    public ExcelReaderService() {
        this(new CellValueReaderRegistry());
    }

    public ExcelReaderService(CellValueReaderRegistry readerRegistry) {
        this.readerRegistry = readerRegistry;
    }

    @Override
    public String readCell(File excelFile, String cellReference, CellDataType expectedType) throws IOException {
        CellPosition position = parseCellReference(cellReference);

        try (FileInputStream inputStream = new FileInputStream(excelFile);
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("El archivo no contiene hojas de calculo.");
            }

            var sheet = workbook.getSheetAt(0);
            Row row = sheet.getRow(position.rowIndex());
            if (row == null) {
                throw new IllegalArgumentException("La fila " + (position.rowIndex() + 1) + " esta vacia.");
            }

            Cell cell = row.getCell(position.columnIndex());
            if (cell == null) {
                throw new IllegalArgumentException("La celda " + cellReference.toUpperCase() + " esta vacia.");
            }

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            return readByExpectedType(cell, expectedType, evaluator);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("No se pudo leer el archivo Excel. Verifica que no este danado o abierto con bloqueo.", exception);
        }
    }

    @Override
    public List<ColumnCellValue> readColumnRange(File excelFile,
                                                 String columnReference,
                                                 int startRow,
                                                 int endRow,
                                                 CellDataType expectedType) throws IOException {
        int columnIndex = parseColumnReference(columnReference);
        validateRowRange(startRow, endRow);
        String normalizedColumn = columnReference.trim().toUpperCase();

        try (FileInputStream inputStream = new FileInputStream(excelFile);
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("El archivo no contiene hojas de calculo.");
            }

            var sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<ColumnCellValue> values = new ArrayList<>();

            for (int rowNumber = startRow; rowNumber <= endRow; rowNumber++) {
                Row row = sheet.getRow(rowNumber - 1);
                if (row == null) {
                    values.add(new ColumnCellValue(normalizedColumn + rowNumber, ""));
                    continue;
                }

                Cell cell = row.getCell(columnIndex);
                if (cell == null) {
                    values.add(new ColumnCellValue(normalizedColumn + rowNumber, ""));
                    continue;
                }

                values.add(new ColumnCellValue(
                        normalizedColumn + rowNumber,
                        readOptionalByExpectedType(cell, expectedType, evaluator)
                ));
            }

            return values;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("No se pudo leer el archivo Excel. Verifica que no este danado o abierto con bloqueo.", exception);
        }
    }

    @Override
    public List<ColumnCellValue> readColumnRangeAsText(File excelFile,
                                                       String columnReference,
                                                       int startRow,
                                                       int endRow) throws IOException {
        int columnIndex = parseColumnReference(columnReference);
        validateRowRange(startRow, endRow);
        String normalizedColumn = columnReference.trim().toUpperCase();

        try (FileInputStream inputStream = new FileInputStream(excelFile);
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("El archivo no contiene hojas de calculo.");
            }

            var sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            List<ColumnCellValue> values = new ArrayList<>();

            for (int rowNumber = startRow; rowNumber <= endRow; rowNumber++) {
                Row row = sheet.getRow(rowNumber - 1);
                if (row == null) {
                    values.add(new ColumnCellValue(normalizedColumn + rowNumber, ""));
                    continue;
                }

                Cell cell = row.getCell(columnIndex);
                values.add(new ColumnCellValue(
                        normalizedColumn + rowNumber,
                        cell == null ? "" : formatter.formatCellValue(cell, evaluator)
                ));
            }

            return values;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("No se pudo leer el archivo Excel. Verifica que no este danado o abierto con bloqueo.", exception);
        }
    }

    @Override
    public CellPosition parseCellReference(String cellReference) {
        if (cellReference == null) {
            throw new IllegalArgumentException("La celda es obligatoria.");
        }

        Matcher matcher = CELL_REFERENCE_PATTERN.matcher(cellReference.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("La celda debe tener un formato valido, por ejemplo A1, B2 o C10.");
        }

        String columnLetters = matcher.group(1).toUpperCase();
        int rowIndex = Integer.parseInt(matcher.group(2)) - 1;
        int columnIndex = convertColumnLettersToIndex(columnLetters);

        return new CellPosition(rowIndex, columnIndex);
    }

    @Override
    public int parseColumnReference(String columnReference) {
        if (columnReference == null) {
            throw new IllegalArgumentException("La columna es obligatoria.");
        }

        String normalizedColumn = columnReference.trim();
        if (!COLUMN_REFERENCE_PATTERN.matcher(normalizedColumn).matches()) {
            throw new IllegalArgumentException("La columna debe tener un formato valido, por ejemplo A, B o AA.");
        }

        return convertColumnLettersToIndex(normalizedColumn.toUpperCase());
    }

    private void validateRowRange(int startRow, int endRow) {
        if (startRow < 1) {
            throw new IllegalArgumentException("La fila inicial debe ser de al menos 1.");
        }
        if (endRow < startRow) {
            throw new IllegalArgumentException("La fila final debe ser mayor o igual a la inicial.");
        }
    }

    private int convertColumnLettersToIndex(String columnLetters) {
        int columnNumber = 0;
        for (char letter : columnLetters.toCharArray()) {
            columnNumber = columnNumber * 26 + (letter - 'A' + 1);
        }
        return columnNumber - 1;
    }

    private String readByExpectedType(Cell cell, CellDataType expectedType, FormulaEvaluator evaluator) {
        CellValue cellValue = evaluator.evaluate(cell);
        if (cellValue == null || cellValue.getCellType() == CellType.BLANK) {
            throw new IllegalArgumentException("La celda seleccionada esta vacia.");
        }

        return readerRegistry.getReader(expectedType).read(cell, cellValue);
    }

    private String readOptionalByExpectedType(Cell cell, CellDataType expectedType, FormulaEvaluator evaluator) {
        CellValue cellValue = evaluator.evaluate(cell);
        if (cellValue == null || cellValue.getCellType() == CellType.BLANK) {
            return "";
        }

        return readerRegistry.getReader(expectedType).read(cell, cellValue);
    }
}
