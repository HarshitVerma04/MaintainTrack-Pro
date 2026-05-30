package com.maintaintrack.api.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "part")
public class Part {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sync_id")
    private String syncId;           // ← NEW

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(nullable = false)
    private String name;

    @Column(name = "qty_on_hand")
    private Integer qtyOnHand;

    @Column(name = "min_qty")
    private Integer minQty;

    private String unit;

    @Column(name = "unit_cost")
    private BigDecimal unitCost;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private Boolean synced;

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId()                                { return id; }
    public void setId(Long id)                         { this.id = id; }
    public String getSyncId()                          { return syncId; }
    public void setSyncId(String syncId)               { this.syncId = syncId; }
    public Supplier getSupplier()                      { return supplier; }
    public void setSupplier(Supplier supplier)         { this.supplier = supplier; }
    public String getName()                            { return name; }
    public void setName(String name)                   { this.name = name; }
    public Integer getQtyOnHand()                      { return qtyOnHand; }
    public void setQtyOnHand(Integer qtyOnHand)        { this.qtyOnHand = qtyOnHand; }
    public Integer getMinQty()                         { return minQty; }
    public void setMinQty(Integer minQty)              { this.minQty = minQty; }
    public String getUnit()                            { return unit; }
    public void setUnit(String unit)                   { this.unit = unit; }
    public BigDecimal getUnitCost()                    { return unitCost; }
    public void setUnitCost(BigDecimal unitCost)       { this.unitCost = unitCost; }
    public LocalDateTime getUpdatedAt()                { return updatedAt; }
    public Boolean getSynced()                         { return synced; }
    public void setSynced(Boolean synced)              { this.synced = synced; }
}