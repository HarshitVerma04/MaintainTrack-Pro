package com.maintaintrack.controllers;

import com.maintaintrack.dao.DBConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ActivityController — drives the full Activity Feed screen.
 *
 * Merges MAINTENANCE_LOG + BREAKDOWN_LOG + ISSUE_RECORD into one
 * chronologically sorted feed using SQL UNION ALL.
 *
 * Filters:
 *   - Type       (All / Maintenance / Breakdown / Part Issued / Part Returned)
 *   - Date range (From → To)
 *   - Search     (equipment name or detail text)
 *
 * Each row is String[5]: {type, date, detail, subject, actor}
 */
public class ActivityController {

    // ── Table ─────────────────────────────────────────────────────────────
    @FXML private TableView<String[]>          activityTable;
    @FXML private TableColumn<String[],String> colType;
    @FXML private TableColumn<String[],String> colDate;
    @FXML private TableColumn<String[],String> colSubject;
    @FXML private TableColumn<String[],String> colDetail;
    @FXML private TableColumn<String[],String> colActor;

    // ── Filters ───────────────────────────────────────────────────────────
    @FXML private ComboBox<String> filterType;
    @FXML private DatePicker       filterFrom;
    @FXML private DatePicker       filterTo;
    @FXML private TextField        filterSearch;

    // ── Header ────────────────────────────────────────────────────────────
    @FXML private Label totalLabel;

    private final ObservableList<String[]> tableData = FXCollections.observableArrayList();

    // ── Init ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        loadActivity(null, null, null, "All");
    }

    private void setupTable() {
        colType.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[0]));
        colDate.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[1]));
        colSubject.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[3]));
        colDetail.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[2]));
        colActor.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[4]));

        // Type column — colour coded
        colType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) { setText(null); setStyle(""); return; }
                setText(type);
                setStyle(switch (type) {
                    case "Maintenance"   -> "-fx-text-fill:#1a7a4a; -fx-font-weight:bold;";
                    case "Breakdown"     -> "-fx-text-fill:#8b1a1a; -fx-font-weight:bold;";
                    case "Part Issued"   -> "-fx-text-fill:#b85c00; -fx-font-weight:bold;";
                    case "Part Returned" -> "-fx-text-fill:#2e86de; -fx-font-weight:bold;";
                    default              -> "";
                });
            }
        });

        activityTable.setItems(tableData);
    }

    private void setupFilters() {
        filterType.setItems(FXCollections.observableArrayList(
                "All", "Maintenance", "Breakdown", "Part Issued", "Part Returned"
        ));
        filterType.setValue("All");

        // Default date range: last 90 days → today
        filterFrom.setValue(LocalDate.now().minusDays(90));
        filterTo.setValue(LocalDate.now());
    }

    // ── Filter actions ────────────────────────────────────────────────────

    @FXML
    private void onApply() {
        loadActivity(
                filterFrom.getValue(),
                filterTo.getValue(),
                filterSearch.getText().trim().isEmpty() ? null : filterSearch.getText().trim(),
                filterType.getValue()
        );
    }

    @FXML
    private void onReset() {
        filterType.setValue("All");
        filterFrom.setValue(LocalDate.now().minusDays(90));
        filterTo.setValue(LocalDate.now());
        filterSearch.clear();
        loadActivity(null, null, null, "All");
    }

    // ── Data load ─────────────────────────────────────────────────────────

    /**
     * Builds and runs the UNION ALL query with optional filters applied.
     * All three log tables are merged and sorted by date DESC.
     */
    private void loadActivity(LocalDate from, LocalDate to,
                              String search, String typeFilter) {
        /*
         * Base UNION — same structure as DashboardService.getRecentActivity()
         * but without the LIMIT and with date/search WHERE clauses added.
         *
         * We wrap the UNION in a subquery so ORDER BY and WHERE apply to
         * the merged result, not individual tables.
         */
        String inner = """
                SELECT 'Maintenance' AS type,
                       m.done_on     AS event_date,
                       m.notes       AS detail,
                       e.name        AS subject,
                       m.done_by     AS actor
                FROM MAINTENANCE_LOG m
                JOIN EQUIPMENT e ON m.equipment_id = e.id

                UNION ALL

                SELECT 'Breakdown',
                       b.occurred_on,
                       b.description,
                       e.name,
                       COALESCE(b.resolved_by, 'Unresolved')
                FROM BREAKDOWN_LOG b
                JOIN EQUIPMENT e ON b.equipment_id = e.id

                UNION ALL

                SELECT CASE ir.type
                           WHEN 'issue' THEN 'Part Issued'
                           ELSE 'Part Returned'
                       END,
                       ir.issued_on,
                       p.name || ' × ' || ir.qty,
                       e.name,
                       COALESCE(ir.issued_by, '—')
                FROM ISSUE_RECORD ir
                JOIN PART p      ON ir.part_id      = p.id
                JOIN EQUIPMENT e ON ir.equipment_id = e.id
                """;

        // Build WHERE conditions dynamically
        List<String> conditions = new ArrayList<>();
        List<Object> params     = new ArrayList<>();

        if (from != null) {
            conditions.add("event_date >= ?");
            params.add(from.toString());
        }
        if (to != null) {
            conditions.add("event_date <= ?");
            params.add(to.toString());
        }
        if (search != null && !search.isBlank()) {
            conditions.add("(subject LIKE ? OR detail LIKE ?)");
            params.add("%" + search + "%");
            params.add("%" + search + "%");
        }
        if (typeFilter != null && !typeFilter.equals("All")) {
            conditions.add("type = ?");
            params.add(typeFilter);
        }

        String where = conditions.isEmpty()
                ? ""
                : " WHERE " + String.join(" AND ", conditions);

        String sql = "SELECT * FROM (" + inner + ") ORDER BY event_date DESC" + where.replace("WHERE", "");

        // Rebuild properly — SQLite doesn't support WHERE after ORDER BY on UNION
        // So wrap the whole thing as a subquery:
        sql = "SELECT * FROM (\n" + inner + "\n) AS feed"
                + (conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions))
                + " ORDER BY event_date DESC;";

        List<String[]> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new String[]{
                        rs.getString("type"),
                        rs.getString("event_date"),
                        rs.getString("detail"),
                        rs.getString("subject"),
                        rs.getString("actor")
                });
            }

            tableData.setAll(rows);
            totalLabel.setText(rows.size() + " events");

        } catch (SQLException e) {
            System.err.println("[Activity] Query failed: " + e.getMessage());
            totalLabel.setText("Error loading activity");
        }
    }
}
