
/**
 * Domain model for the {@code parts} table.
 */
public class Parts {

    // ----- Fields ------------------------------------------
    private int    id;
    private String name;
    private String partNumber;
    private String description;
    private int    qtyOnHand;
    private int    minQty;          // low-stock threshold used by alert engine (Day 13)
    private double unitCost;
    private int    supplierId;      // FK → suppliers.id (0 = none)

    // ----- Constructors --------------------------------------------
    public Parts() {}

    public Parts(int id, String name, String partNumber, String description,
                 int qtyOnHand, int minQty, double unitCost, int supplierId) {
        this.id          = id;
        this.name        = name;
        this.partNumber  = partNumber;
        this.description = description;
        this.qtyOnHand   = qtyOnHand;
        this.minQty      = minQty;
        this.unitCost    = unitCost;
        this.supplierId  = supplierId;
    }

    // ----- Derived helpers -----------------------------------
    /** True when qty_on_hand falls at or below min_qty — drives Day 13 alert. */
    public boolean isLowStock() {
        return qtyOnHand <= minQty;
    }

    // ----- Getters / Setters -----------------------------------
    public int    getId()                  { return id; }
    public void   setId(int id)            { this.id = id; }

    public String getName()               { return name; }
    public void   setName(String v)       { this.name = v; }

    public String getPartNumber()          { return partNumber; }
    public void   setPartNumber(String v)  { this.partNumber = v; }

    public String getDescription()         { return description; }
    public void   setDescription(String v) { this.description = v; }

    public int    getQtyOnHand()           { return qtyOnHand; }
    public void   setQtyOnHand(int v)      { this.qtyOnHand = v; }

    public int    getMinQty()              { return minQty; }
    public void   setMinQty(int v)         { this.minQty = v; }

    public double getUnitCost()            { return unitCost; }
    public void   setUnitCost(double v)    { this.unitCost = v; }

    public int    getSupplierId()          { return supplierId; }
    public void   setSupplierId(int v)     { this.supplierId = v; }

    @Override
    public String toString() {
        return "Parts{id=" + id + ", name='" + name + "', qty=" + qtyOnHand + "}";
    }
}
