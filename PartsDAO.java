

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Day 4 — Parts CRUD.
 *
 * Stock quantity changes (issue / return) are handled atomically in
 * {@link IssueRecordDAO} to keep the transactional write on Day 11 isolated.
 */
public class PartsDAO {

    // ----- CREATE -------------------------------------------

    public int insert(Parts p) throws SQLException {
        String sql = """
            INSERT INTO parts
              (name, part_number, description, qty_on_hand, min_qty, unit_cost, supplier_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getPartNumber());
            ps.setString(3, p.getDescription());
            ps.setInt(4, p.getQtyOnHand());
            ps.setInt(5, p.getMinQty());
            ps.setDouble(6, p.getUnitCost());
            setNullableInt(ps, 7, p.getSupplierId());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    p.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Insert failed — no generated key returned.");
    }

    // ----- READ -------------------------------------------------

    public List<Parts> findAll() throws SQLException {
        List<Parts> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM parts ORDER BY name")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Optional<Parts> findById(int id) throws SQLException {
        try (PreparedStatement ps =
                     conn().prepareStatement("SELECT * FROM parts WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns parts where qty_on_hand <= min_qty.
     * Used by the alert engine on Day 13.
     */
    public List<Parts> findLowStock() throws SQLException {
        String sql = "SELECT * FROM parts WHERE qty_on_hand <= min_qty ORDER BY name";
        List<Parts> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    // ----- UPDATE -------------------------------------------

    public void update(Parts p) throws SQLException {
        String sql = """
            UPDATE parts
               SET name        = ?,
                   part_number = ?,
                   description = ?,
                   qty_on_hand = ?,
                   min_qty     = ?,
                   unit_cost   = ?,
                   supplier_id = ?
             WHERE id = ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getPartNumber());
            ps.setString(3, p.getDescription());
            ps.setInt(4, p.getQtyOnHand());
            ps.setInt(5, p.getMinQty());
            ps.setDouble(6, p.getUnitCost());
            setNullableInt(ps, 7, p.getSupplierId());
            ps.setInt(8, p.getId());
            ps.executeUpdate();
        }
    }

    // ----- DELETE -------------------------------------------

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps =
                     conn().prepareStatement("DELETE FROM parts WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ----- Private helpers -----------------------------------

    private Connection conn() { return DatabaseManager.getConnection(); }

    private Parts map(ResultSet rs) throws SQLException {
        return new Parts(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("part_number"),
            rs.getString("description"),
            rs.getInt("qty_on_hand"),
            rs.getInt("min_qty"),
            rs.getDouble("unit_cost"),
            rs.getInt("supplier_id")  // returns 0 if SQL NULL
        );
    }

    private static void setNullableInt(PreparedStatement ps, int idx, int value)
            throws SQLException {
        if (value <= 0) ps.setNull(idx, Types.INTEGER);
        else            ps.setInt(idx, value);
    }
}
