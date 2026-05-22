package com.maintaintrack.api.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "breakdown_log")
public class BreakdownLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "occurred_on", nullable = false)
    private String occurredOn;

    private String description;

    @Column(name = "resolved_by")
    private String resolvedBy;

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
    public Equipment getEquipment() { return equipment; }
    public void setEquipment(Equipment equipment) { this.equipment = equipment; }
    public String getOccurredOn() { return occurredOn; }
    public void setOccurredOn(String occurredOn) { this.occurredOn = occurredOn; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Boolean getSynced() { return synced; }
    public void setSynced(Boolean synced) { this.synced = synced; }
}