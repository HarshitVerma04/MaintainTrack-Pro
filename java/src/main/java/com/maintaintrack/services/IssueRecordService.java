package com.maintaintrack.services;

import com.maintaintrack.dao.DBConnection;
import com.maintaintrack.dao.IssueRecordDAO;
import com.maintaintrack.dao.PartDAO;
import com.maintaintrack.models.IssueRecord;
import com.maintaintrack.models.Part;
import com.maintaintrack.sync.SyncService;
import com.maintaintrack.sync.SyncTask;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

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

        // Push to cloud after successful local transaction
        String endpoint = "issue".equals(record.getType())
                ? "ISSUE_RECORD" : "ISSUE_RECORD_RETURN";
        SyncService.getInstance().push(new SyncTask(
                endpoint, record.getId(),
                toJson(record), SyncTask.Operation.INSERT));
    }

    public List<IssueRecord> getAllRecords() throws SQLException {
        return dao.findAll();
    }

    public List<IssueRecord> getRecordsForEquipment(int id) throws SQLException {
        return dao.findByEquipment(id);
    }

    public List<IssueRecord> getBreakdownParts(int breakdownId) throws SQLException {
        return dao.findByBreakdown(breakdownId);
    }

    public double getBreakdownWorkOrderCost(int breakdownId) throws SQLException {
        return dao.getWorkOrderCost(breakdownId);
    }

    public List<IssueRecord> getMaintenanceParts(int maintenanceId) throws SQLException {
        return dao.findByMaintenance(maintenanceId);
    }

    public double getMaintenanceWorkOrderCost(int maintenanceId) throws SQLException {
        return dao.getMaintenanceCost(maintenanceId);
    }

    private String toJson(IssueRecord r) {
        return String.format(
                "{\"partId\":%d,\"equipmentId\":%d,\"qty\":%d," +
                        "\"issuedBy\":\"%s\",\"issuedOn\":\"%s\",\"type\":\"%s\"," +
                        "\"breakdownId\":%s,\"maintenanceId\":%s}",
                r.getPartId(),
                r.getEquipmentId(),
                r.getQty(),
                escape(r.getIssuedBy()),
                r.getIssuedOn() != null ? r.getIssuedOn().toString() : "",
                escape(r.getType()),
                r.getBreakdownId() != null ? r.getBreakdownId() : "null",
                r.getMaintenanceId() != null ? r.getMaintenanceId() : "null"
        );
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
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