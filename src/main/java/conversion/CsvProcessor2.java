package conversion;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CsvProcessor2 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        String downloadsFolder = System.getProperty("user.home") + File.separator + "Downloads";

        System.out.println("Enter the full path of the input file (.csv or .xls):");
        String inputFilePath = scanner.nextLine();
        File inputFile = new File(inputFilePath);

        if (!inputFile.exists() || !inputFile.isFile()) {
            System.err.println("Error: The specified file does not exist or is not a valid file.");
            scanner.close();
            return;
        }

        String csvFilePath = inputFilePath;
        if (inputFilePath.endsWith(".xls") || inputFilePath.endsWith(".xlsx")) {
            try {
                // If the input is an Excel file, convert it to CSV
                csvFilePath = convertXlsToCsv(inputFile);
                System.out.println("Converted Excel file to CSV: " + csvFilePath);
            } catch (IOException e) {
                System.err.println("Error converting Excel to CSV: " + e.getMessage());
                scanner.close();
                return;
            }
        }

        // Set the output file path
        Path outputFilePath = Paths.get(downloadsFolder, "processed_output.xlsx");

        // Read CSV, process, and save the Excel before and after removing empty cells
        processCsvFile(new File(csvFilePath), outputFilePath);

        scanner.close();
    }

    private static String convertXlsToCsv(File xlsFile) throws IOException {
        String csvFilePath = xlsFile.getAbsolutePath().replace(".xls", ".csv");
        if (csvFilePath.endsWith(".xlsx")) {
            csvFilePath = csvFilePath.replace(".xlsx", ".csv");
        }

        File csvFile = new File(csvFilePath);

        try (Workbook workbook = xlsFile.getName().endsWith(".xls") ? new HSSFWorkbook(new FileInputStream(xlsFile)) : new XSSFWorkbook(new FileInputStream(xlsFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                StringBuilder rowBuilder = new StringBuilder();

                for (int colIndex = 0; colIndex < row.getLastCellNum(); colIndex++) {
                    Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String cellValue = (cell == null) ? "" : getFormattedCellValue(cell);
                    rowBuilder.append(cellValue).append(",");
                }

                if (rowBuilder.length() > 0) {
                    rowBuilder.setLength(rowBuilder.length() - 1);
                }
                writer.write(rowBuilder.toString());
                writer.newLine();
            }
        }
        return csvFilePath;
    }

    private static String getFormattedCellValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new java.text.SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    private static void processCsvFile(File inputFile, Path outputFilePath) {
        List<List<String>> allRows = new ArrayList<>();
        double closingBalance;

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            // Create a workbook to write the processed data
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("ProcessedData");

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                lineNumber++;

                if (lineNumber <= 16) {
                    continue;
                }

                if (line.isEmpty()) continue;

                if (line.contains("TOTAL")) {
                    break;
                }

                String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                for (int i = 18; i < columns.length; i++) {
                    columns[i] = columns[i].replaceAll("^\"|\"$", "").trim();
                }
                if (Arrays.stream(columns).allMatch(String::isEmpty)) {
                    continue; // Skip empty rows
                }

                // Populate row data
                String col1 = (columns.length > 0) ? columns[0] : "";
                String col2 = (columns.length > 1) ? columns[1] : "";
                String col4 = (columns.length > 3) ? columns[3] : "";
                String Book_Rate = (columns.length > 17) ? columns[17] : "";
                String colHStr = (columns.length > 13) ? columns[13] : "";
                double colH = colHStr.isEmpty() ? 0.0 : Double.parseDouble(colHStr.replaceAll("[^\\d.]", "").trim());

                // Process and remove empty cells
                String[] outputRow = new String[] {
                        col1, col2, col4, Book_Rate, String.valueOf(colH)
                };

                // Create a row in the Excel sheet
                
            }

            // Save the Excel file before removing empty cells
            try (FileOutputStream preRemoveFile = new FileOutputStream("before_remove_empty_cells.xlsx")) {
                workbook.write(preRemoveFile);
            }

            // After processing, remove empty cells in specific columns
            removeEmptyCellsInColumns(sheet, 1,2, 3, 4);

            // Save the Excel file after removing empty cells
            try (FileOutputStream postRemoveFile = new FileOutputStream(outputFilePath.toFile())) {
                workbook.write(postRemoveFile);
            }

            System.out.println("Processed Excel files saved before and after removing empty cells.");
        } catch (IOException e) {
            System.err.println("Error while processing the file: " + e.getMessage());
        }
    }

    private static void removeEmptyCellsInColumns(Sheet sheet, int... columns) {
        for (Row row : sheet) {
            for (int colIndex : columns) {
                Cell cell = row.getCell(colIndex);
                if (cell != null && cell.getCellType() == CellType.BLANK) {
                    row.removeCell(cell);
                }
            }
        }
    }
}
