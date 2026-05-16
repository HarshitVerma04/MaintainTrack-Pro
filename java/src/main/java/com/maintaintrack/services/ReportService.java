package com.maintaintrack.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * ReportService — bridges Java to Python report scripts.
 * Uses ProcessBuilder to call Python from Java.
 * Output directory: reports/ (auto-created)
 */
public class ReportService {

    private static final String REPORTS_DIR = "reports";
    private static final String SCRIPTS_DIR = "scripts";
    private static final String PYTHON_CMD  = getPythonCommand();

    private static String getPythonCommand() {
        return System.getProperty("os.name").toLowerCase().contains("win")
                ? "python" : "python3";
    }

    public String generateMaintenanceReport(int equipmentId,
                                             String equipmentName) throws Exception {
        Files.createDirectories(Paths.get(REPORTS_DIR));

        String safeName    = equipmentName.replaceAll("[^a-zA-Z0-9]", "_");
        String outputPath  = REPORTS_DIR + File.separator
                + "report_" + safeName + "_" + equipmentId + ".pdf";

        ProcessBuilder pb = new ProcessBuilder(
                PYTHON_CMD,
                SCRIPTS_DIR + File.separator + "generate_report.py",
                "--equipment-id", String.valueOf(equipmentId),
                "--output", outputPath
        );
        pb.directory(new File(System.getProperty("user.dir")));
        pb.redirectErrorStream(true);

        Process process = pb.start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append("\n");
                System.out.println("[Python] " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0)
            throw new Exception("Python script failed (exit " + exitCode + "):\n" + out);

        return new File(outputPath).getAbsolutePath();
    }

    public void openFile(String absolutePath) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;
        if (os.contains("win"))       pb = new ProcessBuilder("cmd", "/c", "start", "", absolutePath);
        else if (os.contains("mac"))  pb = new ProcessBuilder("open", absolutePath);
        else                          pb = new ProcessBuilder("xdg-open", absolutePath);
        pb.directory(new File(System.getProperty("user.dir")));
        pb.start();
    }
}
