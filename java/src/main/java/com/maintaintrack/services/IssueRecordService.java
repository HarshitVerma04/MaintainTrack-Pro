package com.maintaintrack.services;

import com.maintaintrack.dao.IssueRecordDAO;
import com.maintaintrack.dao.PartDAO;
import com.maintaintrack.models.IssueRecord;
import com.maintaintrack.models.Part;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.maintaintrack.dao.DBConnection;

/**
 * IssueRecordService — business layer for part issue/return transactions.
 *
 * Day 8: supports optional breakdown_id linking (work order).
 *
 * Core rule: the ISSUE_RECORD insert and PART.qty_on_hand update
 * always happen together in a single transaction. If either fails,
 * both are rolled back — inventory is never silently wrong.
 *
 * Validation:
 *   - Part and equipment are required
 *   - Qty must be positive
 *   - For an issue: qty must not exceed current stock
 *   - breakdown_id is optional — null means standalone stock draw
 */
public class IssueRecordService {

    private final IssueRecordDAO dao     = new IssueRecordDAO();
    private final PartDAO        partDAO = new PartDAO();

    /**
     * Records a part transaction (issue or return) atomically.
     * breakdown_id on the record is preserved if set (work order link).
     */
    public void recordTransaction(IssueRecord record) throws SQLException {
        validate(record);

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                dao.insert(record, conn);
                dao.adjustPartQty(
                        record.getPartId(),
                        record.getQty(),
                        record.getType(),
                        conn
                );
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<IssueRecord> getAllRecords() throws SQLException {
        return dao.findAll();
    }

    public List<IssueRecord> getRecordsForEquipment(int equipmentId) throws SQLException {
        return dao.findByEquipment(equipmentId);
    }

    /**
     * Day 8 — Work order: all parts issued for a specific breakdown.
     */
    public List<IssueRecord> getWorkOrderParts(int breakdownId) throws SQLException {
        return dao.findByBreakdown(breakdownId);
    }

    /**
     * Day 8 — Work order cost: total parts spend for one breakdown.
     */
    public double getWorkOrderCost(int breakdownId) throws SQLException {
        return dao.getWorkOrderCost(breakdownId);
    }

    // ── Validation ────────────────────────────────────────────────────────

    private void validate(IssueRecord r) throws SQLException {
        if (r.getPartId() <= 0)
            throw new IllegalArgumentException("Please select a part.");
        if (r.getEquipmentId() <= 0)
            throw new IllegalArgumentException("Please select an equipment.");
        if (r.getIssuedOn() == null)
            throw new IllegalArgumentException("Issue date is required.");
        if (r.getQty() <= 0)
            throw new IllegalArgumentException("Quantity must be a positive number.");

        // For issues: check we have enough stock
        if ("issue".equals(r.getType())) {
            Part part = partDAO.findById(r.getPartId());
            if (part != null && r.getQty() > part.getQtyOnHand()) {
                throw new IllegalArgumentException(
                        "Insufficient stock. Available: "
                        + part.getQtyOnHand() + " " + part.getUnit()
                        + ", requested: " + r.getQty() + "."
                );
            }
        }
    }
}
