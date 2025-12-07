/**
 * STRATEGY PATTERN: Concrete Strategy
 * Uses per-product override threshold when present; otherwise falls back to a
 * default.
 *
 * This allows specific items (like "Gold") to have different alert rules
 * than generic items (like "Paper"), while still using a single strategy class.
 */
public class PerProductThresholdStrategy implements StockAlertStrategy {
    private final int defaultThreshold;

    public PerProductThresholdStrategy(int defaultThreshold) {
        this.defaultThreshold = defaultThreshold;
    }

    @Override
    public String getName() {
        return "Per-product threshold (default <= " + defaultThreshold + ")";
    }

    @Override
    public String evaluate(Product product) {
        Integer override = product.getAlertThresholdOverride();
        int threshold = override != null ? override : defaultThreshold;
        if (product.getQuantity() <= threshold) {
            String suffix = override != null ? " (product override)" : " (default)";
            return product.getName() + " low stock " + product.getQuantity() + " <= " + threshold + suffix;
        }
        return null;
    }
}
