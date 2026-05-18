package com.maintaintrack.services;

import com.maintaintrack.dao.DBConnection;
import com.maintaintrack.dao.IssueRecordDAO;
import com.maintaintrack.dao.PartDAO;
import com.maintaintrack.models.IssueRecord;
import com.maintaintrack.models.Part;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * IssueRecordService — business layer for part transactions.
 *
 * Day 8: breakdown_id work order link
 * Day 9: maintenance_id work order link + WorkOrderService delegation
 *
 * Core rule: ISSUE_RECORD insert and PART.qty_on_hand update
 * always happen atomically. If either fails, both roll back.
 */
public class IssueRecordService {

    private final IssueRecordDAO dao     = new IssueRecordDAO();
    private final PartDAO        partDAO = new PartDAO();

    public void recordTransaction(IssueRecord record) throws SQLException {
        validate(record);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                dao.insert(record, conn);
                dao.adjustPartQty(record.getPartId(), record.getQty(),
                                  record.getType(), conn);
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

    public List<IssueRecord> getRecordsForEquipment(int id) throws SQLException {
        return dao.findByEquipment(id);
    }

    // Day 8
    public List<IssueRecord> getBreakdownParts(int breakdownId) throws SQLException {
        return dao.findByBreakdown(breakdownId);
    }

    public double getBreakdownWorkOrderCost(int breakdownId) throws SQLException {
        return dao.getWorkOrderCost(breakdownId);
    }

    // Day 9
    public List<IssueRecord> getMaintenanceParts(int maintenanceId) throws SQLException {
        return dao.findByMaintenance(maintenanceId);
    }

    public double getMaintenanceWorkOrderCost(int maintenanceId) throws SQLException {
        return dao.getMaintenanceCost(maintenanceId);
    }

    private void validate(IssueRecord r) throws SQLException {
        if (r.getPartId() <= 0)
            throw new IllegalArgumentException("Please select a part.");
        if (r.getEquipmentId() <= 0)
            throw new IllegalArgumentException("Please select an equipment.");
        if (r.getIssuedOn() == null)
            throw new IllegalArgumentException("Issue date is required.");
        if (r.getQty() <= 0)
            throw new IllegalArgumentException("Quantity must be a positive number.");
        if ("issue".equals(r.getType())) {
            Part part = partDAO.findById(r.getPartId());
            if (part != null && r.getQty() > part.getQtyOnHand()) {
                throw new IllegalArgumentException(
                    "Insufficient stock. Available: " + part.getQtyOnHand()
                    + " " + part.getUnit() + ", requested: " + r.getQty() + ".");
            }
        }
    }
}
