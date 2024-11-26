package conversion;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class CsvProcessor2 {
    double closingBalance;
    static Workbook workbook = new XSSFWorkbook();  // Create a new workbook for Excel
    static Sheet sheet = workbook.createSheet("Asset Data");  // Create a new sheet
    static String assetName = null; 

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
        if (inputFilePath.endsWith(".xls")) {
            try {
                csvFilePath = convertXlsToCsv(inputFile);
                System.out.println("Converted .xls file to .csv: " + csvFilePath);
            } catch (IOException e) {
                System.err.println("Error converting .xls to .csv: " + e.getMessage());
                scanner.close();
                return;
            }
        }

        Path outputFilePath = Paths.get(downloadsFolder, "processed_output.csv");

        processCsvFile(new File(csvFilePath), outputFilePath);

        scanner.close();
    }

    private static String convertXlsToCsv(File xlsFile) throws IOException {
        String csvFilePath = xlsFile.getAbsolutePath().replace(".xls", ".csv");
        File csvFile = new File(csvFilePath);

        try (Workbook workbook = new HSSFWorkbook(new FileInputStream(xlsFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {

            Sheet sheet = workbook.getSheetAt(0);

            int maxColumns = 0;
            for (Row row : sheet) {
                maxColumns = Math.max(maxColumns, row.getLastCellNum());
            }

            for (Row row : sheet) {
                StringBuilder rowBuilder = new StringBuilder();

                for (int colIndex = 0; colIndex < maxColumns; colIndex++) {
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
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath.toFile()))) {

            writer.write("*AssetName,*AssetNumber,PurchaseDate,PurchasePrice,AssetType,Description,TrackingCategory1,TrackingOption1,TrackingCategory2,TrackingOption2,SerialNumber,WarrantyExpiry,Book_DepreciationStartDate,Book_CostLimit,Book_ResidualValue,Book_DepreciationMethod,Book_AveragingMethod,Book_Rate,Book_EffectiveLife,Book_OpeningBookAccumulatedDepreciation,Tax_DepreciationMethod,Tax_PoolName,Tax_PooledDate,Tax_PooledAmount,Tax_DepreciationStartDate,Tax_CostLimit,Tax_ResidualValue,Tax_AveragingMethod,Tax_Rate,Tax_EffectiveLife,Tax_OpeningAccumulatedDepreciation");
            writer.newLine();

            String line;
            String previousTextInCol4 = null;
            int lineNumber = 0;

            String bookAveragingMethod = "Actual Days";
            String taxRate = "Actual Days";

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                lineNumber++;

                if (lineNumber <= 16) {
                    continue;
                }
                if (line.matches("^,*$")) {
                    continue;  // Skip this row as it contains only commas or is empty
                }

                if (line.contains("TOTAL")) {
                    break;
                }

                String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                for (int i = 18; i < columns.length; i++) {
                    columns[i] = columns[i].replaceAll("^\"|\"$", "").trim();
                }
                String number = (columns.length > 0) ? columns[0] : "";
                String col1 = (columns.length > 0) ? columns[0] : "";
                String col2 = (columns.length > 1) ? columns[1] : "";
                String col4 = (columns.length > 3) ? columns[3] : "";
                String Book_Rate = (columns.length > 17) ? columns[17] : "";
                String colHStr = (columns.length > 13) ? columns[13] : "";
                double colH = colHStr.isEmpty() ? 0.0 : Double.parseDouble(colHStr.replaceAll("[^\\d.]", "").trim());

                col2 = col2.replaceAll("\\s*::\\s*AM", "").trim();

                List<String> assetRow = new ArrayList<>();
                assetRow.add(col1.trim());
                assetRow.add(col4.trim());
                allRows.add(assetRow);

                String col4Text = col4;
                String col4Number = "";
                String col6 = "";

                col4Text = col4.replaceAll("[^\\x00-\\x7F]", "").replaceAll("\\.", "");
                col4Number = "";
                if (col4.trim().isEmpty()) {
                    continue;
                }

                if (col4.matches(".*\\d+\\.\\d+.*")) {
                    col4Number = col4.replaceAll("[^\\d.]", "").trim();
                    col4Text = col4.replaceAll("[^\\D]", "");
                }

                assetName = col4Text;
//                Row row = sheet.createRow(sheet.getPhysicalNumberOfRows());  // Create a new row
//                Cell assetNameCell = row.createCell(0);  // Create a new cell in the first column
//                assetNameCell.setCellValue(assetName);
//                if (col4Number.isEmpty() || col4Number.equals("null") || col4Number.equals("")) {
//                    continue;  // Skip to the next iteration if col4Number is empty
//                }

                if (assetName == null) {  // Only assign assetName if it's not already set
                    assetName = col4Text;  // Initial assignment of assetName
                }

                double col4Value = col4Number.isEmpty() ? 0.0 : Double.parseDouble(col4Number);
                closingBalance = colH - col4Value;

                previousTextInCol4 = col4Text;

                String col13 = col2;
                String col25 = col2;

                String purchaseDate = col2;
                String assetNumber = col1;
                String assetType = col6;
                String book_DepreciationStartDate = col13;
                String tax_DepreciationStartDate = col25;
                String tax_Rate = Book_Rate;
                closingBalance = col4Value - colH;
                String closingBalanceStr = (closingBalance == 0.0) ? "" : String.valueOf(closingBalance);

                assetName = assetName.replaceAll("\\.", "");

                bookAveragingMethod = purchaseDate.isEmpty() ? "" : "Actual Days";
                taxRate = purchaseDate.isEmpty() ? "" : "Actual Days";  // Don't set "Actual Days" if assetNumber is empty

                String[] outputRow = {
                        assetName.isEmpty() ? "" : assetName,
                        		assetNumber.isEmpty() ? "" : assetNumber,
                        purchaseDate.isEmpty() ? "" : purchaseDate,
                        col4Number.isEmpty() ? "" : col4Number,
                        assetType.isEmpty() ? "" : assetType,
                        "", "", "", "", "", "", "",
                        book_DepreciationStartDate.isEmpty() ? "" : book_DepreciationStartDate,
                        "", "", "", bookAveragingMethod.isEmpty() ? "" : bookAveragingMethod,
                        Book_Rate.isEmpty() ? "" : Book_Rate, "", closingBalanceStr.isEmpty() ? "" : closingBalanceStr,
                        "", "", "", "", tax_DepreciationStartDate.isEmpty() ? "" : tax_DepreciationStartDate,
                        "", "", taxRate.isEmpty() ? "" : taxRate, Book_Rate.isEmpty() ? "" : Book_Rate, "",
                        closingBalanceStr.isEmpty() ? "" : closingBalanceStr, "", "", "", ""
                };

                writer.write(String.join(",", outputRow));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
