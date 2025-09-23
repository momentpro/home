package com.autocaller.app.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.autocaller.app.model.PhoneNumber;

// Simplified version - only CSV support for now
// import org.apache.poi.ss.usermodel.Cell;
// import org.apache.poi.ss.usermodel.Row;
// import org.apache.poi.ss.usermodel.Sheet;
// import org.apache.poi.ss.usermodel.Workbook;
// import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import android.database.Cursor;
import android.provider.OpenableColumns;

public class FileImportUtil {
    private static final String TAG = "FileImportUtil";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\-\\s()]{7,20}$");

    public static List<PhoneNumber> importFromFile(Context context, Uri uri) throws IOException {
        List<PhoneNumber> phoneNumbers = new ArrayList<>();
        
        // Get file name from URI - try multiple methods
        String fileName = getFileName(context, uri);
        Log.d(TAG, "Attempting to import file: " + fileName + " from URI: " + uri.toString());
        
        // Always try CSV first (most flexible approach)
        Log.d(TAG, "Attempting to import as CSV/text file...");
        try {
            phoneNumbers = importFromCsv(context, uri);
            Log.d(TAG, "Successfully imported as CSV/text file");
        } catch (Exception e) {
            Log.e(TAG, "Failed to import file", e);
            
            // Check if it might be an Excel file
            if (isExcelFile(fileName)) {
                throw new IOException("Excel files are temporarily not supported. Please save as CSV file.");
            } else {
                throw new IOException("Could not read file. Please ensure it's a text file with phone numbers (one per line).");
            }
        }

        Log.d(TAG, "Imported " + phoneNumbers.size() + " phone numbers from " + fileName);
        return phoneNumbers;
    }

    private static List<PhoneNumber> importFromCsv(Context context, Uri uri) throws IOException {
        List<PhoneNumber> phoneNumbers = new ArrayList<>();
        
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            
            String line;
            int index = 0;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                if (!line.isEmpty()) {
                    Log.d(TAG, "Processing line " + lineNumber + ": " + line);
                    
                    // Try different parsing strategies
                    String phoneNumber = extractPhoneNumber(line);
                    
                    if (isValidPhoneNumber(phoneNumber)) {
                        phoneNumbers.add(new PhoneNumber(phoneNumber, index++));
                        Log.d(TAG, "Added phone number: " + phoneNumber);
                    } else {
                        Log.w(TAG, "Invalid phone number on line " + lineNumber + ": " + line);
                    }
                }
            }
        }
        
        if (phoneNumbers.isEmpty()) {
            throw new IOException("No valid phone numbers found in the file. Please check the file format.");
        }
        
        return phoneNumbers;
    }
    
    private static String extractPhoneNumber(String line) {
        // Try multiple extraction strategies
        
        // Strategy 1: Split by comma and take first part
        String[] commaParts = line.split(",");
        String candidate1 = cleanPhoneNumber(commaParts[0].trim());
        if (isValidPhoneNumber(candidate1)) {
            return candidate1;
        }
        
        // Strategy 2: Split by semicolon
        String[] semicolonParts = line.split(";");
        String candidate2 = cleanPhoneNumber(semicolonParts[0].trim());
        if (isValidPhoneNumber(candidate2)) {
            return candidate2;
        }
        
        // Strategy 3: Split by tab
        String[] tabParts = line.split("\\t");
        String candidate3 = cleanPhoneNumber(tabParts[0].trim());
        if (isValidPhoneNumber(candidate3)) {
            return candidate3;
        }
        
        // Strategy 4: Use the whole line
        String candidate4 = cleanPhoneNumber(line.trim());
        if (isValidPhoneNumber(candidate4)) {
            return candidate4;
        }
        
        // Strategy 5: Extract first number-like sequence
        String numberPattern = "[+]?[0-9\\-\\s()]{7,20}";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(numberPattern);
        java.util.regex.Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String candidate5 = cleanPhoneNumber(matcher.group());
            if (isValidPhoneNumber(candidate5)) {
                return candidate5;
            }
        }
        
        return "";
    }

    // Excel support temporarily disabled for compatibility
    /*
    private static List<PhoneNumber> importFromExcel(Context context, Uri uri) throws IOException {
        List<PhoneNumber> phoneNumbers = new ArrayList<>();
        
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            
            Sheet sheet = workbook.getSheetAt(0); // Get first sheet
            int index = 0;
            
            for (Row row : sheet) {
                Cell firstCell = row.getCell(0);
                if (firstCell != null) {
                    String phoneNumber = getCellValueAsString(firstCell);
                    phoneNumber = cleanPhoneNumber(phoneNumber);
                    
                    if (isValidPhoneNumber(phoneNumber)) {
                        phoneNumbers.add(new PhoneNumber(phoneNumber, index++));
                    }
                }
            }
        }
        
        return phoneNumbers;
    }

    private static String getCellValueAsString(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // Handle phone numbers stored as numbers
                double numericValue = cell.getNumericCellValue();
                return String.valueOf((long) numericValue);
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    */

    private static String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        
        // Remove common formatting characters but preserve phone number structure
        String cleaned = phoneNumber.trim()
                .replaceAll("\\s+", ""); // Remove all whitespace
        
        // Fix Korean phone numbers that lost leading zero
        if (cleaned.matches("^10-\\d{4}-\\d{4}$")) {
            // 10-1234-5678 -> 010-1234-5678
            cleaned = "0" + cleaned;
        } else if (cleaned.matches("^2-\\d{3,4}-\\d{4}$")) {
            // 2-123-4567 -> 02-123-4567
            cleaned = "0" + cleaned;
        } else if (cleaned.matches("^31-\\d{3,4}-\\d{4}$")) {
            // 31-123-4567 -> 031-123-4567
            cleaned = "0" + cleaned;
        } else if (cleaned.matches("^32-\\d{3,4}-\\d{4}$")) {
            // 32-123-4567 -> 032-123-4567
            cleaned = "0" + cleaned;
        } else if (cleaned.matches("^33-\\d{3,4}-\\d{4}$")) {
            // 33-123-4567 -> 033-123-4567
            cleaned = "0" + cleaned;
        } else if (cleaned.matches("^\\d{9}$")) {
            // 103614306 -> 010-3614-3064 (9 digits without dashes)
            cleaned = "0" + cleaned.substring(0, 2) + "-" + cleaned.substring(2, 6) + "-" + cleaned.substring(6);
        } else if (cleaned.matches("^\\d{8}$")) {
            // 21234567 -> 02-1234-5678 (8 digits Seoul number)
            cleaned = "0" + cleaned.substring(0, 1) + "-" + cleaned.substring(1, 5) + "-" + cleaned.substring(5);
        }
        
        return cleaned;
    }

    private static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }
        
        // Check for Korean phone number patterns
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");
        
        // Valid Korean phone numbers:
        // 010-xxxx-xxxx (11 digits)
        // 02-xxx-xxxx or 02-xxxx-xxxx (9-10 digits)
        // 031/032/033-xxx-xxxx (10-11 digits)
        if (digitsOnly.length() >= 8 && digitsOnly.length() <= 11) {
            // Check specific patterns
            return phoneNumber.matches("^01[0-9]-\\d{4}-\\d{4}$") ||  // 010-1234-5678
                   phoneNumber.matches("^02-\\d{3,4}-\\d{4}$") ||     // 02-123-4567
                   phoneNumber.matches("^0[3-6][0-9]-\\d{3,4}-\\d{4}$"); // 031-123-4567
        }
        
        return false;
    }

    private static String getFileName(Context context, Uri uri) {
        String fileName = null;
        
        // Try to get the actual file name from the content resolver
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not get file name from content resolver", e);
            }
        }
        
        // Fallback to URI path
        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }
        
        // Final fallback
        if (fileName == null) {
            fileName = "unknown_file";
        }
        
        return fileName;
    }

    private static boolean isCSVFile(String fileName, Context context, Uri uri) {
        if (fileName != null) {
            String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".csv") || lowerName.endsWith(".txt")) {
                return true;
            }
        }
        
        // Try to detect by MIME type
        try {
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType != null) {
                return mimeType.equals("text/csv") || mimeType.equals("text/plain") || 
                       mimeType.equals("application/csv");
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not get MIME type", e);
        }
        
        return false;
    }

    private static boolean isExcelFile(String fileName) {
        if (fileName != null) {
            String lowerName = fileName.toLowerCase();
            return lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls");
        }
        return false;
    }
}
