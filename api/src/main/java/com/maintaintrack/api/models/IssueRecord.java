package com.maintaintrack.api.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "issue_record")
public class IssueRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sync_id")
    private String syncId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breakdown_id")
    private BreakdownLog breakdown;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_id")
    private MaintenanceLog maintenance;

    @Column(name = "issued_on", nullable = false)
    private String issuedOn;

    @Column(nullable = false)
    private Integer qty;

    @Column(name = "issued_by")
    private String issuedBy;

    private String type;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private Boolean synced;

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Part getPart() { return part; }
    public void setPart(Part part) { this.part = part; }
    public Equipment getEquipment() { return equipment; }
    public void setEquipment(Equipment equipment) { this.equipment = equipment; }
    public BreakdownLog getBreakdown() { return breakdown; }
    public void setBreakdown(BreakdownLog breakdown) { this.breakdown = breakdown; }
    public MaintenanceLog getMaintenance() { return maintenance; }
    public void setMaintenance(MaintenanceLog maintenance) { this.maintenance = maintenance; }
    public String getIssuedOn() { return issuedOn; }
    public void setIssuedOn(String issuedOn) { this.issuedOn = issuedOn; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public String getIssuedBy() { return issuedBy; }
    public void setIssuedBy(String issuedBy) { this.issuedBy = issuedBy; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Boolean getSynced() { return synced; }
    public void setSynced(Boolean synced) { this.synced = synced; }
    public String getSyncId()              { return syncId; }
    public void setSyncId(String syncId)   { this.syncId = syncId; }
}