import java.util.Random;

public class SampleDataLoader {
    /**
     * Populates the current inventory with sample categories and products.
     */
    public static void loadSampleData(InventoryManager manager) {
        Category root = manager.getRootCategory();
        root.getChildren().clear();

        Category electronics = new Category("Electronics");
        Category grocery = new Category("Grocery");
        Category hardware = new Category("Hardware");

        root.add(electronics);
        root.add(grocery);
        root.add(hardware);

        electronics.add(new Product("Laptop", 8, 1200, "A1"));
        electronics.add(new Product("Mouse", 25, 20, "A2"));
        electronics.add(new Product("Keyboard", 15, 45, "A2"));

        grocery.add(new Product("Milk", 12, 1.5, "B1"));
        grocery.add(new Product("Eggs", 6, 2.8, "B1"));
        grocery.add(new Product("Coffee", 20, 6.5, "B2"));

        hardware.add(new Product("Hammer", 9, 15, "C1"));
        hardware.add(new Product("Nails Pack", 50, 5, "C1"));
        hardware.add(new Product("Drill", 4, 90, "C2"));

        // Randomize per-product thresholds lightly
        Random rnd = new Random();
        for (ProductComponent pc : root.getChildren()) {
            if (pc instanceof Category cat) {
                for (ProductComponent leaf : cat.getChildren()) {
                    if (leaf instanceof Product p) {
                        if (rnd.nextBoolean()) {
                            p.setAlertThresholdOverride(rnd.nextInt(5) + 3); // 3-7
                        }
                    }
                }
            }
        }
    }
}
