
import java.time.LocalDate;

/**
 * Domain model for the {@code equipment} table.
 * Immutable fields are set via the builder; mutable state
 * (status, next_maint_date) is updated by the service layer.
 */
public class Equipment {

    public enum Status { ACTIVE, INACTIVE, RETIRED }

    // ---- Fields --------------------------------------------
    private int         id;
    private String      name;
    private String      model;
    private String      serialNumber;
    private String      location;
    private LocalDate   purchaseDate;
    private Status      status;
    private LocalDate   nextMaintDate;  // recalculated on Day 7

    // ---- Constructors --------------------------------------
    public Equipment() {}

    public Equipment(int id, String name, String model, String serialNumber,
                     String location, LocalDate purchaseDate,
                     Status status, LocalDate nextMaintDate) {
        this.id           = id;
        this.name         = name;
        this.model        = model;
        this.serialNumber = serialNumber;
        this.location     = location;
        this.purchaseDate = purchaseDate;
        this.status       = status;
        this.nextMaintDate = nextMaintDate;
    }

    // ---- Getters / Setters ---------------------------------
    public int         getId()            { return id; }
    public void        setId(int id)      { this.id = id; }

    public String      getName()          { return name; }
    public void        setName(String v)  { this.name = v; }

    public String      getModel()         { return model; }
    public void        setModel(String v) { this.model = v; }

    public String      getSerialNumber()           { return serialNumber; }
    public void        setSerialNumber(String v)   { this.serialNumber = v; }

    public String      getLocation()              { return location; }
    public void        setLocation(String v)      { this.location = v; }

    public LocalDate   getPurchaseDate()           { return purchaseDate; }
    public void        setPurchaseDate(LocalDate v){ this.purchaseDate = v; }

    public Status      getStatus()                 { return status; }
    public void        setStatus(Status v)         { this.status = v; }

    public LocalDate   getNextMaintDate()           { return nextMaintDate; }
    public void        setNextMaintDate(LocalDate v){ this.nextMaintDate = v; }

    @Override
    public String toString() {
        return "Equipment{id=" + id + ", name='" + name + "', status=" + status + "}";
    }
}
