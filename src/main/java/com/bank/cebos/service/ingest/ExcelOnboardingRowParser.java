package com.bank.cebos.service.ingest;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** Parses the first sheet of an .xlsx: row 1 = headers, row 2+ = data. */
@Component
public final class ExcelOnboardingRowParser {

  public record ParsedRow(String cnic, String mobile, String fullName) {}

  private static final DataFormatter FORMATTER = new DataFormatter();

  public List<ParsedRow> parseDataRows(InputStream xlsxIn) throws IOException {
    try (Workbook workbook = new XSSFWorkbook(xlsxIn)) {
      Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
      if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
        return List.of();
      }
      Row headerRow = sheet.getRow(0);
      if (headerRow == null) {
        return List.of();
      }
      int cnicCol = findColumn(headerRow, "CNIC");
      int mobileCol = findColumn(headerRow, "MOBILE");
      int nameCol = findColumn(headerRow, "FULL_NAME");
      if (cnicCol < 0 || mobileCol < 0 || nameCol < 0) {
        throw new IllegalArgumentException(
            "Worksheet must contain header columns CNIC, MOBILE, FULL_NAME (row 1)");
      }
      List<ParsedRow> rows = new ArrayList<>();
      for (int r = 1; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        if (row == null) {
          continue;
        }
        String cnic = cellTrim(row, cnicCol);
        String mobile = cellTrim(row, mobileCol);
        String fullName = cellTrim(row, nameCol);
        if (cnic.isEmpty() && mobile.isEmpty() && fullName.isEmpty()) {
          continue;
        }
        rows.add(new ParsedRow(cnic, mobile, fullName));
      }
      return rows;
    }
  }

  private static int findColumn(Row headerRow, String name) {
    String target = name.toLowerCase(Locale.ROOT);
    short last = headerRow.getLastCellNum();
    for (int c = 0; c < last; c++) {
      String v = cellTrim(headerRow, c).toLowerCase(Locale.ROOT);
      if (v.equals(target)) {
        return c;
      }
    }
    return -1;
  }

  private static String cellTrim(Row row, int col) {
    if (row == null) {
      return "";
    }
    var cell = row.getCell(col);
    if (cell == null) {
      return "";
    }
    return FORMATTER.formatCellValue(cell).trim();
  }
}
