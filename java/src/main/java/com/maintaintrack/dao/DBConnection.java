package com.maintaintrack.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection — single source of truth for the SQLite connection.
 *
 * The database file lives at:  data/maintaintrack.db
 * (relative to wherever the app is launched from — project root in dev,
 *  next to the JAR in production)
 *
 * Usage:
 *   try (Connection conn = DBConnection.getConnection()) {
 *       // use conn
 *   }
 * The try-with-resources pattern auto-closes the connection.
 */
public class DBConnection {

    private static final String DB_PATH = "data/maintaintrack.db";
    private static final String URL     = "jdbc:sqlite:" + DB_PATH;

    // Private constructor — this class should never be instantiated
    private DBConnection() {}

    /**
     * Opens and returns a new connection to the SQLite database.
     * Caller is responsible for closing it (use try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        new java.io.File("data").mkdirs(); // creates data/ if missing
        return DriverManager.getConnection(URL);
    }
}
