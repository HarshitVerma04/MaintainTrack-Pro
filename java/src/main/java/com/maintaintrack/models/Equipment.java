package com.maintaintrack.models;

import java.time.LocalDate;

/**
 * Equipment — plain Java model matching the EQUIPMENT table.
 * No business logic here — just fields, getters, setters.
 */
public class Equipment {

    private int       id;
    private String    name;
    private String    location;
    private String    status;
    private LocalDate nextMaintenanceDate;
    private int       intervalDays;

    // ── Constructors ──────────────────────────────────────────────────────

    public Equipment() {}

    public Equipment(int id, String name, String location, String status,
                     LocalDate nextMaintenanceDate, int intervalDays) {
        this.id                  = id;
        this.name                = name;
        this.location            = location;
        this.status              = status;
        this.nextMaintenanceDate = nextMaintenanceDate;
        this.intervalDays        = intervalDays;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }

    public String getName()                   { return name; }
    public void setName(String name)          { this.name = name; }

    public String getLocation()               { return location; }
    public void setLocation(String location)  { this.location = location; }

    public String getStatus()                 { return status; }
    public void setStatus(String status)      { this.status = status; }

    public LocalDate getNextMaintenanceDate()                        { return nextMaintenanceDate; }
    public void setNextMaintenanceDate(LocalDate nextMaintenanceDate){ this.nextMaintenanceDate = nextMaintenanceDate; }

    public int getIntervalDays()                  { return intervalDays; }
    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }

    @Override
    public String toString() {
        return name + " (" + location + ")";
    }
}
