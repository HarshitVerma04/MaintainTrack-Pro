package com.maintaintrack.models;

import java.time.LocalDate;

/**
 * IssueRecord — POJO for the ISSUE_RECORD table.
 *
 * Day 8 addition: breakdownId (optional FK to BREAKDOWN_LOG).
 * When set, the issue is part of a work order tied to a specific
 * breakdown incident — enabling full traceability:
 *   Breakdown → Parts issued → Cost of that repair
 *
 * When null, the issue is a standalone stock draw not tied to a breakdown.
 */
public class IssueRecord {

    private int       id;
    private int       partId;
    private String    partName;        // joined for display
    private int       equipmentId;
    private String    equipmentName;   // joined for display
    private Integer   breakdownId;     // nullable — Day 8 work order link
    private String    breakdownDesc;   // joined for display
    private LocalDate issuedOn;
    private int       qty;
    private String    issuedBy;
    private String    type;            // 'issue' or 'return'

    public IssueRecord() {}

    // ── Getters & Setters ─────────────────────────────────────────────────

    public int       getId()                        { return id; }
    public void      setId(int id)                  { this.id = id; }

    public int       getPartId()                    { return partId; }
    public void      setPartId(int p)               { this.partId = p; }

    public String    getPartName()                  { return partName; }
    public void      setPartName(String p)          { this.partName = p; }

    public int       getEquipmentId()               { return equipmentId; }
    public void      setEquipmentId(int e)          { this.equipmentId = e; }

    public String    getEquipmentName()             { return equipmentName; }
    public void      setEquipmentName(String e)     { this.equipmentName = e; }

    public Integer   getBreakdownId()               { return breakdownId; }
    public void      setBreakdownId(Integer b)      { this.breakdownId = b; }

    public String    getBreakdownDesc()             { return breakdownDesc; }
    public void      setBreakdownDesc(String b)     { this.breakdownDesc = b; }

    public LocalDate getIssuedOn()                  { return issuedOn; }
    public void      setIssuedOn(LocalDate d)       { this.issuedOn = d; }

    public int       getQty()                       { return qty; }
    public void      setQty(int q)                  { this.qty = q; }

    public String    getIssuedBy()                  { return issuedBy; }
    public void      setIssuedBy(String i)          { this.issuedBy = i; }

    public String    getType()                      { return type; }
    public void      setType(String t)              { this.type = t; }

    /**
     * Used by TableView cell factory for colour-coded display.
     * Returns "Issue" or "Return" (capitalised).
     */
    public String getTypeDisplay() {
        if (type == null) return "-";
        return "issue".equals(type) ? "Issue" : "Return";
    }

    /**
     * True if this issue is linked to a specific breakdown incident.
     */
    public boolean isWorkOrder() {
        return breakdownId != null;
    }
}
