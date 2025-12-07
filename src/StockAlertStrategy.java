/* +
 * STRATEGY PATTERN: Strategy Interface
 * This defines the common interface for all stock alert algorithms.
 * By using this interface, the InventoryManager (Context) can swap between
 * different rules (Fixed, Reorder, Per-Product) at runtime without knowing
 * the implementation details.
 */

public interface StockAlertStrategy {
    /**
     * Human readable name for UI selection.
     * Used by the JComboBox in the Toolbar to display options to the user.
     */
    String getName();

    /**
     * Returns an alert message if the product requires attention; otherwise null.
     * This encapsulates the "Business Logic" for what constitutes low stock.
     */
    String evaluate(Product product);
}
