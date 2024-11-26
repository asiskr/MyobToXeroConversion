package conversion;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CreateCsvFile extends CsvProcessor2 {
    // Define the lists to store data for rows
    private static List<String> assetNames = new ArrayList<>();
    private static List<String> assetNumbers = new ArrayList<>();
    private static List<String> purchaseDates = new ArrayList<>();
    private static List<String> purchasePrices = new ArrayList<>();
    private static List<String> assetTypes = new ArrayList<>();
    private static List<String> bookDepreciationStartDates = new ArrayList<>();
    private static List<String> bookAveragingMethods = new ArrayList<>();
    private static List<String> bookRates = new ArrayList<>();
    private static List<String> closingBalances = new ArrayList<>();
    private static List<String> taxDepreciationStartDates = new ArrayList<>();
    private static List<String> taxRates = new ArrayList<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Input file location
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

        // Process the input CSV to populate data lists
        populateDataLists(new File(csvFilePath));

        // Write the processed data to a new CSV file in Downloads folder
        String downloadsFolder = System.getProperty("user.home") + File.separator + "Downloads";
        Path outputCsvFilePath = Paths.get(downloadsFolder, "new_asset_file.csv");
        writeToCsvFile(outputCsvFilePath.toFile());

        // Write the processed data to a new Excel file in Downloads folder
        Path outputExcelFilePath = Paths.get(downloadsFolder, "new_asset_file.xlsx");
        writeToExcelFile(outputExcelFilePath.toFile());

        scanner.close();
    }

    private static void populateDataLists(File csvFile) {
        // Implement this method to parse the input CSV and populate data lists
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            reader.readLine(); // Skip the header line
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                if (columns.length < 10) continue; // Skip malformed rows
                
                assetNames.add(columns[0].trim());
                assetNumbers.add(columns[1].trim());
                purchaseDates.add(columns[2].trim());
                purchasePrices.add(columns[3].trim());
                assetTypes.add(columns[4].trim());
                bookDepreciationStartDates.add(columns[5].trim());
                bookAveragingMethods.add(columns[6].trim());
                bookRates.add(columns[7].trim());
                closingBalances.add(columns[8].trim());
                taxDepreciationStartDates.add(columns[9].trim());
//                taxRates.add(columns[10].trim());
            }
        } catch (IOException e) {
            System.err.println("Error reading the CSV file: " + e.getMessage());
        }
    }

    private static void writeToCsvFile(File csvFile) {
        String headers = "*AssetName,*AssetNumber,PurchaseDate,PurchasePrice,AssetType,Description,"
                + "TrackingCategory1,TrackingOption1,TrackingCategory2,TrackingOption2,SerialNumber,WarrantyExpiry,"
                + "Book_DepreciationStartDate,Book_CostLimit,Book_ResidualValue,Book_DepreciationMethod,"
                + "Book_AveragingMethod,Book_Rate,Book_EffectiveLife,Book_OpeningBookAccumulatedDepreciation,"
                + "Tax_DepreciationMethod,Tax_PoolName,Tax_PooledDate,Tax_PooledAmount,Tax_DepreciationStartDate,"
                + "Tax_CostLimit,Tax_ResidualValue,Tax_AveragingMethod,Tax_Rate,Tax_EffectiveLife,"
                + "Tax_OpeningAccumulatedDepreciation";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
            // Write headers
            writer.write(headers);
            writer.newLine();

            // Write rows dynamically based on data lists
            int numRows = assetNames.size();
            for (int i = 0; i < numRows; i++) {
                String[] outputRow = {
                        getValue(assetNames, i),
                        getValue(assetNumbers, i),
                        getValue(purchaseDates, i),
                        getValue(purchasePrices, i),
                        getValue(assetTypes, i),
                        "", "", "", "", "", "", "",
                        getValue(bookDepreciationStartDates, i),
                        "", "", "",
                        getValue(bookAveragingMethods, i),
                        getValue(bookRates, i),
                        "", getValue(closingBalances, i),
                        "", "", "", "",
                        getValue(taxDepreciationStartDates, i),
                        "", "", getValue(taxRates, i),
                        getValue(bookRates, i),
                        getValue(closingBalances, i),
                        "", "", "", ""
                };

                writer.write(String.join(",", outputRow));
                writer.newLine();
            }

            System.out.println("CSV file created successfully at: " + csvFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error while writing to the CSV file: " + e.getMessage());
        }
    }

    private static void writeToExcelFile(File excelFile) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Assets");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"*AssetName", "*AssetNumber", "PurchaseDate", "PurchasePrice", "AssetType", "Description",
                    "TrackingCategory1", "TrackingOption1", "TrackingCategory2", "TrackingOption2", "SerialNumber", "WarrantyExpiry",
                    "Book_DepreciationStartDate", "Book_CostLimit", "Book_ResidualValue", "Book_DepreciationMethod",
                    "Book_AveragingMethod", "Book_Rate", "Book_EffectiveLife", "Book_OpeningBookAccumulatedDepreciation",
                    "Tax_DepreciationMethod", "Tax_PoolName", "Tax_PooledDate", "Tax_PooledAmount", "Tax_DepreciationStartDate",
                    "Tax_CostLimit", "Tax_ResidualValue", "Tax_AveragingMethod", "Tax_Rate", "Tax_EffectiveLife",
                    "Tax_OpeningAccumulatedDepreciation"};

            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // Write data rows
            int rowIndex = 1; // Start from row 1 (after header)
            int numRows = assetNames.size();
            for (int i = 0; i < numRows; i++) {
                Row row = sheet.createRow(rowIndex++);

                row.createCell(0).setCellValue(getValue(assetNames, i));
                row.createCell(1).setCellValue(getValue(assetNumbers, i));
                row.createCell(2).setCellValue(getValue(purchaseDates, i));
                row.createCell(3).setCellValue(getValue(purchasePrices, i));
                row.createCell(4).setCellValue(getValue(assetTypes, i));
                row.createCell(5).setCellValue(""); // Description
                row.createCell(6).setCellValue(""); // TrackingCategory1
                row.createCell(7).setCellValue(""); // TrackingOption1
                row.createCell(8).setCellValue(""); // TrackingCategory2
                row.createCell(9).setCellValue(""); // TrackingOption2
                row.createCell(10).setCellValue(""); // SerialNumber
                row.createCell(11).setCellValue(""); // WarrantyExpiry
                row.createCell(12).setCellValue(getValue(bookDepreciationStartDates, i));
                row.createCell(13).setCellValue(""); // Book_CostLimit
                row.createCell(14).setCellValue(""); // Book_ResidualValue
                row.createCell(15).setCellValue(""); // Book_DepreciationMethod
                row.createCell(16).setCellValue(getValue(bookAveragingMethods, i));
                row.createCell(17).setCellValue(getValue(bookRates, i));
                row.createCell(18).setCellValue(""); // Book_EffectiveLife
                row.createCell(19).setCellValue(getValue(closingBalances, i));
                row.createCell(20).setCellValue(""); // Tax_DepreciationMethod
                row.createCell(21).setCellValue(""); // Tax_PoolName
                row.createCell(22).setCellValue(""); // Tax_PooledDate
                row.createCell(23).setCellValue(""); // Tax_PooledAmount
                row.createCell(24).setCellValue(getValue(taxDepreciationStartDates, i));
                row.createCell(25).setCellValue(""); // Tax_CostLimit
                row.createCell(26).setCellValue(""); // Tax_ResidualValue
                row.createCell(27).setCellValue(""); // Tax_AveragingMethod
                row.createCell(28).setCellValue(getValue(taxRates, i));
                row.createCell(29).setCellValue(""); // Tax_EffectiveLife
                row.createCell(30).setCellValue(""); // Tax_OpeningAccumulatedDepreciation
            }

            // Write to the Excel file
            try (FileOutputStream fileOut = new FileOutputStream(excelFile)) {
                workbook.write(fileOut);
                System.out.println("Excel file created successfully at: " + excelFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Error while writing to the Excel file: " + e.getMessage());
        }
    }

    private static String getValue(List<String> list, int index) {
        return index < list.size() ? list.get(index) : "";
    }

    public static String convertXlsToCsv(File xlsFile) throws IOException {
        // You can implement XLS to CSV conversion logic here using libraries like Apache POI
        // This is a simplified version that just gives a placeholder for the path
        return xlsFile.getAbsolutePath().replace(".xls", ".csv");
    }
}
