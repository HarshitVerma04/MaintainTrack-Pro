package com.maintaintrack.services;

import com.maintaintrack.dao.DBConnection;
import com.maintaintrack.dao.IssueRecordDAO;
import com.maintaintrack.dao.PartDAO;
import com.maintaintrack.models.IssueRecord;
import com.maintaintrack.models.Part;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * IssueRecordService — business logic for issuing and returning parts.
 *
 * CORE RULE (Days 10–12):
 *   When a part is issued:  qty_on_hand -= qty   (atomic, same transaction)
 *   When a part is returned: qty_on_hand += qty  (atomic, same transaction)
 *
 * Both the ISSUE_RECORD insert AND the PART qty update happen inside a
 * single Connection with autoCommit=false. If either fails, both are
 * rolled back — no ghost records, no phantom stock changes.
 */
public class IssueRecordService {

    private final IssueRecordDAO issueDAO = new IssueRecordDAO();
    private final PartDAO        partDAO  = new PartDAO();

    /**
     * Records a part issue or return AND updates stock atomically.
     * This is the central transactional write for Phase 2 / Days 10–12.
     */
    public void recordTransaction(IssueRecord record) throws SQLException {
        // ── Validate ──────────────────────────────────────────────────────
        if (record.getPartId() <= 0)
            throw new IllegalArgumentException("Please select a part.");
        if (record.getEquipmentId() <= 0)
            throw new IllegalArgumentException("Please select an equipment.");
        if (record.getIssuedOn() == null)
            throw new IllegalArgumentException("Date is required.");
        if (record.getIssuedOn().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Date cannot be in the future.");
        if (record.getQty() <= 0)
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        if (record.getType() == null || (!record.getType().equals("issue") && !record.getType().equals("return")))
            throw new IllegalArgumentException("Type must be 'issue' or 'return'.");

        // For issues, check we have enough stock
        if ("issue".equals(record.getType())) {
            Part part = partDAO.findById(record.getPartId());
            if (part != null && part.getQtyOnHand() < record.getQty()) {
                throw new IllegalArgumentException(
                        "Insufficient stock. Available: " + part.getQtyOnHand()
                        + " " + part.getUnit() + ".");
            }
        }

        // ── Transactional write ───────────────────────────────────────────
        // delta: negative for issue (stock out), positive for return (stock in)
        int delta = "issue".equals(record.getType()) ? -record.getQty() : +record.getQty();

        Connection conn = DBConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            conn.createStatement().execute("PRAGMA foreign_keys = ON;");

            issueDAO.insert(record, conn);
            issueDAO.adjustPartQty(record.getPartId(), delta, conn);

            conn.commit();
            System.out.printf("[Issue] %s %d × part#%d → part qty adjusted by %+d%n",
                    record.getType(), record.getQty(), record.getPartId(), delta);

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    public List<IssueRecord> getAllRecords() throws SQLException {
        return issueDAO.findAll();
    }

    public List<IssueRecord> getRecordsForEquipment(int equipmentId) throws SQLException {
        return issueDAO.findByEquipment(equipmentId);
    }

    public List<IssueRecord> getRecordsForPart(int partId) throws SQLException {
        return issueDAO.findByPart(partId);
    }

    public void deleteRecord(int id) throws SQLException {
        issueDAO.delete(id);
    }
}
