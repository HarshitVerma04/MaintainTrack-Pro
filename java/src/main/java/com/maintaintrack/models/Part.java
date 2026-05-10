package com.maintaintrack.models;

/**
 * Part — plain Java model matching the PART table.
 */
public class Part {

    private int      id;
    private int      supplierId;
    private String   supplierName;   // joined field — not stored in PART table
    private String   name;
    private int      qtyOnHand;
    private int      minQty;
    private String   unit;
    private double   unitCost;

    // ── Constructors ──────────────────────────────────────────────────────

    public Part() {}

    public Part(int id, int supplierId, String name,
                int qtyOnHand, int minQty, String unit, double unitCost) {
        this.id         = id;
        this.supplierId = supplierId;
        this.name       = name;
        this.qtyOnHand  = qtyOnHand;
        this.minQty     = minQty;
        this.unit       = unit;
        this.unitCost   = unitCost;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public int    getId()                        { return id; }
    public void   setId(int id)                  { this.id = id; }

    public int    getSupplierId()                { return supplierId; }
    public void   setSupplierId(int s)           { this.supplierId = s; }

    public String getSupplierName()              { return supplierName; }
    public void   setSupplierName(String s)      { this.supplierName = s; }

    public String getName()                      { return name; }
    public void   setName(String name)           { this.name = name; }

    public int    getQtyOnHand()                 { return qtyOnHand; }
    public void   setQtyOnHand(int q)            { this.qtyOnHand = q; }

    public int    getMinQty()                    { return minQty; }
    public void   setMinQty(int m)               { this.minQty = m; }

    public String getUnit()                      { return unit; }
    public void   setUnit(String unit)           { this.unit = unit; }

    public double getUnitCost()                  { return unitCost; }
    public void   setUnitCost(double c)          { this.unitCost = c; }

    /** True when stock is at or below the reorder threshold. */
    public boolean isLowStock() {
        return qtyOnHand <= minQty;
    }

    @Override
    public String toString() { return name; }
}
