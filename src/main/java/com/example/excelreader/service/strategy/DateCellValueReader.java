package com.example.excelreader.service.strategy;

import com.example.excelreader.CellDataType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateCellValueReader implements CellValueReaderStrategy {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public CellDataType supportedType() {
        return CellDataType.FECHA;
    }

    @Override
    public String read(Cell cell, CellValue cellValue) {
        if (!DateUtil.isCellDateFormatted(cell)) {
            throw new IllegalArgumentException("La celda seleccionada no tiene formato de fecha.");
        }

        // POI entrega Date; se convierte a LocalDate para mostrarla limpia.
        return cell.getDateCellValue()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DATE_FORMATTER);
    }
}
