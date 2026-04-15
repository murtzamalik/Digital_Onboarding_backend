package com.bank.cebos.service.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ExcelOnboardingRowParserTest {

  private final ExcelOnboardingRowParser parser = new ExcelOnboardingRowParser();

  @Test
  void parsesHeaderRowAndDataRows() throws Exception {
    byte[] xlsx = buildWorkbook();

    List<ExcelOnboardingRowParser.ParsedRow> rows =
        parser.parseDataRows(new ByteArrayInputStream(xlsx));

    assertThat(rows)
        .hasSize(2)
        .containsExactly(
            new ExcelOnboardingRowParser.ParsedRow("111", "03001234567", "Ada Lovelace"),
            new ExcelOnboardingRowParser.ParsedRow("222", "03007654321", "Alan Turing"));
  }

  @Test
  void rejectsMissingRequiredHeaders() throws Exception {
    byte[] xlsx = buildWorkbookMissingMobile();

    assertThatThrownBy(() -> parser.parseDataRows(new ByteArrayInputStream(xlsx)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("MOBILE");
  }

  private static byte[] buildWorkbook() throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      var sheet = wb.createSheet();
      var h = sheet.createRow(0);
      h.createCell(0).setCellValue("CNIC");
      h.createCell(1).setCellValue("MOBILE");
      h.createCell(2).setCellValue("FULL_NAME");
      var r1 = sheet.createRow(1);
      r1.createCell(0).setCellValue("111");
      r1.createCell(1).setCellValue("03001234567");
      r1.createCell(2).setCellValue("Ada Lovelace");
      var r2 = sheet.createRow(2);
      r2.createCell(0).setCellValue("222");
      r2.createCell(1).setCellValue("03007654321");
      r2.createCell(2).setCellValue("Alan Turing");
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      wb.write(bos);
      return bos.toByteArray();
    }
  }

  private static byte[] buildWorkbookMissingMobile() throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      var sheet = wb.createSheet();
      var h = sheet.createRow(0);
      h.createCell(0).setCellValue("CNIC");
      h.createCell(1).setCellValue("FULL_NAME");
      var r1 = sheet.createRow(1);
      r1.createCell(0).setCellValue("111");
      r1.createCell(1).setCellValue("Name");
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      wb.write(bos);
      return bos.toByteArray();
    }
  }
}
