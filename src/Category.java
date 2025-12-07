/* +
 * This class represents the 'Composite' in the Composite Design Pattern.
 * It acts as a container (like a folder) that holds a list of children.
 *
 * It implements the shared ProductComponent interface, but instead of
 * returning a single value, it recursively aggregates values from its children.
 */

import java.util.ArrayList;
import java.util.List;

public class Category extends ProductComponent {
    // Holds both Products (Leaves) and other Categories (Composites)
    private List<ProductComponent> children = new ArrayList<>();

    public Category(String name) {
        super(name);
    }

    @Override
    // Composite Management: Adds a child component to this node
    public void add(ProductComponent component) { // adds given ProductComponent instance to children array
        children.add(component);
    }

    @Override
    // Composite Management: Removes a child component
    public void remove(ProductComponent component) { // removes given ProductComponent instance from children array
        children.remove(component);
    }

    @Override
    // Composite Management: Access a specific child
    public ProductComponent getChild(int i) { // returns the child located at given index
        return children.get(i);
    }

    // Allow renaming the category
    @Override
    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be empty"); // error handling
        }
        this.name = name.trim();
    }

    /*
     * Categories do not have their own price.
     * Their price is derived dynamically from the sum of their contents.
     * Therefore, setting a manual price is forbidden.
     */
    @Override
    public void setPrice(double price) {
        throw new UnsupportedOperationException("Cannot set price for category; it is calculated from children.");
    }

    public int getChildrenCount() {
        return children.size();
    }

    /*
     * Recursive Operation:
     * Iterates through all children and sums their quantities.
     * If a child is a Category, it will recursively call its own getQuantity().
     */
    @Override
    public int getQuantity() { // calculates the childrens total number and returns it
        int total = 0;
        for (ProductComponent c : children)
            total += c.getQuantity();
        return total;
    }

    /*
     * Recursive Operation:
     * Iterates through all children and sums their prices.
     */
    @Override
    public double getPrice() { // calculates the total price and returns it
        double total = 0;
        for (ProductComponent c : children)
            total += c.getPrice();
        return total;
    }

    // Categories cannot have a quantity directly; only their children do.
    @Override
    public void setQuantity(int qty) {
        throw new UnsupportedOperationException("Cannot set quantity for category");
    }

    // Recursively prints the tree structure (Indent increases with depth)
    @Override
    public void print(String indent) {
        System.out.println(indent + "+ " + name);
        for (ProductComponent c : children)
            c.print(indent + "  ");
    }

    @Override
    public String toString() {
        return name;
    }

    public List<ProductComponent> getChildren() {
        return children;
    }
}
