package conversion;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.opencsv.CSVWriter;

public class ExcelToCsvConverter {

	 public static void main(String[] args) {
	        // Input file path
	        String inputFilePath = "C://Users/test/Downloads/Asis/Asis/Neylon Legal Business Trust/Neylon Legal Business Trust  - TB MYOB AE.xlsx";
	        convertExcelToCsv(inputFilePath);
	    }
    public static void convertExcelToCsv(String inputFilePath) {
        File inputFile = new File(inputFilePath);
        if (!inputFile.exists() || !inputFile.isFile()) {
            System.err.println("The specified file does not exist: " + inputFilePath);
            return;
        }

        // Define the output file path
        String outputFilePath = inputFile.getParent() + "/" + getFileNameWithoutExtension(inputFile.getName()) + ".csv";

        try (FileInputStream fis = new FileInputStream(inputFile);
             Workbook workbook = new XSSFWorkbook(fis); // For .xlsx files
             CSVWriter writer = new CSVWriter(new FileWriter(outputFilePath))) {

            // Get the first sheet
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                int lastCellNum = row.getLastCellNum();
                String[] rowData = new String[lastCellNum];

                for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
                    Cell cell = row.getCell(cellIndex);
                    rowData[cellIndex] = (cell == null) ? "" : getCellValue(cell);
                }

                // Write row data to CSV
                writer.writeNext(rowData);
            }

            System.out.println("Excel file has been successfully converted to CSV: " + outputFilePath);

        } catch (IOException e) {
            System.err.println("Error while converting Excel to CSV: " + e.getMessage());
        }
    }

    private static String getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private static String getFileNameWithoutExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }
}
