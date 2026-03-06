package com.excel.reader;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 列头读取器
 * 读取 Excel 文件并输出每列的列头
 */
public class ExcelColumnReader {

    private static final String EXCEL_FILE_PATH = "D:\\学习资料\\git\\study\\ai编程相关\\Vibe Coding氛围编程\\excel-reader\\src\\main\\resources\\测试.xlsx";

    public static void main(String[] args) {
        try {
            List<String> headers = readColumnHeaders(EXCEL_FILE_PATH);
            if (headers.isEmpty()) {
                System.out.println("未找到列头，请确认 Excel 文件第一行是否为列头。");
            } else {
                System.out.println("列头列表（共 " + headers.size() + " 列）:");
                for (int i = 0; i < headers.size(); i++) {
                    System.out.println("  列 " + (i + 1) + ": " + headers.get(i));
                }
            }
        } catch (IOException e) {
            System.err.println("读取文件失败: " + e.getMessage());
        }
    }

    /**
     * 读取 Excel 文件第一行的列头
     *
     * @param filePath Excel 文件路径（支持 .xls 和 .xlsx）
     * @return 列头列表
     */
    public static List<String> readColumnHeaders(String filePath) throws IOException {
        List<String> headers = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = createWorkbook(fis, filePath)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                return headers;
            }

            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                String headerValue = getCellValueAsString(cell);
                headers.add(headerValue);
            }
        }

        return headers;
    }

    private static Workbook createWorkbook(FileInputStream fis, String filePath) throws IOException {
        if (filePath.toLowerCase().endsWith(".xlsx")) {
            return new XSSFWorkbook(fis);
        } else if (filePath.toLowerCase().endsWith(".xls")) {
            return new HSSFWorkbook(fis);
        } else {
            throw new IllegalArgumentException("不支持的文件格式，请使用 .xls 或 .xlsx 文件");
        }
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (IllegalStateException e) {
                    return cell.getStringCellValue();
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }
}
