

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Day 5 — Supplier CRUD.
 */
public class SupplierDAO {

    // ------ CREATE ------------------------------------------

    public int insert(Supplier s) throws SQLException {
        String sql = """
            INSERT INTO suppliers (name, contact_name, phone, email, address)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getContactName());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getEmail());
            ps.setString(5, s.getAddress());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    s.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Insert failed — no generated key returned.");
    }

    // ------ READ ------------------------------------------

    public List<Supplier> findAll() throws SQLException {
        List<Supplier> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM suppliers ORDER BY name")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Optional<Supplier> findById(int id) throws SQLException {
        try (PreparedStatement ps =
                     conn().prepareStatement("SELECT * FROM suppliers WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    // ----- UPDATE ----------------------------------------------

    public void update(Supplier s) throws SQLException {
        String sql = """
            UPDATE suppliers
               SET name         = ?,
                   contact_name = ?,
                   phone        = ?,
                   email        = ?,
                   address      = ?
             WHERE id = ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getContactName());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getEmail());
            ps.setString(5, s.getAddress());
            ps.setInt(6, s.getId());
            ps.executeUpdate();
        }
    }

    // ----- DELETE -------------------------------------------

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps =
                     conn().prepareStatement("DELETE FROM suppliers WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ----- Private helpers ----------------------------------------

    private Connection conn() { return DatabaseManager.getConnection(); }

    private Supplier map(ResultSet rs) throws SQLException {
        return new Supplier(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("contact_name"),
            rs.getString("phone"),
            rs.getString("email"),
            rs.getString("address")
        );
    }
}
