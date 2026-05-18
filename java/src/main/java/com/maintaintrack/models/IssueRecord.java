package com.maintaintrack.models;

import java.time.LocalDate;

/**
 * IssueRecord — plain Java model matching the ISSUE_RECORD table.
 *
 * type must be either "issue" (stock goes out) or "return" (stock comes back).
 * partName and equipmentName are joined fields for display — not stored in the table.
 */
public class IssueRecord {

    private int       id;
    private int       partId;
    private String    partName;        // joined — not in ISSUE_RECORD table
    private int       equipmentId;
    private String    equipmentName;   // joined — not in ISSUE_RECORD table
    private LocalDate issuedOn;
    private int       qty;
    private String    issuedBy;
    private String    type;            // "issue" or "return"

    // ── Constructors ──────────────────────────────────────────────────────

    public IssueRecord() {}

    public IssueRecord(int id, int partId, int equipmentId,
                       LocalDate issuedOn, int qty, String issuedBy, String type) {
        this.id          = id;
        this.partId      = partId;
        this.equipmentId = equipmentId;
        this.issuedOn    = issuedOn;
        this.qty         = qty;
        this.issuedBy    = issuedBy;
        this.type        = type;
    }

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

    public LocalDate getIssuedOn()                  { return issuedOn; }
    public void      setIssuedOn(LocalDate d)       { this.issuedOn = d; }

    public int       getQty()                       { return qty; }
    public void      setQty(int q)                  { this.qty = q; }

    public String    getIssuedBy()                  { return issuedBy; }
    public void      setIssuedBy(String s)          { this.issuedBy = s; }

    public String    getType()                      { return type; }
    public void      setType(String t)              { this.type = t; }

    /** Returns a display label: "Issue" or "Return". */
    public String getTypeDisplay() {
        if (type == null) return "";
        return "issue".equals(type) ? "Issue" : "Return";
    }
}
