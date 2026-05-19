package com.maintaintrack.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection — single source of truth for the SQLite connection.
 *
 * Development:  data/maintaintrack.db  (project root)
 * Production:   C:\Users\<name>\AppData\Roaming\MaintainTrackPro\maintaintrack.db
 *
 * Detection: if AppData db exists → use it (production).
 * Otherwise fall back to local data/ folder (development).
 * This means dev and prod never conflict — different paths entirely.
 */
public class DBConnection {

    private static final String DB_PATH = resolveDbPath();

    private static String resolveDbPath() {
        // Production path — AppData
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            File prodDir = new File(appData + File.separator + "MaintainTrackPro");
            File prodDb  = new File(prodDir, "maintaintrack.db");
            // Use AppData if the folder already exists (app was installed)
            // OR if we're running from a jpackage install (no scripts/ folder present)
            File scriptsDir = new File("scripts");
            if (!scriptsDir.exists() || prodDb.exists()) {
                prodDir.mkdirs();
                return prodDb.getAbsolutePath();
            }
        }

        // Development path — local data/ folder
        new File("data").mkdirs();
        return "data" + File.separator + "maintaintrack.db";
    }

    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /** Returns the resolved DB path — useful for logging and diagnostics. */
    public static String getDbPath() { return DB_PATH; }
}
