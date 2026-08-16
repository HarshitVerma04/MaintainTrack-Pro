package com.maintaintrack.sync;

import com.maintaintrack.dao.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists SyncTasks to a local SYNC_QUEUE table.
 * Survives app crashes — queue drains on next startup.
 */
public class SyncQueueDAO {

    public static void createTableIfMissing() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS SYNC_QUEUE (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    table_name  TEXT    NOT NULL,
                    local_id    INTEGER NOT NULL,
                    payload     TEXT    NOT NULL,
                    operation   TEXT    NOT NULL,
                    timestamp   TEXT    NOT NULL,
                    retry_count INTEGER DEFAULT 0
                );
                """;
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void enqueue(SyncTask task) throws SQLException {
        String sql = """
                INSERT INTO SYNC_QUEUE (table_name, local_id, payload, operation, timestamp, retry_count)
                VALUES (?, ?, ?, ?, ?, 0);
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, task.getTableName());
            ps.setInt(2, task.getLocalId());
            ps.setString(3, task.getPayload());
            ps.setString(4, task.getOperation().name());
            ps.setString(5, task.getTimestamp());
            ps.executeUpdate();
        }
    }

    public List<SyncTask> getPending() throws SQLException {
        String sql = "SELECT * FROM SYNC_QUEUE ORDER BY id ASC;";
        List<SyncTask> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                SyncTask t = new SyncTask();
                t.setId(rs.getInt("id"));
                t.setTableName(rs.getString("table_name"));
                t.setLocalId(rs.getInt("local_id"));
                t.setPayload(rs.getString("payload"));
                t.setOperation(SyncTask.Operation.valueOf(rs.getString("operation")));
                t.setTimestamp(rs.getString("timestamp"));
                t.setRetryCount(rs.getInt("retry_count"));
                list.add(t);
            }
        }
        return list;
    }

    public void delete(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM SYNC_QUEUE WHERE id = ?;")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void incrementRetry(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE SYNC_QUEUE SET retry_count = retry_count + 1 WHERE id = ?;")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
