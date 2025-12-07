/* +
 * This class handles the Persistence Layer of the application.
 * It is responsible for bridging the gap between the Object-Oriented model (Composite Tree)
 * and the Relational model (SQLite Tables).
 *
 * It provides static utility methods to:
 * 1. Initialize the database schema (create tables).
 * 2. Save the current state of the inventory (Object -> DB).
 * 3. Load the inventory back into memory (DB -> Object).
 */

import java.sql.*;
import java.util.*;

public class DatabaseHelper {
    // This file will be created in your project folder (next to src, lib, bin)
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:inventory.db";

    /*
     * Initializes the database connection and schema.
     * Uses 'Class.forName' to ensure the SQLite JDBC driver is loaded.
     * Creates 'categories' and 'products' tables if they do not exist.
     * Also handles schema migration (adding new columns to existing databases).
     */
    public static void initDB() {
        initDB(DEFAULT_DB_URL);
    }

    public static void initDB(String dbUrl) {
        // 1. CRITICAL FIX: Force load the driver
        // This prevents "No suitable driver" errors in VS Code
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found. Check your lib folder!");
            e.printStackTrace();
            return;
        }

        // 2. Create Tables if they don't exist
        try (Connection conn = DriverManager.getConnection(dbUrl);
                Statement stmt = conn.createStatement()) {

            // Table for Categories
            stmt.execute("CREATE TABLE IF NOT EXISTS categories (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "parent_id INTEGER)");

            // Table for Products
            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "quantity INTEGER, " +
                    "price REAL, " +
                    "alert_threshold INTEGER, " +
                    "location TEXT, " +
                    "category_id INTEGER, " +
                    "FOREIGN KEY(category_id) REFERENCES categories(id))");

            // Ensure location column exists for older DBs
            try {
                stmt.execute("ALTER TABLE products ADD COLUMN location TEXT");
            } catch (SQLException ignored) {
                // Ignore if column already exists
            }
            try {
                stmt.execute("ALTER TABLE products ADD COLUMN alert_threshold INTEGER");
            } catch (SQLException ignored) {
                // Ignore if column already exists
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- SAVE (Wipes old data and saves current tree) ---
    public static void saveInventory(Category root) {
        saveInventory(root, DEFAULT_DB_URL);
    }

    /*
     * Saves the entire Inventory Tree to the database.
     * Strategy: "Wipe and Replace". It clears the tables and rewrites the tree.
     *
     * Transaction Management:
     * We use 'conn.setAutoCommit(false)' to treat the entire save operation
     * as a single atomic transaction. This ensures data integrity—if the save fails
     * halfway, the database isn't left in a broken state.
     */
    public static void saveInventory(Category root, String dbUrl) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false); // Start transaction for speed and safety

            // 1. Clear old data to avoid duplicates
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM products");
                stmt.execute("DELETE FROM categories");
            }

            // 2. Save Root and recursively save children
            saveCategory(conn, root, null);

            conn.commit(); // Commit transaction
            System.out.println("Database saved successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*
     * Recursive Save Method:
     * 1. Saves the current Category to the DB.
     * 2. Retrieves the auto-generated Primary Key (ID).
     * 3. Uses that ID as the 'parent_id' for its children (Recursion).
     */
    private static void saveCategory(Connection conn, Category cat, Integer parentId) throws SQLException {
        String sql = "INSERT INTO categories(name, parent_id) VALUES(?, ?)";
        int myId = -1;

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, cat.getName());

            if (parentId == null) {
                pstmt.setNull(2, Types.INTEGER);
            } else {
                pstmt.setInt(2, parentId);
            }

            pstmt.executeUpdate();

            // Get the auto-generated ID to use for children
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next())
                    myId = rs.getInt(1);
            }
        }

        // Recursively save children
        for (ProductComponent child : cat.getChildren()) {
            if (child instanceof Category) {
                saveCategory(conn, (Category) child, myId);
            } else if (child instanceof Product) {
                saveProduct(conn, (Product) child, myId);
            }
        }
    }

    private static void saveProduct(Connection conn, Product p, int categoryId) throws SQLException {
        String sql = "INSERT INTO products(name, quantity, price, alert_threshold, location, category_id) VALUES(?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, p.getName());
            pstmt.setInt(2, p.getQuantity());
            pstmt.setDouble(3, p.getPrice());
            if (p.getAlertThresholdOverride() == null) {
                pstmt.setNull(4, Types.INTEGER);
            } else {
                pstmt.setInt(4, p.getAlertThresholdOverride());
            }
            pstmt.setString(5, p.getLocation());
            pstmt.setInt(6, categoryId);
            pstmt.executeUpdate();
        }
    }

    // --- LOAD ---
    public static Category loadInventory() {
        return loadInventory(DEFAULT_DB_URL);
    }

    /*
     * Loads the Inventory from the database.
     * Challenge: The DB is flat (rows), but the application is a Tree (Composite).
     *
     * Algorithm:
     * 1. Load all Categories into a Map<ID, Category>.
     * 2. Re-establish parent-child links between Categories using the Map.
     * 3. Load all Products and add them to their respective Category in the Map.
     */
    public static Category loadInventory(String dbUrl) {
        Category root = null;
        Map<Integer, Category> catMap = new HashMap<>();

        // 1. CRITICAL FIX: Force load the driver here too (just in case load runs
        // first)
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        try (Connection conn = DriverManager.getConnection(dbUrl)) {

            // A. Load all Categories
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM categories")) {

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    int parentId = rs.getInt("parent_id"); // Returns 0 if null in SQLite

                    Category c = new Category(name);
                    catMap.put(id, c);

                    // If parent_id is 0 (or null), this is the root
                    if (parentId == 0) {
                        root = c;
                    }
                }
            }

            // B. Re-link Category Parent/Child relationships
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt
                            .executeQuery("SELECT * FROM categories WHERE parent_id IS NOT NULL AND parent_id != 0")) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int parentId = rs.getInt("parent_id");

                    Category child = catMap.get(id);
                    Category parent = catMap.get(parentId);

                    if (parent != null && child != null) {
                        parent.add(child);
                    }
                }
            }

            // C. Load Products and add to Categories
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM products")) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    int qty = rs.getInt("quantity");
                    double price = rs.getDouble("price");
                    Integer alertThreshold;
                    try {
                        int raw = rs.getInt("alert_threshold");
                        alertThreshold = rs.wasNull() ? null : raw;
                    } catch (SQLException e) {
                        alertThreshold = null;
                    }
                    String location;
                    try {
                        location = rs.getString("location");
                    } catch (SQLException e) {
                        location = "N/A";
                    }
                    int catId = rs.getInt("category_id");

                    Product p = new Product(name, qty, price, location);
                    p.setAlertThresholdOverride(alertThreshold);
                    Category parent = catMap.get(catId);

                    if (parent != null) {
                        parent.add(p);
                    }
                }
            }

        } catch (SQLException e) {
            // This usually happens if the DB file doesn't exist yet.
            System.out.println("Database not found or empty. Starting fresh.");
            return null;
        }

        return root;
    }
}
