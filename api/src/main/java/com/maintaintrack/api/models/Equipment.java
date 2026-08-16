package com.maintaintrack.api.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sync_id")
    private String syncId;

    @Column(nullable = false)
    private String name;

    private String location;
    private String status;

    @Column(name = "next_maintenance_date")
    private String nextMaintenanceDate;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private Boolean synced;

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId()                                    { return id; }
    public void setId(Long id)                             { this.id = id; }
    public String getSyncId()                              { return syncId; }
    public void setSyncId(String syncId)                   { this.syncId = syncId; }
    public String getName()                                { return name; }
    public void setName(String name)                       { this.name = name; }
    public String getLocation()                            { return location; }
    public void setLocation(String location)               { this.location = location; }
    public String getStatus()                              { return status; }
    public void setStatus(String status)                   { this.status = status; }
    public String getNextMaintenanceDate()                 { return nextMaintenanceDate; }
    public void setNextMaintenanceDate(String d)           { this.nextMaintenanceDate = d; }
    public Integer getIntervalDays()                       { return intervalDays; }
    public void setIntervalDays(Integer intervalDays)      { this.intervalDays = intervalDays; }
    public LocalDateTime getUpdatedAt()                    { return updatedAt; }
    public Boolean getSynced()                             { return synced; }
    public void setSynced(Boolean synced)                  { this.synced = synced; }
}