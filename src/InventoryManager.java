/* +
 * Inventory manager class uses singleton design pattern,
 * it uses singleton pattern to make sure there is only one inventory instance,
 * where users can manipulate,
 * without it program can be run on multiple inventory instances which will be break the whole system,
 * singleton will prevent this situation.

 * also alert alertStrategy connection will let us select different alert behaviours in runtime
 * we encapsulated different alert approaches, just gave them a common interface.
*/

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InventoryManager {
    private static InventoryManager instance; // InventoryManager instance reference

    private Category rootCategory; // root
    private List<StockObserver> observers; // InventoryManager acts as the Subject in Observer Pattern
    private StockAlertStrategy alertStrategy; // Holds a reference to the alertStrategy interface
    private String dbUrl = "jdbc:sqlite:inventory.db"; // database connection

    /*
     * constructor of InventoryManager,
     * its access modifier is private,
     * as required by the Singleton pattern to prevent outside instantiation.
     */
    private InventoryManager() {
        rootCategory = new Category("All Products"); // initialize root category
        observers = new ArrayList<>(); // initialize the list to hold observers

        // Initialize DB tables
        DatabaseHelper.initDB(dbUrl);

        // Try to load data
        rootCategory = DatabaseHelper.loadInventory(dbUrl);

        // If nothing in DB, create default Root
        if (rootCategory == null) {
            rootCategory = new Category("All Products");
        }

        // Default alert strategy
        alertStrategy = new FixedThresholdStrategy(5); // initially set "FixedThresholdStrategy"
    }

    /*
     * The public static synchronized getInstance method checks if an instance
     * has already been created.
     * If an instance exists, it returns it.
     * If no instance exists, it creates one and returns it.
     * It is static so it can be called globally without creating an object.
     * We use the 'synchronized' keyword to prevent threading bugs on multicore
     * architectures.
     */
    public static synchronized InventoryManager getInstance() {
        if (instance == null)
            instance = new InventoryManager();
        return instance;
    }

    public void saveToDatabase() { // Saves the current state of rootCategory to SQLite
        DatabaseHelper.saveInventory(rootCategory, dbUrl);
    }

    public void addObserver(StockObserver obs) { // addObserver method adds a class that implements observer interface ,
                                                 // to InventoryManagers observers arraylist
        observers.add(obs);
    }

    public void removeObserver(StockObserver obs) { // same logic as addObservers, now its just removes it
        observers.remove(obs);
    }

    public void notifyObservers(Product product) { // notify all observers located in the arraylist, order is not
                                                   // important in this case
        for (StockObserver obs : observers)
            obs.update(product);
    }

    // Setter method to change the alert behavior (Strategy) at runtime
    public void setAlertStrategy(StockAlertStrategy strategy) {
        this.alertStrategy = strategy;
    }

    // Getter method to return the current alert strategy
    public StockAlertStrategy getAlertStrategy() {
        return alertStrategy;
    }

    /*
     * This method executes the Strategy Pattern.
     * Instead of containing "if-else" logic for low stock here,
     * the InventoryManager delegates the decision to the 'alertStrategy'.
     *
     * We use 'Optional' to safely handle cases where there is no alert (null),
     * avoiding NullPointerExceptions in the UI.
     */
    public Optional<String> evaluateStock(Product product) {
        if (alertStrategy == null)
            return Optional.empty(); // Return empty if no strategy is set
        // Delegate the logic to the strategy and wrap the result
        return Optional.ofNullable(alertStrategy.evaluate(product));
    }

    // getter method for returning rootCategory
    public Category getRootCategory() {
        return rootCategory;
    }

    // For testing or alternate DB files
    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
        DatabaseHelper.initDB(dbUrl);
    }
}
