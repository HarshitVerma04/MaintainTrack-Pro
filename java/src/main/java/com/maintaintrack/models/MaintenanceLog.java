package com.maintaintrack.models;

import java.time.LocalDate;

/**
 * MaintenanceLog — plain Java model matching the MAINTENANCE_LOG table.
 */
public class MaintenanceLog {

    private int       id;
    private int       equipmentId;
    private String    equipmentName;  // joined field for display
    private LocalDate doneOn;
    private String    notes;
    private String    doneBy;

    // ── Constructors ──────────────────────────────────────────────────────

    public MaintenanceLog() {}

    public MaintenanceLog(int id, int equipmentId, LocalDate doneOn,
                          String notes, String doneBy) {
        this.id          = id;
        this.equipmentId = equipmentId;
        this.doneOn      = doneOn;
        this.notes       = notes;
        this.doneBy      = doneBy;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public int       getId()                        { return id; }
    public void      setId(int id)                  { this.id = id; }

    public int       getEquipmentId()               { return equipmentId; }
    public void      setEquipmentId(int e)          { this.equipmentId = e; }

    public String    getEquipmentName()             { return equipmentName; }
    public void      setEquipmentName(String e)     { this.equipmentName = e; }

    public LocalDate getDoneOn()                    { return doneOn; }
    public void      setDoneOn(LocalDate d)         { this.doneOn = d; }

    public String    getNotes()                     { return notes; }
    public void      setNotes(String n)             { this.notes = n; }

    public String    getDoneBy()                    { return doneBy; }
    public void      setDoneBy(String d)            { this.doneBy = d; }

    @Override
    public String toString() {
        return "#" + id + " — " + equipmentName + " (" + doneOn + ")";
    }
}
