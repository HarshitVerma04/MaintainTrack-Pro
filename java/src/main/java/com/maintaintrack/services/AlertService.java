package com.maintaintrack.services;

import com.maintaintrack.dao.EquipmentDAO;
import com.maintaintrack.dao.PartDAO;
import com.maintaintrack.models.Alert;
import com.maintaintrack.models.Alert.Severity;
import com.maintaintrack.models.Alert.Type;
import com.maintaintrack.models.Equipment;
import com.maintaintrack.models.Part;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * AlertService — detects low-stock parts and overdue maintenance.
 *
 * Day 13: Low-stock detect — qty_on_hand vs min_qty
 * Day 14: Overdue detect   — next_maint_date vs today
 * Day 19: Self-clears on fix — always re-queries live DB, so when
 *         stock is replenished or maintenance is logged, the alert
 *         disappears automatically on the next poll cycle.
 *
 * This service is STATELESS — every call to getActiveAlerts() runs
 * fresh queries. The ScheduledExecutorService in AlertPollingService
 * calls this on a timer and pushes results to the UI.
 *
 * Severity rules:
 *   Overdue maintenance:
 *     ≥ 7 days overdue → HIGH
 *     1–6 days overdue → MEDIUM
 *   Low stock:
 *     qty == 0        → HIGH  (completely out)
 *     qty <= min_qty  → MEDIUM
 */
public class AlertService {

    private final EquipmentDAO equipmentDAO = new EquipmentDAO();
    private final PartDAO      partDAO      = new PartDAO();

    /**
     * Returns the current list of active alerts.
     * Called by the background polling thread every N seconds.
     * Because it re-queries every time, alerts self-clear when the
     * underlying condition is fixed (Day 19).
     */
    public List<Alert> getActiveAlerts() throws SQLException {
        List<Alert> alerts = new ArrayList<>();
        alerts.addAll(buildOverdueAlerts());
        alerts.addAll(buildLowStockAlerts());
        return alerts;
    }

    // ── Day 14: Overdue maintenance alerts ───────────────────────────────

    private List<Alert> buildOverdueAlerts() throws SQLException {
        List<Alert> alerts = new ArrayList<>();
        LocalDate today    = LocalDate.now();

        for (Equipment eq : equipmentDAO.findAll()) {
            LocalDate nextDue = eq.getNextMaintenanceDate();
            if (nextDue == null || !nextDue.isBefore(today)) continue;

            long daysOverdue = today.toEpochDay() - nextDue.toEpochDay();
            Severity sev = daysOverdue >= 7 ? Severity.HIGH : Severity.MEDIUM;

            alerts.add(new Alert(
                    Type.OVERDUE_MAINTENANCE,
                    sev,
                    "Maintenance Overdue: " + eq.getName(),
                    daysOverdue + " day" + (daysOverdue == 1 ? "" : "s") + " overdue"
                    + "  ·  Location: " + (eq.getLocation() != null ? eq.getLocation() : "—"),
                    eq.getId()
            ));
        }
        return alerts;
    }

    // ── Day 13: Low-stock alerts ──────────────────────────────────────────

    private List<Alert> buildLowStockAlerts() throws SQLException {
        List<Alert> alerts = new ArrayList<>();

        for (Part part : partDAO.findLowStock()) {
            Severity sev = part.getQtyOnHand() == 0 ? Severity.HIGH : Severity.MEDIUM;

            String detail = part.getQtyOnHand() == 0
                    ? "Out of stock! Min required: " + part.getMinQty() + " " + part.getUnit()
                    : "Stock: " + part.getQtyOnHand() + " / min: " + part.getMinQty()
                      + " " + part.getUnit();

            if (part.getSupplierName() != null) {
                detail += "  ·  Supplier: " + part.getSupplierName();
            }

            alerts.add(new Alert(
                    Type.LOW_STOCK,
                    sev,
                    "Low Stock: " + part.getName(),
                    detail,
                    part.getId()
            ));
        }
        return alerts;
    }

    // ── Convenience counts (used by sidebar badges) ───────────────────────

    public int countOverdueEquipment() throws SQLException {
        LocalDate today = LocalDate.now();
        return (int) equipmentDAO.findAll().stream()
                .filter(e -> e.getNextMaintenanceDate() != null
                          && e.getNextMaintenanceDate().isBefore(today))
                .count();
    }

    public int countLowStockParts() throws SQLException {
        return partDAO.findLowStock().size();
    }
}
