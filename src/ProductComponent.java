/* +
 * This is the abstract 'Component' class in the Composite Design Pattern.
 * It defines a common interface for both 'Leaf' objects (Products)
 * and 'Composite' objects (Categories).
 *
 * This allows the client (InventoryManager) to treat individual products
 * and groups of products uniformly without checking their specific type.
 */

public abstract class ProductComponent { // super class for both category, and product
    protected String name; // Shared state for both Product and Category

    public ProductComponent(String name) { // contructor takes a string "name"
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty"); // error handling for empty name space
        }
        this.name = name.trim();
    }

    /*
     * Composite Management Methods:
     * These allow adding/removing children.
     * Categories will implement these logic.
     * Products (Leafs) will throw UnsupportedOperationException.
     */
    public abstract void add(ProductComponent component); // add child

    public abstract void remove(ProductComponent component); // remove child

    public abstract ProductComponent getChild(int i); // get child

    /*
     * Operation Methods:
     * These are the business logic methods.
     * For a Product, they return the specific value.
     * For a Category, they recursively calculate the total from children.
     */
    public abstract int getQuantity();

    public abstract double getPrice();

    public abstract void setQuantity(int qty);

    public abstract void print(String indent);

    public abstract void setName(String name);

    public abstract void setPrice(double price);

    public String getName() {
        return name;
    }
}
