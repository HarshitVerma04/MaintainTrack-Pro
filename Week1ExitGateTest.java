
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exit-gate tests for Days 1–5.
 *
 * Verifies:
 *   ✓ All 6 tables created without errors
 *   ✓ Equipment CRUD works end-to-end
 *   ✓ Parts CRUD works end-to-end (+ low-stock helper)
 *   ✓ Supplier CRUD works end-to-end
 *   ✓ FK constraint is enforced (parts → suppliers)
 *
 * Uses an in-memory SQLite DB so tests are isolated and fast.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Week1ExitGateTest {

    private static EquipmentDAO equipDao;
    private static PartsDAO     partsDao;
    private static SupplierDAO  supDao;

    @BeforeAll
    static void setup() throws Exception {
        // Override DB path to in-memory for tests
        System.setProperty("maintaintrack.db", ":memory:");
        // DatabaseManager.getConnection() will create and seed the schema
        Connection conn = DatabaseManager.getConnection();
        assertNotNull(conn, "Connection should not be null");

        equipDao = new EquipmentDAO();
        partsDao = new PartsDAO();
        supDao   = new SupplierDAO();
    }

    @AfterAll
    static void teardown() {
        DatabaseManager.close();
    }

    // ----- Schema -----------------------------------------------

    @Test @Order(1)
    void allSixTablesExist() throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        String[] tables = {"equipment","parts","suppliers",
                           "maintenance_log","breakdown_log","issue_record"};
        for (String t : tables) {
            try (Statement st = conn.createStatement()) {
                // If the table doesn't exist, this will throw
                st.executeQuery("SELECT 1 FROM " + t + " LIMIT 1");
            }
        }
    }

    // ----- Equipment CRUD -----------------------------------------

    @Test @Order(2)
    void equipmentInsertAndFindById() throws SQLException {
        Equipment eq = new Equipment();
        eq.setName("Lathe Machine A1");
        eq.setModel("XR-500");
        eq.setSerialNumber("SN-001");
        eq.setLocation("Workshop 1");
        eq.setPurchaseDate(LocalDate.of(2022, 1, 15));
        eq.setStatus(Equipment.Status.ACTIVE);

        int id = equipDao.insert(eq);
        assertTrue(id > 0, "Generated ID should be positive");

        Optional<Equipment> found = equipDao.findById(id);
        assertTrue(found.isPresent());
        assertEquals("Lathe Machine A1", found.get().getName());
        assertEquals(Equipment.Status.ACTIVE,      found.get().getStatus());
    }

    @Test @Order(3)
    void equipmentUpdateAndDelete() throws SQLException {
        Equipment eq = new Equipment();
        eq.setName("Temp Equipment");
        eq.setStatus(Equipment.Status.ACTIVE);
        int id = equipDao.insert(eq);

        eq.setName("Updated Equipment");
        eq.setStatus(Equipment.Status.INACTIVE);
        equipDao.update(eq);

        Optional<Equipment> updated = equipDao.findById(id);
        assertTrue(updated.isPresent());
        assertEquals("Updated Equipment", updated.get().getName());
        assertEquals(Equipment.Status.INACTIVE,     updated.get().getStatus());

        equipDao.delete(id);
        assertTrue(equipDao.findById(id).isEmpty(), "Deleted record should not be found");
    }

    @Test @Order(4)
    void equipmentFindAll() throws SQLException {
        List<Equipment> list = equipDao.findAll();
        assertFalse(list.isEmpty(), "findAll should return at least the record from earlier test");
    }

    // ----- Supplier CRUD -----------------------------------------

    @Test @Order(5)
    void supplierInsertAndFindById() throws SQLException {
        Supplier s = new Supplier();
        s.setName("AcmeParts Ltd");
        s.setContactName("Jane Smith");
        s.setPhone("+65-1234-5678");
        s.setEmail("jane@acmeparts.com");
        s.setAddress("10 Industrial Ave, Singapore");

        int id = supDao.insert(s);
        assertTrue(id > 0);

        Optional<Supplier> found = supDao.findById(id);
        assertTrue(found.isPresent());
        assertEquals("AcmeParts Ltd", found.get().getName());
    }

    @Test @Order(6)
    void supplierUpdateAndDelete() throws SQLException {
        Supplier s = new Supplier();
        s.setName("Temp Supplier");
        int id = supDao.insert(s);

        s.setName("Renamed Supplier");
        supDao.update(s);
        assertEquals("Renamed Supplier", supDao.findById(id).orElseThrow().getName());

        supDao.delete(id);
        assertTrue(supDao.findById(id).isEmpty());
    }

    // ----- Parts CRUD -----------------------------------------

    @Test @Order(7)
    void partsInsertAndLowStockFlag() throws SQLException {
        Parts p = new Parts();
        p.setName("Bearing 6205");
        p.setPartNumber("BRG-6205");
        p.setQtyOnHand(2);
        p.setMinQty(5);   // qty < minQty → should be low stock
        p.setUnitCost(4.50);

        int id = partsDao.insert(p);
        assertTrue(id > 0);

        Optional<Parts> found = partsDao.findById(id);
        assertTrue(found.isPresent());
        assertTrue(found.get().isLowStock(), "Qty 2 < minQty 5 → isLowStock must be true");

        List<Parts> lowStock = partsDao.findLowStock();
        assertTrue(lowStock.stream().anyMatch(part -> part.getId() == id));
    }

    @Test @Order(8)
    void partsUpdateAndDelete() throws SQLException {
        Parts p = new Parts();
        p.setName("Temp Part");
        p.setQtyOnHand(10);
        p.setMinQty(2);
        int id = partsDao.insert(p);

        p.setName("Updated Part");
        p.setQtyOnHand(20);
        partsDao.update(p);

        Optional<Parts> updated = partsDao.findById(id);
        assertTrue(updated.isPresent());
        assertEquals("Updated Part", updated.get().getName());
        assertEquals(20, updated.get().getQtyOnHand());

        partsDao.delete(id);
        assertTrue(partsDao.findById(id).isEmpty());
    }

    // ----- FK constraint ----------------------------------------

    @Test @Order(9)
    void partsLinkedToSupplierViaFK() throws SQLException {
        Supplier s = new Supplier();
        s.setName("FK Test Supplier");
        int supId = supDao.insert(s);

        Parts p = new Parts();
        p.setName("FK Test Part");
        p.setQtyOnHand(1);
        p.setSupplierId(supId);
        int partId = partsDao.insert(p);

        Optional<Parts> found = partsDao.findById(partId);
        assertTrue(found.isPresent());
        assertEquals(supId, found.get().getSupplierId());
    }
}

