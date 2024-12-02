package conversion;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class XlsToCsvConverter {
    List<String> col4Below1000List = new ArrayList<>();
	 List<String> colJList = new ArrayList<>();
     List<String> colVList = new ArrayList<>();
 	List<String> col1List = new ArrayList<>();
	List<String> col2List = new ArrayList<>();
	List<String> col4List = new ArrayList<>();
	List<Double> col4NumericList = new ArrayList<>();  
	  List<Double> colVDifferenceList = new ArrayList<>(); 
	    List<String> col4SkippedList = new ArrayList<>();
	
	public static void main(String[] args) {
		XlsToCsvConverter converter = new XlsToCsvConverter();
		String inputFilePath = "C:\\Users\\test\\OneDrive - The Outsource Pro\\Desktop\\DeliverablesMehul\\Seitei Trust\\Seitei Trust - MYOB FA Schedule.xls";
		String csvFilePath = converter.convertXlsToCsv(inputFilePath);
		if (!csvFilePath.isEmpty()) {
			converter.writeCsvToDownloads(csvFilePath);
			converter.readSpecificColumns(csvFilePath);
			 converter.readColumnsJAndV(csvFilePath);   
			 converter.createExcelFileWithHeaders();
		}
	}

	public String convertXlsToCsv(String xlsFilePath) {
		String csvFilePath = xlsFilePath.replace(".xls", ".csv");
		File xlsFile = new File(xlsFilePath);

		if (!xlsFile.exists() || !xlsFile.isFile()) {
			System.err.println("Error: The specified XLS file does not exist.");
			return "";
		}

		try (Workbook workbook = new HSSFWorkbook(new FileInputStream(xlsFile));
				BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath))) {

			Sheet sheet = workbook.getSheetAt(0);
			int maxColumns = getMaxColumns(sheet);

			for (Row row : sheet) {
				writeCsvRow(writer, row, maxColumns);
			}

		} catch (IOException e) {
			System.err.println("Error during XLS to CSV conversion: " + e.getMessage());
		}

		return csvFilePath;
	}

	private int getMaxColumns(Sheet sheet) {
		int maxColumns = 0;
		for (Row row : sheet) {
			maxColumns = Math.max(maxColumns, row.getLastCellNum());
		}
		return maxColumns;
	}

	private void writeCsvRow(BufferedWriter writer, Row row, int maxColumns) throws IOException {
		for (int i = 0; i < maxColumns; i++) {
			Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
			String cellValue = cell.toString().replaceAll("\n", " ").replaceAll("\r", "");
			writer.write(cellValue);

			if (i < maxColumns - 1) {
				writer.write(",");
			}
		}
		writer.newLine();
	}

	public void writeCsvToDownloads(String csvFilePath) {
		String downloadsDir = System.getProperty("user.home") + "/Downloads/";
		Path outputPath = Paths.get(downloadsDir, new File(csvFilePath).getName());

		try {
			Files.copy(Paths.get(csvFilePath), outputPath);
			System.out.println("CSV file saved to: " + outputPath.toString());
		} catch (IOException e) {
			System.err.println("Error saving the CSV file to Downloads: " + e.getMessage());
		}
	}

	public void readSpecificColumns(String csvFilePath) {

			try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
			String line;
			int rowNum = 0; 

			while ((line = reader.readLine()) != null) {
				rowNum++;
				if (rowNum <= 10) {
					continue;
				}

				String[] columns = line.split(",");

				if (columns.length >= 4) {
					String column1 = columns[0].trim();
					String column2 = columns[1].trim();
					String column4 = columns[3].trim();
//					String columnJ = columns[9].trim();

					if (!column4.isEmpty()) {
						if (!column1.isEmpty()) {
							col1List.add(column1);
						}
						if (!column2.isEmpty()) {
							col2List.add(column2);
						}
						
						try {
							double col4Value = Double.parseDouble(column4);
							if (!col4NumericList.contains(col4Value)) {  // Remove duplicates from numeric list
//								col4NumericList.add(col4Value);
							}
						} catch (NumberFormatException e) {
							col4List.add(column4);
						}
					}
					try {
	                    double col1Value = Double.parseDouble(column1);
	                    if (col1Value < 1000) {
	                        col4Below1000List.add(column4);  // Add corresponding col4 value to list
	                    }
	                } catch (NumberFormatException e) {
	                    // Handle the case where column1 is not a number
//	                    System.err.println("Non-numeric value in column 1 at row " + rowNum + ": " + column1);
	                }
					
					 try {
	                        double value = Double.parseDouble(column4);

	                        // Check if the value is the sum of any combination of previous values
	                        if (!isSumOfAnyCombination(value, col4NumericList)) {
	                            col4NumericList.add(value);
	                        } else {
	                            System.out.println("Skipped value: " + value);
	                        }
	                    } catch (NumberFormatException e) {
	                        // Ignore non-numeric values
	                    }
					
				}
			}

			System.out.println("Column 1 List: " + col1List);
			System.out.println("Column 2 List: " + col2List);
			System.out.println("Column 4 List: " + col4List);
			System.out.println("col4NumericList List: " + col4NumericList);
			  System.out.println("col4Below1000List: " + col4Below1000List);

		} catch (IOException e) {
			System.err.println("Error reading the CSV file: " + e.getMessage());
		}
	}
	private boolean isSumOfAnyCombination(double value, List<Double> list) {
        int n = list.size();
        // Use a bitmask to find all subsets
        for (int i = 0; i < (1 << n); i++) {
            double sum = 0;

            for (int j = 0; j < n; j++) {
                if ((i & (1 << j)) != 0) { // Check if j-th element is in the subset
                    sum += list.get(j);
                }
            }

            if (Math.abs(sum - value) < 1e-9) { // Check with a small tolerance to handle floating-point precision
                return true;
            }
        }
        return false;
    }
	public void readColumnsJAndV(String csvFilePath) {

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            int rowNum = 0;

            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (rowNum <= 10) {
                    continue; // Skip header rows
                }

                String[] columns = line.split(",");

                if (columns.length >= 22) { // Ensure the row has at least 22 columns
                    String columnJ = columns[9].trim();  // Column J is index 9
                    String columnV = columns[21].trim(); // Column V is index 21

                    if (!columnJ.isEmpty()) {
                        colJList.add(columnJ);
                    }
                    if (!columnV.isEmpty()) {
                        colVList.add(columnV);
                    }
                } 
                int iterations = Math.min(colVList.size(), col4NumericList.size());
                colVDifferenceList.clear(); // Clear any existing data to avoid appending repeatedly

                for (int i = 0; i < iterations; i++) {
                    try {
                        double colVValue = Double.parseDouble(colVList.get(i)); // Convert colVList element to double
                        double col4Value = col4NumericList.get(i);              // Get corresponding col4NumericList element
                        double difference = col4Value - colVValue ;              // Calculate difference
                        colVDifferenceList.add(difference);                     // Store difference
                    } catch (NumberFormatException e) {
                        System.err.println("Non-numeric value in colVList at index " + i + ": " + colVList.get(i));
                    }
                }
            }

            // Print the collected values for verification
            System.out.println("Column J List: " + colJList);
            System.out.println("Column V List: " + colVList);
            System.out.println("Column V Difference List: " + colVDifferenceList);

        } catch (IOException e) {
            System.err.println("Error reading the CSV file: " + e.getMessage());
        }
    }
	public void createExcelFileWithHeaders() {
	    // Define the headers
	    String[] headers = {
	        "AssetName", "AssetNumber", "PurchaseDate", "PurchasePrice", "AssetType", "Description",
	        "TrackingCategory1", "TrackingOption1", "TrackingCategory2", "TrackingOption2", "SerialNumber",
	        "WarrantyExpiry", "Book_DepreciationStartDate", "Book_CostLimit", "Book_ResidualValue", 
	        "Book_DepreciationMethod", "Book_AveragingMethod", "Book_Rate", "Book_EffectiveLife", 
	        "Book_OpeningBookAccumulatedDepreciation", "Tax_DepreciationMethod", "Tax_PoolName", 
	        "Tax_PooledDate", "Tax_PooledAmount", "Tax_DepreciationStartDate", "Tax_CostLimit", 
	        "Tax_ResidualValue", "Tax_AveragingMethod", "Tax_Rate", "Tax_EffectiveLife", 
	        "Tax_OpeningAccumulatedDepreciation"
	    };
	    Set<String> writtenItems = new HashSet<>(); 
	    // Create a new workbook and sheet
	    Workbook workbook = new XSSFWorkbook();
	    Sheet sheet = workbook.createSheet("AssetData");

	    // Create the header row
	    Row headerRow = sheet.createRow(0);

	    // Add headers to the first row
	    for (int i = 0; i < headers.length; i++) {
	        Cell cell = headerRow.createCell(i);
	        cell.setCellValue(headers[i]);
	    }

	    // Get the maximum size of lists for iteration (use the longest list to avoid index out of bounds errors)
	    int maxRows = Math.max(Math.max(Math.max(col1List.size(), col2List.size()), Math.max(col4List.size(), col4NumericList.size())),
	                          Math.max(Math.max(colJList.size(), colVList.size()), colVDifferenceList.size()));

	    // Start creating rows and populating data
	    for (int rowIndex = 0; rowIndex < maxRows; rowIndex++) {
	        Row row = sheet.createRow(rowIndex + 1); // Start from row 1 to avoid overwriting header

	        // Populate the columns
	        if (rowIndex < col1List.size()) {
	            row.createCell(1).setCellValue(col1List.get(rowIndex)); // AssetName
	        }

	        if (rowIndex < col2List.size()) {
	            row.createCell(2).setCellValue(col2List.get(rowIndex)); // AssetNumber
	        }
	        if (rowIndex < col2List.size()) {
	            row.createCell(12).setCellValue(col2List.get(rowIndex)); // AssetNumber
	        }
	        if (rowIndex < col2List.size()) {
	            row.createCell(25).setCellValue(col2List.get(rowIndex)); // AssetNumber
	        }
	        if (rowIndex < col4List.size()) {
	            row.createCell(0).setCellValue(col4List.get(rowIndex)); // PurchasePrice
	        }

	        if (rowIndex < col4NumericList.size()) {
	            row.createCell(3).setCellValue(col4NumericList.get(rowIndex)); // Book_CostLimit
	        }

	        if (rowIndex < colJList.size()) {
	            row.createCell(17).setCellValue(colJList.get(rowIndex)); // TrackingCategory1
	        }

	        if (rowIndex < colVList.size()) {
	            row.createCell(19).setCellValue(colVList.get(rowIndex)); // Tax_PooledAmount
	        }
	        if (rowIndex < colJList.size()) {
	            row.createCell(28).setCellValue(colJList.get(rowIndex)); // TrackingCategory1
	        }

	        if (rowIndex < colVDifferenceList.size()) {
	            row.createCell(30).setCellValue(colVDifferenceList.get(rowIndex)); // Tax_OpeningAccumulatedDepreciation
	        }
	        if (rowIndex < maxRows) {
	            Row currentRow = sheet.getRow(rowIndex + 1); // Get the current row
	            if (currentRow != null) {
	                Cell cellD = currentRow.getCell(3); // Column D is index 3
	                if (cellD != null && !cellD.toString().isEmpty()) {
	                    // Write "Actual Days" in column Q (index 16)
	                    currentRow.createCell(16).setCellValue("Actual Days");
	                    currentRow.createCell(27).setCellValue("Actual Days");
	                }
	            }
	         // Check if col1List value matches any item in col4Below1000List
	            for (String col1Value : col1List) {
	                for (String col4Below1000Value : col4Below1000List) {
	                    if (col1Value.equals(col4Below1000Value) || !col1Value.contains(col4Below1000Value)) {
	                        // Write the matching value to column 5
	                        row.createCell(4).setCellValue(col4Below1000Value); // Column 5 (index 4)
	                        writtenItems.add(col4Below1000Value);  // Mark this value as written
	                    }
	                }
	            }

	            // If an empty cell is found in col4, stop writing further
	            if (rowIndex < col4List.size() && col4List.get(rowIndex).isEmpty()) {
	                break; // Stop writing further
	            }
	        }
	    }
	    // Create a path for saving the Excel file in Downloads folder
	    String downloadsDir = System.getProperty("user.home") + "/Downloads/";
	    Path outputPath = Paths.get(downloadsDir, "AssetData.xlsx");

	    // Write the workbook to the file
	    try (FileOutputStream fileOut = new FileOutputStream(outputPath.toFile())) {
	        workbook.write(fileOut);
	        System.out.println("Excel file created and saved to: " + outputPath.toString());
	    } catch (IOException e) {
	        System.err.println("Error writing Excel file: " + e.getMessage());
	    } finally {
	        try {
	            workbook.close();
	        } catch (IOException e) {
	            System.err.println("Error closing workbook: " + e.getMessage());
	        }
	    }
	}

}
