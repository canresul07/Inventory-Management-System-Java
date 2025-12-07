/**
 * STRATEGY PATTERN: Concrete Strategy
 * Alerts when a product quantity falls below or equals a fixed threshold.
 * This is the simplest logic: "If any item is <= 5, warn me."
 */
public class FixedThresholdStrategy implements StockAlertStrategy {
    private final int threshold;

    public FixedThresholdStrategy(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public String getName() {
        return "Fixed threshold (<= " + threshold + ")";
    }

    @Override
    public String evaluate(Product product) {
        // Simple logic: Is the current quantity below the global hard limit?
        if (product.getQuantity() <= threshold) {
            return product.getName() + " is low on stock (qty " + product.getQuantity() + " <= " + threshold + ").";
        }
        return null;
    }
}
