/**
 * STRATEGY PATTERN: Concrete Strategy
 * Two-level alert: warns at reorder point, escalates at safety stock.
 *
 * This demonstrates complex business logic encapsulated in a strategy.
 * It prioritizes "Safety Stock" (Emergency) over "Reorder Point" (Warning).
 */
public class ReorderPointStrategy implements StockAlertStrategy {
    private final int reorderPoint;
    private final int safetyStock;

    public ReorderPointStrategy(int reorderPoint, int safetyStock) {
        this.reorderPoint = reorderPoint;
        this.safetyStock = safetyStock;
    }

    @Override
    public String getName() {
        return "Reorder point (R:" + reorderPoint + ", S:" + safetyStock + ")";
    }

    @Override
    public String evaluate(Product product) {
        int qty = product.getQuantity();
        if (qty <= safetyStock) {
            return product.getName() + " below safety stock (" + qty + " <= " + safetyStock
                    + "). Immediate action required.";
        }
        if (qty <= reorderPoint) {
            return product.getName() + " reached reorder point (" + qty + " <= " + reorderPoint
                    + "). Plan replenishment.";
        }
        return null;
    }
}
