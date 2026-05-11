package com.maintaintrack.models;

import java.time.LocalDate;

/**
 * BreakdownLog — plain Java model matching the BREAKDOWN_LOG table.
 */
public class BreakdownLog {

    private int       id;
    private int       equipmentId;
    private String    equipmentName;  // joined field for display
    private LocalDate occurredOn;
    private String    description;
    private String    resolvedBy;

    // ── Constructors ──────────────────────────────────────────────────────

    public BreakdownLog() {}

    public BreakdownLog(int id, int equipmentId, LocalDate occurredOn,
                        String description, String resolvedBy) {
        this.id          = id;
        this.equipmentId = equipmentId;
        this.occurredOn  = occurredOn;
        this.description = description;
        this.resolvedBy  = resolvedBy;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public int       getId()                        { return id; }
    public void      setId(int id)                  { this.id = id; }

    public int       getEquipmentId()               { return equipmentId; }
    public void      setEquipmentId(int e)          { this.equipmentId = e; }

    public String    getEquipmentName()             { return equipmentName; }
    public void      setEquipmentName(String e)     { this.equipmentName = e; }

    public LocalDate getOccurredOn()                { return occurredOn; }
    public void      setOccurredOn(LocalDate d)     { this.occurredOn = d; }

    public String    getDescription()               { return description; }
    public void      setDescription(String d)       { this.description = d; }

    public String    getResolvedBy()                { return resolvedBy; }
    public void      setResolvedBy(String r)        { this.resolvedBy = r; }
}
