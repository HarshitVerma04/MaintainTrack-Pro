package com.maintaintrack.sync;

import com.maintaintrack.auth.AuthContext;
import com.maintaintrack.dao.DBConnection;
import com.maintaintrack.dao.SettingsDAO;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Pulls changes from the cloud API into local SQLite.
 * Called on login and every 5 minutes by SyncService.
 *
 * Uses sync_id as the universal key — upserts records
 * using last-write-wins on updated_at.
 */
public class SyncPullService {

    private static final String BASE_URL =
            "https://maintaintrack-pro.onrender.com";

    private static final String LAST_SYNC_KEY = "last_sync_time";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(35))
            .build();

    /**
     * Main entry point — call this on login.
     * Returns a summary string for logging.
     */
    public static String pull() {
        if (!AuthContext.getInstance().isLoggedIn()) return "Not logged in";
        if (!NetworkUtil.isOnline()) return "Offline — skipping pull";

        String since = SettingsDAO.get(LAST_SYNC_KEY, "1970-01-01T00:00:00");
        System.out.println("[Pull] Pulling changes since: " + since);

        try {
            String token = AuthContext.getInstance().getToken();
            String url   = BASE_URL + "/api/sync/changes?since="
                    + since.replace(" ", "T");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .timeout(Duration.ofSeconds(35))
                    .build();

            HttpResponse<String> response = CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return "Pull failed — HTTP " + response.statusCode();
            }

            JSONObject body = new JSONObject(response.body());
            int total = 0;

            total += upsertEquipment(body.optJSONArray("equipment"));
            total += upsertSuppliers(body.optJSONArray("suppliers"));
            total += upsertParts(body.optJSONArray("parts"));
            total += upsertMaintenanceLogs(body.optJSONArray("maintenanceLogs"));
            total += upsertBreakdownLogs(body.optJSONArray("breakdownLogs"));

            // Save successful sync time
            SettingsDAO.set(LAST_SYNC_KEY, LocalDateTime.now().toString());
            System.out.println("[Pull] Complete — " + total + " records upserted.");
            return "Pulled " + total + " records from cloud";

        } catch (Exception e) {
            System.err.println("[Pull] Error: " + e.getMessage());
            return "Pull error: " + e.getMessage();
        }
    }

    // ── Equipment ─────────────────────────────────────────────────────────────
    private static int upsertEquipment(JSONArray arr) throws SQLException {
        if (arr == null) return 0;
        int count = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject r = arr.getJSONObject(i);
            String syncId = r.optString("syncId", null);
            if (syncId == null) continue;

            try (Connection conn = DBConnection.getConnection()) {
                // Check if exists
                PreparedStatement check = conn.prepareStatement(
                        "SELECT id, updated_at FROM EQUIPMENT WHERE sync_id = ?");
                check.setString(1, syncId);
                ResultSet rs = check.executeQuery();

                if (rs.next()) {
                    // Exists — check if cloud is newer
                    String localUpdatedAt = rs.getString("updated_at");
                    String cloudUpdatedAt = r.optString("updatedAt", "");
                    if (isNewer(cloudUpdatedAt, localUpdatedAt)) {
                        PreparedStatement upd = conn.prepareStatement("""
                            UPDATE EQUIPMENT SET
                                name = ?, location = ?, status = ?,
                                next_maintenance_date = ?, interval_days = ?,
                                updated_at = ?, synced = 1
                            WHERE sync_id = ?
                        """);
                        upd.setString(1, r.optString("name"));
                        upd.setString(2, r.optString("location"));
                        upd.setString(3, r.optString("status", "Operational"));
                        upd.setString(4, r.optString("nextMaintenanceDate"));
                        upd.setInt(5, r.optInt("intervalDays", 30));
                        upd.setString(6, cloudUpdatedAt);
                        upd.setString(7, syncId);
                        upd.executeUpdate();
                        count++;
                    }
                } else {
                    // New — insert
                    PreparedStatement ins = conn.prepareStatement("""
                        INSERT INTO EQUIPMENT
                            (sync_id, name, location, status,
                             next_maintenance_date, interval_days,
                             updated_at, synced)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 1)
                    """);
                    ins.setString(1, syncId);
                    ins.setString(2, r.optString("name"));
                    ins.setString(3, r.optString("location"));
                    ins.setString(4, r.optString("status", "Operational"));
                    ins.setString(5, r.optString("nextMaintenanceDate"));
                    ins.setInt(6, r.optInt("intervalDays", 30));
                    ins.setString(7, r.optString("updatedAt"));
                    ins.executeUpdate();
                    count++;
                }
            }
        }
        return count;
    }

    private static int upsertSuppliers(JSONArray arr) throws SQLException {
        if (arr == null) return 0;
        int count = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject r = arr.getJSONObject(i);
            String syncId = r.optString("syncId", null);
            if (syncId == null) continue;

            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement check = conn.prepareStatement(
                        "SELECT id, updated_at FROM SUPPLIER WHERE sync_id = ?");
                check.setString(1, syncId);
                ResultSet rs = check.executeQuery();

                if (rs.next()) {
                    String localUpdatedAt = rs.getString("updated_at");
                    String cloudUpdatedAt = r.optString("updatedAt", "");
                    if (isNewer(cloudUpdatedAt, localUpdatedAt)) {
                        PreparedStatement upd = conn.prepareStatement("""
                        UPDATE SUPPLIER SET
                            name = ?, contact_name = ?, phone = ?,
                            email = ?, updated_at = ?, synced = 1
                        WHERE sync_id = ?
                    """);
                        upd.setString(1, r.optString("name"));
                        upd.setString(2, r.optString("contactName"));
                        upd.setString(3, r.optString("phone"));
                        upd.setString(4, r.optString("email"));
                        upd.setString(5, cloudUpdatedAt);
                        upd.setString(6, syncId);
                        upd.executeUpdate();
                        count++;
                    }
                } else {
                    PreparedStatement ins = conn.prepareStatement("""
                    INSERT INTO SUPPLIER
                        (sync_id, name, contact_name, phone, email, updated_at, synced)
                    VALUES (?, ?, ?, ?, ?, ?, 1)
                """);
                    ins.setString(1, syncId);
                    ins.setString(2, r.optString("name"));
                    ins.setString(3, r.optString("contactName"));
                    ins.setString(4, r.optString("phone"));
                    ins.setString(5, r.optString("email"));
                    ins.setString(6, r.optString("updatedAt"));
                    ins.executeUpdate();
                    count++;
                }
            }
        }
        return count;
    }

    // ── Parts ─────────────────────────────────────────────────────────────────
    private static int upsertParts(JSONArray arr) throws SQLException {
        if (arr == null) return 0;
        int count = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject r = arr.getJSONObject(i);
            String syncId = r.optString("syncId", null);
            if (syncId == null) continue;

            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement check = conn.prepareStatement(
                        "SELECT id, updated_at FROM PART WHERE sync_id = ?");
                check.setString(1, syncId);
                ResultSet rs = check.executeQuery();

                if (rs.next()) {
                    String localUpdatedAt = rs.getString("updated_at");
                    String cloudUpdatedAt = r.optString("updatedAt", "");
                    if (isNewer(cloudUpdatedAt, localUpdatedAt)) {
                        PreparedStatement upd = conn.prepareStatement("""
                            UPDATE PART SET
                                name = ?, qty_on_hand = ?, min_qty = ?,
                                unit = ?, unit_cost = ?,
                                updated_at = ?, synced = 1
                            WHERE sync_id = ?
                        """);
                        upd.setString(1, r.optString("name"));
                        upd.setInt(2, r.optInt("qtyOnHand", 0));
                        upd.setInt(3, r.optInt("minQty", 5));
                        upd.setString(4, r.optString("unit", "pcs"));
                        upd.setDouble(5, r.optDouble("unitCost", 0.0));
                        upd.setString(6, cloudUpdatedAt);
                        upd.setString(7, syncId);
                        upd.executeUpdate();
                        count++;
                    }
                } else {
                    PreparedStatement ins = conn.prepareStatement("""
                        INSERT INTO PART
                            (sync_id, name, qty_on_hand, min_qty,
                             unit, unit_cost, updated_at, synced)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 1)
                    """);
                    ins.setString(1, syncId);
                    ins.setString(2, r.optString("name"));
                    ins.setInt(3, r.optInt("qtyOnHand", 0));
                    ins.setInt(4, r.optInt("minQty", 5));
                    ins.setString(5, r.optString("unit", "pcs"));
                    ins.setDouble(6, r.optDouble("unitCost", 0.0));
                    ins.setString(7, r.optString("updatedAt"));
                    ins.executeUpdate();
                    count++;
                }
            }
        }
        return count;
    }

    // ── Maintenance Logs ──────────────────────────────────────────────────────
    private static int upsertMaintenanceLogs(JSONArray arr) throws SQLException {
        if (arr == null) return 0;
        int count = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject r = arr.getJSONObject(i);
            String syncId = r.optString("syncId", null);
            if (syncId == null) continue;

            // Resolve local equipment_id from equipment's sync_id
            String equipSyncId = null;
            if (r.has("equipment") && !r.isNull("equipment")) {
                equipSyncId = r.getJSONObject("equipment").optString("syncId", null);
            }
            if (equipSyncId == null) continue;

            Long localEquipId = resolveLocalId("EQUIPMENT", equipSyncId);
            if (localEquipId == null) continue;

            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement check = conn.prepareStatement(
                        "SELECT id, updated_at FROM MAINTENANCE_LOG WHERE sync_id = ?");
                check.setString(1, syncId);
                ResultSet rs = check.executeQuery();

                if (rs.next()) {
                    String localUpdatedAt = rs.getString("updated_at");
                    String cloudUpdatedAt = r.optString("updatedAt", "");
                    if (isNewer(cloudUpdatedAt, localUpdatedAt)) {
                        PreparedStatement upd = conn.prepareStatement("""
                            UPDATE MAINTENANCE_LOG SET
                                done_on = ?, notes = ?, done_by = ?,
                                updated_at = ?, synced = 1
                            WHERE sync_id = ?
                        """);
                        upd.setString(1, r.optString("doneOn"));
                        upd.setString(2, r.optString("notes"));
                        upd.setString(3, r.optString("doneBy"));
                        upd.setString(4, cloudUpdatedAt);
                        upd.setString(5, syncId);
                        upd.executeUpdate();
                        count++;
                    }
                } else {
                    PreparedStatement ins = conn.prepareStatement("""
                        INSERT INTO MAINTENANCE_LOG
                            (sync_id, equipment_id, done_on, notes,
                             done_by, updated_at, synced)
                        VALUES (?, ?, ?, ?, ?, ?, 1)
                    """);
                    ins.setString(1, syncId);
                    ins.setLong(2, localEquipId);
                    ins.setString(3, r.optString("doneOn"));
                    ins.setString(4, r.optString("notes"));
                    ins.setString(5, r.optString("doneBy"));
                    ins.setString(6, r.optString("updatedAt"));
                    ins.executeUpdate();
                    count++;
                }
            }
        }
        return count;
    }

    // ── Breakdown Logs ────────────────────────────────────────────────────────
    private static int upsertBreakdownLogs(JSONArray arr) throws SQLException {
        if (arr == null) return 0;
        int count = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject r = arr.getJSONObject(i);
            String syncId = r.optString("syncId", null);
            if (syncId == null) continue;

            String equipSyncId = null;
            if (r.has("equipment") && !r.isNull("equipment")) {
                equipSyncId = r.getJSONObject("equipment").optString("syncId", null);
            }
            if (equipSyncId == null) continue;

            Long localEquipId = resolveLocalId("EQUIPMENT", equipSyncId);
            if (localEquipId == null) continue;

            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement check = conn.prepareStatement(
                        "SELECT id, updated_at FROM BREAKDOWN_LOG WHERE sync_id = ?");
                check.setString(1, syncId);
                ResultSet rs = check.executeQuery();

                if (rs.next()) {
                    String localUpdatedAt = rs.getString("updated_at");
                    String cloudUpdatedAt = r.optString("updatedAt", "");
                    if (isNewer(cloudUpdatedAt, localUpdatedAt)) {
                        PreparedStatement upd = conn.prepareStatement("""
                            UPDATE BREAKDOWN_LOG SET
                                occurred_on = ?, description = ?,
                                resolved_by = ?, updated_at = ?, synced = 1
                            WHERE sync_id = ?
                        """);
                        upd.setString(1, r.optString("occurredOn"));
                        upd.setString(2, r.optString("description"));
                        upd.setString(3, r.optString("resolvedBy"));
                        upd.setString(4, r.optString("updatedAt"));
                        upd.setString(5, syncId);
                        upd.executeUpdate();
                        count++;
                    }
                } else {
                    PreparedStatement ins = conn.prepareStatement("""
                        INSERT INTO BREAKDOWN_LOG
                            (sync_id, equipment_id, occurred_on,
                             description, resolved_by, updated_at, synced)
                        VALUES (?, ?, ?, ?, ?, ?, 1)
                    """);
                    ins.setString(1, syncId);
                    ins.setLong(2, localEquipId);
                    ins.setString(3, r.optString("occurredOn"));
                    ins.setString(4, r.optString("description"));
                    ins.setString(5, r.optString("resolvedBy"));
                    ins.setString(6, r.optString("updatedAt"));
                    ins.executeUpdate();
                    count++;
                }
            }
        }
        return count;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns the local SQLite id for a record identified by its sync_id. */
    private static Long resolveLocalId(String table, String syncId) {
        String sql = "SELECT id FROM " + table + " WHERE sync_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, syncId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("id");
        } catch (Exception e) {
            System.err.println("[Pull] resolveLocalId failed: " + e.getMessage());
        }
        return null;
    }

    /** Returns true if cloudTime is strictly newer than localTime. */
    private static boolean isNewer(String cloudTime, String localTime) {
        if (cloudTime == null || cloudTime.isEmpty()) return false;
        if (localTime == null || localTime.isEmpty()) return true;
        try {
            return LocalDateTime.parse(cloudTime.replace(" ", "T"))
                    .isAfter(LocalDateTime.parse(localTime.replace(" ", "T")));
        } catch (Exception e) {
            return false;
        }
    }
}
