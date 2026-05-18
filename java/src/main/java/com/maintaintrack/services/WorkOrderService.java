package com.maintaintrack.services;

import com.maintaintrack.dao.BreakdownLogDAO;
import com.maintaintrack.dao.IssueRecordDAO;
import com.maintaintrack.dao.MaintenanceLogDAO;
import com.maintaintrack.models.BreakdownLog;
import com.maintaintrack.models.IssueRecord;
import com.maintaintrack.models.MaintenanceLog;

import java.sql.SQLException;
import java.util.List;

/**
 * WorkOrderService — Day 9 full traceability aggregator.
 *
 * Answers the key operational questions:
 *
 *   1. What parts were used to fix breakdown X?
 *      What did it cost?
 *
 *   2. What parts were consumed in maintenance job Y?
 *      What did it cost?
 *
 *   3. For equipment Z, give me the full history:
 *      every PM job, every breakdown, and parts used for each.
 *
 * This service is read-only — it only aggregates existing data.
 * All writes still go through IssueRecordService.
 */
public class WorkOrderService {

    private final IssueRecordDAO    issueDAO       = new IssueRecordDAO();
    private final MaintenanceLogDAO maintenanceDAO = new MaintenanceLogDAO();
    private final BreakdownLogDAO   breakdownDAO   = new BreakdownLogDAO();

    // ── Breakdown work order ──────────────────────────────────────────────

    /**
     * All parts issued for a specific breakdown + total cost.
     */
    public List<IssueRecord> getBreakdownParts(int breakdownId) throws SQLException {
        return issueDAO.findByBreakdown(breakdownId);
    }

    public double getBreakdownCost(int breakdownId) throws SQLException {
        return issueDAO.getWorkOrderCost(breakdownId);
    }

    // ── Maintenance work order ────────────────────────────────────────────

    /**
     * All parts issued for a specific maintenance job + total cost.
     */
    public List<IssueRecord> getMaintenanceParts(int maintenanceId) throws SQLException {
        return issueDAO.findByMaintenance(maintenanceId);
    }

    public double getMaintenanceCost(int maintenanceId) throws SQLException {
        return issueDAO.getMaintenanceCost(maintenanceId);
    }

    // ── Full equipment traceability (Day 9 centrepiece) ───────────────────

    /**
     * EquipmentHistory — complete job and cost history for one equipment.
     *
     * Usage in controller:
     *   WorkOrderService.EquipmentHistory h = ws.getEquipmentHistory(equipmentId);
     *   h.maintenanceLogs  → all PM jobs
     *   h.maintenanceCosts → cost per PM job (index-matched)
     *   h.breakdownLogs    → all breakdown incidents
     *   h.breakdownCosts   → cost per breakdown (index-matched)
     *   h.totalCost        → grand total across all work orders
     */
    public static class EquipmentHistory {
        public final List<MaintenanceLog> maintenanceLogs;
        public final List<Double>         maintenanceCosts;
        public final List<BreakdownLog>   breakdownLogs;
        public final List<Double>         breakdownCosts;
        public final double               totalCost;

        public EquipmentHistory(List<MaintenanceLog> ml, List<Double> mc,
                                List<BreakdownLog> bl,   List<Double> bc) {
            this.maintenanceLogs  = ml;
            this.maintenanceCosts = mc;
            this.breakdownLogs    = bl;
            this.breakdownCosts   = bc;
            double sum = mc.stream().mapToDouble(Double::doubleValue).sum()
                       + bc.stream().mapToDouble(Double::doubleValue).sum();
            this.totalCost = Math.round(sum * 100.0) / 100.0;
        }
    }

    public EquipmentHistory getEquipmentHistory(int equipmentId) throws SQLException {
        // Maintenance jobs + their costs
        List<MaintenanceLog> ml = maintenanceDAO.findByEquipment(equipmentId);
        List<Double> mc = new java.util.ArrayList<>();
        for (MaintenanceLog m : ml) {
            mc.add(issueDAO.getMaintenanceCost(m.getId()));
        }

        // Breakdowns + their costs
        List<BreakdownLog> bl = breakdownDAO.findByEquipment(equipmentId);
        List<Double> bc = new java.util.ArrayList<>();
        for (BreakdownLog b : bl) {
            bc.add(issueDAO.getWorkOrderCost(b.getId()));
        }

        return new EquipmentHistory(ml, mc, bl, bc);
    }

    // ── Summary: all maintenance costs ────────────────────────────────────

    /**
     * Total parts spend across all maintenance jobs for one equipment.
     */
    public double getTotalMaintenanceCost(int equipmentId) throws SQLException {
        List<MaintenanceLog> logs = maintenanceDAO.findByEquipment(equipmentId);
        double total = 0;
        for (MaintenanceLog m : logs) {
            total += issueDAO.getMaintenanceCost(m.getId());
        }
        return Math.round(total * 100.0) / 100.0;
    }

    /**
     * Total parts spend across all breakdown repairs for one equipment.
     */
    public double getTotalBreakdownRepairCost(int equipmentId) throws SQLException {
        List<BreakdownLog> logs = breakdownDAO.findByEquipment(equipmentId);
        double total = 0;
        for (BreakdownLog b : logs) {
            total += issueDAO.getWorkOrderCost(b.getId());
        }
        return Math.round(total * 100.0) / 100.0;
    }
}
