
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Day 1 — Database singleton.
 *
 * Provides a single shared {@link Connection} backed by SQLite.
 * On first call to {@link #getConnection()} the schema is auto-initialised
 * from {@code schema.sql} so the app starts cleanly on any machine.
 *
 * The DB file lives in the user's home directory under
 * {@code .maintaintrack/maintaintrack.db} so it survives re-installs.
 */
public final class DatabaseManager {

    // ---- Configuration --------------------------------------
    private static final String DB_DIR  = System.getProperty("user.home") + "/.maintaintrack";
    private static final String DB_FILE = DB_DIR + "/maintaintrack.db";
    private static final String SCHEMA_RESOURCE = "/schema.sql";

    // ---- Singleton state ------------------------------------
    private static Connection instance;

    private DatabaseManager() {}   // no instances

    // ---- Public API ----------------------------------------

    /**
     * Returns the shared SQLite connection, creating + seeding the DB if needed.
     *
     * @throws RuntimeException if the connection cannot be established
     */
    public static synchronized Connection getConnection() {
        if (instance == null) {
            instance = connect();
        }
        try {
            // Reconnect if the connection was closed externally
            if (instance.isClosed()) {
                instance = connect();
            }
        } catch (SQLException e) {
            instance = connect();
        }
        return instance;
    }

    /**
     * Closes the shared connection. Call on application exit.
     */
    public static synchronized void close() {
        if (instance != null) {
            try {
                instance.close();
            } catch (SQLException ignored) {
            } finally {
                instance = null;
            }
        }
    }

    // ---- Private helpers ------------------------------------

    private static Connection connect() {
        try {
            // Ensure directory exists
            Path dir = Paths.get(DB_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            String url = "jdbc:sqlite:" + DB_FILE;
            Connection conn = DriverManager.getConnection(url);

            // Enable WAL mode and FK enforcement on every new connection
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA journal_mode = WAL");
                st.execute("PRAGMA foreign_keys = ON");
            }

            initSchema(conn);
            System.out.println("[DB] Connected to " + DB_FILE);
            return conn;

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise database: " + e.getMessage(), e);
        }
    }

    /**
     * Reads schema.sql from the classpath and executes each statement.
     * Uses IF NOT EXISTS guards so re-runs are idempotent.
     */
    private static void initSchema(Connection conn) throws Exception {
        InputStream is = DatabaseManager.class.getResourceAsStream(SCHEMA_RESOURCE);
        if (is == null) {
            throw new IllegalStateException("schema.sql not found on classpath");
        }

        String sql;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            sql = reader.lines().collect(Collectors.joining("\n"));
        }

        // Split on semicolons (strip comments first)
        String[] statements = sql
                .replaceAll("--[^\n]*", "")   // remove line comments
                .split(";");

        try (Statement st = conn.createStatement()) {
            for (String stmt : statements) {
                String trimmed = stmt.strip();
                if (!trimmed.isEmpty()) {
                    st.execute(trimmed);
                }
            }
        }
        System.out.println("[DB] Schema initialised.");
    }
}
