package com.maintaintrack.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * ReportService — bridges Java to Python report scripts.
 *
 * Development: calls python scripts/generate_report.py
 * Production:  calls bundled/generate_report.exe (no Python needed)
 *
 * Detection is automatic — if the .exe exists, it's used.
 * If not, falls back to python command.
 */
public class ReportService {

    private static final String REPORTS_DIR = getReportsDir();
    private static final String APP_DATA_DIR = getAppDataDir();

    // ── Path resolution ───────────────────────────────────────────────────

    /**
     * AppData path for production, local reports/ for development.
     */
    private static String getAppDataDir() {
        String appData = System.getenv("APPDATA");
        if (appData == null) appData = System.getProperty("user.home");
        return appData + File.separator + "MaintainTrackPro";
    }

    private static String getReportsDir() {
        return getAppDataDir() + File.separator + "reports";
    }

    /**
     * Detects whether to call the bundled exe or python script.
     * Checks for bundled exe relative to the working directory.
     */
    private static File getBundledExe(String scriptName) {
        // When running from jpackage, exe is next to the JAR
        File inBundled = new File("bundled" + File.separator + scriptName + ".exe");
        if (inBundled.exists()) return inBundled;

        // Also check relative to the app install dir
        String appDir = System.getProperty("user.dir");
        File inApp = new File(appDir + File.separator + "bundled"
                + File.separator + scriptName + ".exe");
        if (inApp.exists()) return inApp;

        return null;
    }

    // ── PDF Report ────────────────────────────────────────────────────────

    public String generateMaintenanceReport(int equipmentId,
                                             String equipmentName) throws Exception {
        Files.createDirectories(Paths.get(REPORTS_DIR));

        String safeName   = equipmentName.replaceAll("[^a-zA-Z0-9]", "_");
        String outputPath = REPORTS_DIR + File.separator
                + "report_" + safeName + "_" + equipmentId + ".pdf";

        File bundledExe = getBundledExe("generate_report");
        ProcessBuilder pb;

        if (bundledExe != null) {
            // Production — bundled exe, no Python needed
            pb = new ProcessBuilder(
                    bundledExe.getAbsolutePath(),
                    "--equipment-id", String.valueOf(equipmentId),
                    "--output", outputPath
            );
        } else {
            // Development — call Python script
            String python = System.getProperty("os.name")
                    .toLowerCase().contains("win") ? "python" : "python3";
            pb = new ProcessBuilder(
                    python,
                    "scripts" + File.separator + "generate_report.py",
                    "--equipment-id", String.valueOf(equipmentId),
                    "--output", outputPath
            );
        }

        return runProcess(pb, outputPath);
    }

    // ── Excel Export ──────────────────────────────────────────────────────

    public String generateExcelExport() throws Exception {
        Files.createDirectories(Paths.get(REPORTS_DIR));

        File bundledExe = getBundledExe("export_parts");
        ProcessBuilder pb;

        if (bundledExe != null) {
            pb = new ProcessBuilder(bundledExe.getAbsolutePath());
        } else {
            String python = System.getProperty("os.name")
                    .toLowerCase().contains("win") ? "python" : "python3";
            pb = new ProcessBuilder(python,
                    "scripts" + File.separator + "export_parts.py");
        }

        // Override output dir so Excel lands in AppData reports folder
        pb.environment().put("MAINTAINTRACK_REPORTS_DIR", REPORTS_DIR);
        return runProcess(pb, null);
    }

    // ── Open File ─────────────────────────────────────────────────────────

    public void openFile(String absolutePath) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;
        if (os.contains("win"))
            pb = new ProcessBuilder("cmd", "/c", "start", "", absolutePath);
        else if (os.contains("mac"))
            pb = new ProcessBuilder("open", absolutePath);
        else
            pb = new ProcessBuilder("xdg-open", absolutePath);
        pb.directory(new File(System.getProperty("user.dir")));
        pb.start();
    }

    // ── Process runner ────────────────────────────────────────────────────

    private String runProcess(ProcessBuilder pb, String expectedOutput) throws Exception {
        pb.directory(new File(System.getProperty("user.dir")));
        pb.redirectErrorStream(true);

        Process process = pb.start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append("\n");
                System.out.println("[Report] " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0)
            throw new Exception("Process failed (exit " + exitCode + "):\n" + out);

        // Extract saved path from output if no explicit path given
        if (expectedOutput != null) return new File(expectedOutput).getAbsolutePath();

        String output = out.toString();
        if (output.contains("->")) {
            return output.substring(output.lastIndexOf("->") + 2).trim();
        }
        return REPORTS_DIR;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    public String getReportsDirPath() { return REPORTS_DIR; }
    public String getAppDataDirPath() { return APP_DATA_DIR; }
}
