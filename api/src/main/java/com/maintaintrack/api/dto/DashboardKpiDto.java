package com.maintaintrack.api.dto;

import java.util.List;
import java.util.Map;

public class DashboardKpiDto {

    private long totalEquipment;
    private long overdueCount;
    private long underMaintenanceCount;
    private long lowStockCount;
    private long openWorkOrders;
    private long inProgressWorkOrders;
    private List<Map<String, Object>> recentActivity;

    // ── Getters & Setters ────────────────────────────────────
    public long getTotalEquipment() { return totalEquipment; }
    public void setTotalEquipment(long totalEquipment) { this.totalEquipment = totalEquipment; }
    public long getOverdueCount() { return overdueCount; }
    public void setOverdueCount(long overdueCount) { this.overdueCount = overdueCount; }
    public long getUnderMaintenanceCount() { return underMaintenanceCount; }
    public void setUnderMaintenanceCount(long v) { this.underMaintenanceCount = v; }
    public long getLowStockCount() { return lowStockCount; }
    public void setLowStockCount(long lowStockCount) { this.lowStockCount = lowStockCount; }
    public long getOpenWorkOrders() { return openWorkOrders; }
    public void setOpenWorkOrders(long openWorkOrders) { this.openWorkOrders = openWorkOrders; }
    public long getInProgressWorkOrders() { return inProgressWorkOrders; }
    public void setInProgressWorkOrders(long v) { this.inProgressWorkOrders = v; }
    public List<Map<String, Object>> getRecentActivity() { return recentActivity; }
    public void setRecentActivity(List<Map<String, Object>> recentActivity) { this.recentActivity = recentActivity; }
}