package com.maintaintrack.models;

import java.time.LocalDate;

/**
 * IssueRecord — POJO for the ISSUE_RECORD table.
 *
 * Day 8: breakdownId   — optional FK to BREAKDOWN_LOG
 * Day 9: maintenanceId — optional FK to MAINTENANCE_LOG
 *
 * Traceability matrix:
 *   Both null       → standalone stock draw (no job context)
 *   breakdownId set → repair work order (parts used to fix a breakdown)
 *   maintenanceId set → PM consumables (parts used in a scheduled PM job)
 *   Both set        → allowed but unusual (breakdown that occurred during PM)
 */
public class IssueRecord {

    private int       id;
    private int       partId;
    private String    partName;
    private int       equipmentId;
    private String    equipmentName;

    // Day 8
    private Integer   breakdownId;
    private String    breakdownDesc;

    // Day 9
    private Integer   maintenanceId;
    private String    maintenanceNotes;

    private LocalDate issuedOn;
    private int       qty;
    private String    issuedBy;
    private String    type;

    public IssueRecord() {}

    // ── Getters & Setters ─────────────────────────────────────────────────

    public int       getId()                          { return id; }
    public void      setId(int id)                    { this.id = id; }

    public int       getPartId()                      { return partId; }
    public void      setPartId(int p)                 { this.partId = p; }

    public String    getPartName()                    { return partName; }
    public void      setPartName(String p)            { this.partName = p; }

    public int       getEquipmentId()                 { return equipmentId; }
    public void      setEquipmentId(int e)            { this.equipmentId = e; }

    public String    getEquipmentName()               { return equipmentName; }
    public void      setEquipmentName(String e)       { this.equipmentName = e; }

    public Integer   getBreakdownId()                 { return breakdownId; }
    public void      setBreakdownId(Integer b)        { this.breakdownId = b; }

    public String    getBreakdownDesc()               { return breakdownDesc; }
    public void      setBreakdownDesc(String b)       { this.breakdownDesc = b; }

    public Integer   getMaintenanceId()               { return maintenanceId; }
    public void      setMaintenanceId(Integer m)      { this.maintenanceId = m; }

    public String    getMaintenanceNotes()            { return maintenanceNotes; }
    public void      setMaintenanceNotes(String m)    { this.maintenanceNotes = m; }

    public LocalDate getIssuedOn()                    { return issuedOn; }
    public void      setIssuedOn(LocalDate d)         { this.issuedOn = d; }

    public int       getQty()                         { return qty; }
    public void      setQty(int q)                    { this.qty = q; }

    public String    getIssuedBy()                    { return issuedBy; }
    public void      setIssuedBy(String i)            { this.issuedBy = i; }

    public String    getType()                        { return type; }
    public void      setType(String t)                { this.type = t; }

    public String getTypeDisplay() {
        if (type == null) return "-";
        return "issue".equals(type) ? "Issue" : "Return";
    }

    /** True if linked to a breakdown work order. */
    public boolean isBreakdownWorkOrder() { return breakdownId != null; }

    /** True if linked to a maintenance PM job. */
    public boolean isMaintenanceWorkOrder() { return maintenanceId != null; }

    /**
     * Human-readable context label for the table's Work Order column.
     * Priority: breakdown > maintenance > standalone
     */
    public String getWorkOrderLabel() {
        if (breakdownId != null) {
            String d = breakdownDesc != null ? breakdownDesc : "Breakdown #" + breakdownId;
            return d.length() > 30 ? d.substring(0, 27) + "..." : d;
        }
        if (maintenanceId != null) {
            String n = maintenanceNotes != null
                    ? maintenanceNotes : "PM Job #" + maintenanceId;
            return n.length() > 30 ? n.substring(0, 27) + "..." : n;
        }
        return "-- Standalone";
    }
}
