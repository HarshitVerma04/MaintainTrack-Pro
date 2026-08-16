package com.maintaintrack.services;

import com.maintaintrack.dao.PartDAO;
import com.maintaintrack.models.Part;
import com.maintaintrack.sync.SyncService;
import com.maintaintrack.sync.SyncTask;

import java.sql.SQLException;
import java.util.List;

public class PartService {

    private final PartDAO dao = new PartDAO();

    public void addPart(Part p) throws SQLException {
        validate(p);
        dao.insert(p);
        SyncService.getInstance().push(new SyncTask(
                "PART", p.getId(),
                toJson(p), SyncTask.Operation.INSERT));
    }

    public void updatePart(Part p) throws SQLException {
        validate(p);
        dao.update(p);
        SyncService.getInstance().push(new SyncTask(
                "PART", p.getId(),
                toJson(p), SyncTask.Operation.UPDATE));
    }

    public void deletePart(int id) throws SQLException {
        dao.delete(id);
        SyncService.getInstance().push(new SyncTask(
                "PART", id,
                "{\"id\":" + id + "}", SyncTask.Operation.DELETE));
    }

    public List<Part> getAllParts() throws SQLException {
        return dao.findAll();
    }

    public Part getById(int id) throws SQLException {
        return dao.findById(id);
    }

    public List<Part> getLowStockParts() throws SQLException {
        return dao.findLowStock();
    }

    public List<Part> search(String keyword) throws SQLException {
        return dao.search(keyword);
    }

    private String toJson(Part p) {
        String syncId = getSyncId(p.getId());
        return String.format(
                "{\"syncId\":\"%s\",\"name\":\"%s\",\"qtyOnHand\":%d," +
                        "\"minQty\":%d,\"unit\":\"%s\",\"unitCost\":%.2f," +
                        "\"supplierId\":%d,\"updatedAt\":\"%s\"}",
                syncId,
                escape(p.getName()),
                p.getQtyOnHand(),
                p.getMinQty(),
                escape(p.getUnit()),
                p.getUnitCost(),
                p.getSupplierId(),
                java.time.LocalDateTime.now().toString()
        );
    }

    private String getSyncId(int id) {
        String sql = "SELECT sync_id FROM PART WHERE id = ?";
        try (java.sql.Connection conn =
                     com.maintaintrack.dao.DBConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String s = rs.getString("sync_id");
                return s != null ? s : java.util.UUID.randomUUID().toString();
            }
        } catch (Exception e) {
            System.err.println("[Sync] Part getSyncId failed: " + e.getMessage());
        }
        return java.util.UUID.randomUUID().toString();
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    private void validate(Part p) {
        if (p.getName() == null || p.getName().isBlank())
            throw new IllegalArgumentException("Part name is required.");
        if (p.getQtyOnHand() < 0)
            throw new IllegalArgumentException("Quantity on hand cannot be negative.");
        if (p.getMinQty() < 0)
            throw new IllegalArgumentException("Minimum quantity cannot be negative.");
        if (p.getUnitCost() < 0)
            throw new IllegalArgumentException("Unit cost cannot be negative.");
        if (p.getUnit() == null || p.getUnit().isBlank())
            throw new IllegalArgumentException("Unit is required (e.g. pcs, kg, can).");
    }
}