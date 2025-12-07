/* +
 * This class represents the 'Leaf' in the Composite Design Pattern.
 * It is the end node of the tree structure and holds actual data (Quantity, Price).
 *
 * Unlike Categories, Products cannot have children, so methods like add/remove
 * throw an UnsupportedOperationException to enforce the pattern structure.
 */

public class Product extends ProductComponent {
    private int quantity;
    private double price;
    private String location; // Warehouse location
    private Integer alertThresholdOverride; // Optional: Custom alert limit for this specific product

    // Constructor chaining: Default location to "N/A" if not provided
    public Product(String name, int quantity, double price) {
        this(name, quantity, price, "N/A");
    }

    public Product(String name, int quantity, double price, String location) {
        super(name);
        setQuantityInternal(quantity);
        setPriceInternal(price);
        setLocation(location);
        this.alertThresholdOverride = null; // Default: No override (use global strategy)
    }

    /*
     * COMPOSITE PATTERN: Leaf Constraint
     * A Leaf (Product) cannot contain other components.
     * Calling add/remove/getChild throws an exception to indicate this logic error.
     */
    @Override
    public void add(ProductComponent component) {
        throw new UnsupportedOperationException("Cannot add to a leaf");
    }

    @Override
    public void remove(ProductComponent component) {
        throw new UnsupportedOperationException("Cannot remove from a leaf");
    }

    @Override
    public ProductComponent getChild(int i) {
        throw new UnsupportedOperationException("No child in leaf");
    }

    // --- Getters & Setters ---
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setPrice(double price) {
        setPriceInternal(price);
    }

    public void setLocation(String location) {
        this.location = (location == null || location.isBlank()) ? "N/A" : location.trim();
    }

    @Override
    public int getQuantity() {
        return quantity;
    }

    @Override
    public double getPrice() {
        return price;
    }

    public String getLocation() {
        return location;
    }

    public Integer getAlertThresholdOverride() {
        return alertThresholdOverride;
    }

    /*
     * OBSERVER PATTERN TRIGGER:
     * When quantity changes, this method triggers the Observer Pattern.
     * It notifies the InventoryManager, which in turn alerts the UI to refresh.
     */
    @Override
    public void setQuantity(int qty) {
        setQuantityInternal(qty);
        // Notify Observers that the state of this object has changed
        InventoryManager.getInstance().notifyObservers(this);
    }

    public void setAlertThresholdOverride(Integer threshold) {
        if (threshold != null && threshold < 0)
            throw new IllegalArgumentException("Threshold cannot be negative");
        this.alertThresholdOverride = threshold;
    }

    @Override
    public void print(String indent) {
        System.out.println(
                indent + "- " + name + " [Qty: " + quantity + ", Price: " + price + ", Loc: " + location + "]");
    }

    @Override
    public String toString() {
        return name + " (Qty: " + quantity + ", $" + price + ", Loc: " + location + ")";
    }

    // --- Internal Validation Helpers ---
    // Kept private to ensure validation logic is not bypassed
    private void setQuantityInternal(int qty) {
        if (qty < 0)
            throw new IllegalArgumentException("Quantity cannot be negative");
        if (qty > 1_000_000)
            throw new IllegalArgumentException("Quantity too large");
        this.quantity = qty;
    }

    private void setPriceInternal(double price) {
        if (price < 0)
            throw new IllegalArgumentException("Price cannot be negative");
        if (price > 1_000_000)
            throw new IllegalArgumentException("Price too large");
        this.price = price;
    }
}
