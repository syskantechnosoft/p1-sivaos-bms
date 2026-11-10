package com.banking.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility for formatting and printing aligned tables in the console.
 */
public class TablePrinter {

    private final List<String> headers;
    private final List<List<String>> rows = new ArrayList<>();

    public TablePrinter(String... headers) {
        this.headers = Arrays.asList(headers);
    }

    public void addRow(String... cells) {
        rows.add(Arrays.asList(cells));
    }

    public void print() {
        int[] colWidths = new int[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            colWidths[i] = headers.get(i).length();
        }

        for (List<String> row : rows) {
            for (int i = 0; i < row.size() && i < colWidths.length; i++) {
                String cell = row.get(i) != null ? row.get(i) : "";
                if (cell.length() > colWidths[i]) {
                    colWidths[i] = cell.length();
                }
            }
        }

        StringBuilder line = new StringBuilder("+");
        for (int w : colWidths) {
            line.append("-".repeat(w + 2)).append("+");
        }
        String separator = line.toString();

        System.out.println(separator);

        // Print header
        System.out.print("|");
        for (int i = 0; i < headers.size(); i++) {
            System.out.printf(" %-" + colWidths[i] + "s |", headers.get(i));
        }
        System.out.println();
        System.out.println(separator);

        // Print rows
        if (rows.isEmpty()) {
            int totalWidth = separator.length() - 2;
            System.out.printf("| %-" + (totalWidth - 1) + "s|\n", "No records found.");
        } else {
            for (List<String> row : rows) {
                System.out.print("|");
                for (int i = 0; i < colWidths.length; i++) {
                    String cell = (i < row.size() && row.get(i) != null) ? row.get(i) : "";
                    System.out.printf(" %-" + colWidths[i] + "s |", cell);
                }
                System.out.println();
            }
        }
        System.out.println(separator);
    }
}
