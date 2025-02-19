package TCases.Automation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class BFI_Bio {
    private static final String DB_URL = "jdbc:mysql://dev2mani.humanbrain.in:3306/HBA_V2";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Health#123";
    // Fixed range from 1 to 3000
    private static final int LIMIT = 3000;

    @Parameters({"biosampleId"})
    @Test
    public void testBFIQuery(@Optional("0") String biosampleId) {
        // Prompt user if biosampleId is not provided via TestNG.
        if ("0".equals(biosampleId)) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter biosample ID: ");
            biosampleId = scanner.nextLine();
        }

        // --- New Query for BFI using the pattern "BFI-SE_" ---
        // This query extracts the section number from s.jp2Path.
        String queryBFI = "SELECT DISTINCT " +
                          "SUBSTRING( " +
                          "s.jp2Path, " +
                          "INSTR(s.jp2Path, 'BFI-SE_') + LENGTH('BFI-SE_'), " +
                          "LOCATE('.jp2', s.jp2Path) - (INSTR(s.jp2Path, 'BFI-SE_') + LENGTH('BFI-SE_')) " +
                          ") AS bfi_section_number " +
                          "FROM section s " +
                          "JOIN biosample b ON s.jp2Path LIKE CONCAT('%/', b.id, '/%') " +
                          "WHERE b.id = ? " +
                          "AND s.jp2Path LIKE '%BFI-SE_%' " +
                          "ORDER BY CAST(bfi_section_number AS UNSIGNED)";

        // Retrieve BFI section numbers into a sorted set to avoid duplicates and for sorting.
        Set<Integer> bfiNumbers = new TreeSet<>();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(queryBFI)) {

            // Set the parameter for b.id (biosampleId)
            pstmt.setString(1, biosampleId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String numStr = rs.getString("bfi_section_number");
                if (numStr != null && !numStr.isEmpty()) {
                    try {
                        int num = Integer.parseInt(numStr);
                        if (num <= LIMIT) { // include only numbers within the fixed range
                            bfiNumbers.add(num);
                        }
                    } catch (NumberFormatException nfe) {
                        // Skip non-numeric values
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Convert the set to a list for processing
        List<Integer> presentBFI = new ArrayList<>(bfiNumbers);
        // Compute missing numbers in the fixed range 1 to LIMIT that are not present in the query result
        List<Integer> missingBFI = computeMissingNumbers(presentBFI);

        // Build a report table with two columns: "Present BFI" and "Missing BFI"
        StringBuilder report = new StringBuilder();
        report.append("\n================== BFI Report ==================\n");
        report.append("------------------------------------------------------------\n");
        report.append(String.format("| %-20s | %-20s |\n", "Present BFI", "Missing BFI"));
        report.append("------------------------------------------------------------\n");

        int maxRows = Math.max(presentBFI.size(), missingBFI.size());
        for (int i = 0; i < maxRows; i++) {
            String present = i < presentBFI.size() ? String.valueOf(presentBFI.get(i)) : "";
            String missing = i < missingBFI.size() ? String.valueOf(missingBFI.get(i)) : "";
            report.append(String.format("| %-20s | %-20s |\n", present, missing));
        }
        report.append("------------------------------------------------------------\n");

        // Print the report to the console
        System.out.println(report.toString());

        // Write the report to a file "BFI_Output.txt"
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("BFI_Output.txt"))) {
            writer.write(report.toString());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Computes missing numbers in the range 1 to LIMIT that are not in the provided list.
     *
     * @param presentNumbers List of numbers present from the query
     * @return List of missing numbers in the fixed range
     */
    private List<Integer> computeMissingNumbers(List<Integer> presentNumbers) {
        List<Integer> missing = new ArrayList<>();
        for (int i = 1; i <= LIMIT; i++) {
            if (!presentNumbers.contains(i)) {
                missing.add(i);
            }
        }
        return missing;
    }
}
