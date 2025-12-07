/* +
 * OBSERVER PATTERN: Observer Interface
 * This defines the contract for any class that needs to listen to inventory changes.
 *
 * - The 'InventoryManager' (Subject) holds a list of these observers.
 * - The 'InventoryUI' (Concrete Observer) implements this interface to update the screen.
 * This decouples the Data Layer from the Presentation Layer.
 */

public interface StockObserver {
    // Triggered automatically whenever a Product's state (quantity) changes.
    void update(Product product);
}
