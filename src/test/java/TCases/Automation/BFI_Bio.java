package TCases.Automation;

import java.sql.*;
import java.util.*;
import java.io.*;

import org.testng.annotations.Test;

public class BFI_Bio {
    private static final String DB_URL = "jdbc:mysql://apollo2.humanbrain.in:3306/HBA_V2";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Health#123";

    @Test
    public void testBFIQuery() {
        Scanner scanner = new Scanner(System.in);

        // Manually entering biosample ID
        System.out.print("Enter biosample ID: ");
        String biosampleId = scanner.nextLine().trim();

        // Manually entering limit value
        System.out.print("Enter the limit value: ");
        int limit = scanner.nextInt();
        scanner.nextLine(); 

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

        Set<Integer> bfiNumbers = new TreeSet<>();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(queryBFI)) {

            pstmt.setString(1, biosampleId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String numStr = rs.getString("bfi_section_number");
                if (numStr != null && !numStr.isEmpty()) {
                    try {
                        int num = Integer.parseInt(numStr);
                        if (num <= limit) { 
                            bfiNumbers.add(num);
                        }
                    } catch (NumberFormatException nfe) {
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        List<Integer> presentBFI = new ArrayList<>(bfiNumbers);
        List<Integer> missingBFI = computeMissingNumbers(presentBFI, limit);

        StringBuilder report = new StringBuilder();
        report.append("\n================== BFI Report ==================\n");
        report.append("-------------------------------------\n");
        report.append(String.format("| %-15s | %-15s |\n", "Present BFI", "Missing BFI"));
        report.append("-------------------------------------\n");
        int maxRows = Math.max(presentBFI.size(), missingBFI.size());
        for (int i = 0; i < maxRows; i++) {
            String present = i < presentBFI.size() ? String.valueOf(presentBFI.get(i)) : "";
            String missing = i < missingBFI.size() ? String.valueOf(missingBFI.get(i)) : "";
            report.append(String.format("| %-15s | %-15s |\n", present, missing));
        }
        report.append("-------------------------------------\n");
        System.out.println(report.toString());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("BFI_Output.txt"))) {
            writer.write(report.toString());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private List<Integer> computeMissingNumbers(List<Integer> presentNumbers, int limit) {
        List<Integer> missing = new ArrayList<>();
        for (int i = 1; i <= limit; i++) {
            if (!presentNumbers.contains(i)) {
                missing.add(i);
            }
        }
        return missing;
    }
}
