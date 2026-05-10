package com.maintaintrack.models;

/**
 * Supplier — plain Java model matching the SUPPLIER table.
 */
public class Supplier {

    private int    id;
    private String name;
    private String contactName;
    private String phone;
    private String email;

    // ── Constructors ──────────────────────────────────────────────────────

    public Supplier() {}

    public Supplier(int id, String name, String contactName, String phone, String email) {
        this.id          = id;
        this.name        = name;
        this.contactName = contactName;
        this.phone       = phone;
        this.email       = email;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public int    getId()                        { return id; }
    public void   setId(int id)                  { this.id = id; }

    public String getName()                      { return name; }
    public void   setName(String name)           { this.name = name; }

    public String getContactName()               { return contactName; }
    public void   setContactName(String c)       { this.contactName = c; }

    public String getPhone()                     { return phone; }
    public void   setPhone(String phone)         { this.phone = phone; }

    public String getEmail()                     { return email; }
    public void   setEmail(String email)         { this.email = email; }

    /**
     * Used by the ComboBox in PartController —
     * shows the supplier name as the display value.
     */
    @Override
    public String toString() { return name; }
}
