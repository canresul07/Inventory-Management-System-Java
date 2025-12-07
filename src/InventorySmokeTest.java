/**
 * Lightweight sanity checks without external test frameworks.
 * Run with:
 *   java -cp "bin:lib/sqlite-jdbc-3.51.0.0.jar" InventorySmokeTest
 */
public class InventorySmokeTest {
    public static void main(String[] args) {
        testStrategy();
        testProductValidation();
        testDatabaseRoundTrip();
        System.out.println("Smoke tests passed.");
    }

    private static void testStrategy() {
        StockAlertStrategy s = new FixedThresholdStrategy(5);
        Product p = new Product("Test", 4, 10.0, "A1");
        String msg = s.evaluate(p);
        assertTrue(msg != null && msg.contains("low on stock"), "Strategy should alert when qty <= threshold");

        p.setQuantity(6);
        msg = s.evaluate(p);
        assertTrue(msg == null, "Strategy should be quiet when qty above threshold");
    }

    private static void testProductValidation() {
        boolean thrown = false;
        try { new Product("X", -1, 1.0); } catch (IllegalArgumentException e) { thrown = true; }
        assertTrue(thrown, "Negative quantity must fail");

        thrown = false;
        try { new Product("X", 1, -5.0); } catch (IllegalArgumentException e) { thrown = true; }
        assertTrue(thrown, "Negative price must fail");

        Product p = new Product("Valid", 1, 1.0);
        thrown = false;
        try { p.setQuantity(-2); } catch (IllegalArgumentException e) { thrown = true; }
        assertTrue(thrown, "Setter negative quantity must fail");
    }

    private static void testDatabaseRoundTrip() {
        String testDb = "jdbc:sqlite:test_inventory.db";
        InventoryManager mgr = InventoryManager.getInstance();
        mgr.setDbUrl(testDb);
        // Build tiny tree
        Category root = mgr.getRootCategory();
        root.getChildren().clear();
        Category cat = new Category("TestCat");
        Product prod = new Product("TestProd", 2, 5.0, "Z1");
        prod.setAlertThresholdOverride(3);
        cat.add(prod);
        root.add(cat);

        mgr.saveToDatabase();
        Category loaded = DatabaseHelper.loadInventory(testDb);
        assertTrue(loaded != null && loaded.getChildrenCount() == 1, "Category should load");
        Category loadedCat = (Category) loaded.getChild(0);
        Product loadedProd = (Product) loadedCat.getChild(0);
        assertTrue(loadedProd.getQuantity() == 2, "Quantity should persist");
        assertTrue("Z1".equals(loadedProd.getLocation()), "Location should persist");
    }

    private static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new RuntimeException("Assertion failed: " + msg);
    }
}
