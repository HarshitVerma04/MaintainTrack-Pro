
/**
 * Domain model for the {@code suppliers} table.
 */
public class Supplier {

    private int    id;
    private String name;
    private String contactName;
    private String phone;
    private String email;
    private String address;

    public Supplier() {}

    public Supplier(int id, String name, String contactName,
                    String phone, String email, String address) {
        this.id          = id;
        this.name        = name;
        this.contactName = contactName;
        this.phone       = phone;
        this.email       = email;
        this.address     = address;
    }

    public int    getId()                   { return id; }
    public void   setId(int id)             { this.id = id; }

    public String getName()                 { return name; }
    public void   setName(String v)         { this.name = v; }

    public String getContactName()          { return contactName; }
    public void   setContactName(String v)  { this.contactName = v; }

    public String getPhone()                { return phone; }
    public void   setPhone(String v)        { this.phone = v; }

    public String getEmail()                { return email; }
    public void   setEmail(String v)        { this.email = v; }

    public String getAddress()              { return address; }
    public void   setAddress(String v)      { this.address = v; }

    @Override
    public String toString() {
        return "Supplier{id=" + id + ", name='" + name + "'}";
    }
}
