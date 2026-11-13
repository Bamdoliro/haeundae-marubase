package com.bamdoliro.maru.infrastructure.xlsx;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.infrastructure.xlsx.constant.XlsxConstant;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@RequiredArgsConstructor
@Component
public class XlsxGenerator {

    private final XlsxService xlsxService;

    public Resource execute(String templateName, List<Form> formList, List<Function<Form, Object>> columnList, List<String> styleList) throws IOException {
        return execute(templateName, formList, columnList, styleList, XlsxConstant.FIRST_ROW_INDEX_WITH_TITLE);
    }

    public Resource execute(String templateName, List<Form> formList, List<Function<Form, Object>> columnList, List<String> styleList, int firstRowIndex) throws IOException {
        Workbook workbook = xlsxService.openTemplate(templateName);
        Sheet sheet = workbook.getSheetAt(0);

        Map<String, CellStyle> styleMap = Map.of(
                "default", xlsxService.createDefaultCellStyle(workbook),
                "right", xlsxService.createRightCellStyle(workbook),
                "empty", xlsxService.createEmptyCellStyle(workbook),
                "date", xlsxService.createDateCellStyle(workbook),
                "wrap", xlsxService.createWrapTextCellStyle(workbook)
        );

        for (int index = 0; index < formList.size(); index++) {
            Form form = formList.get(index);
            Row row = sheet.createRow(index + firstRowIndex);
            int columnIndex = 0;

            for (Function<Form, Object> column : columnList) {
                createCell(row, columnIndex, column.apply(form), styleMap.get(styleList.get(columnIndex++)));
            }
        }

        return xlsxService.convertToByteArrayResource(workbook);
    }

    private void createCell(Row row, int cellIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(cellIndex);
        if (value instanceof Integer)
            cell.setCellValue((Integer) value);
        else if (value instanceof Double)
            cell.setCellValue((Double) value);
        else if (value instanceof Boolean)
            cell.setCellValue((Boolean) value);
        else if (value instanceof Long)
            cell.setCellValue((Long) value);
        else if (value instanceof LocalDate)
            cell.setCellValue((LocalDate) value);
        else
            cell.setCellValue((String) value);
        cell.setCellStyle(style);
    }
}
