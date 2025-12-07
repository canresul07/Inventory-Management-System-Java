/* +
 * This class acts as the Data Model for the JTable component.
 * It extends AbstractTableModel, which provides the standard methods
 * needed to display data in a Swing table.
 *
 * It acts as an ADAPTER: it translates a List<Product> (our data structure)
 * into a 2D grid of rows and columns (the table's data structure).
 */

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class ProductTableModel extends AbstractTableModel {
    // Defines the headers for the table columns
    private String[] columns = { "Name", "Quantity", "Price", "Location" };
    // The actual data source: a simple list of products
    private List<Product> products = new ArrayList<>();

    /*
     * Required Method: Tells the JTable how many rows to draw.
     * We simply return the size of our list.
     */
    @Override
    public int getRowCount() {
        return products.size();
    }

    /*
     * Required Method: Tells the JTable how many columns to draw.
     */
    @Override
    public int getColumnCount() {
        return columns.length;
    }

    /*
     * Optional Method: Provides the text for the column headers.
     * Without this, headers would be "A", "B", "C", etc.
     */
    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    /*
     * CORE LOGIC: MAPPING
     * This method is called by the JTable for every single cell it needs to draw.
     * It maps a (row, column) coordinate to a specific field in a Product object.
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Product p = products.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> p.getName();
            case 1 -> p.getQuantity();
            case 2 -> p.getPrice();
            case 3 -> p.getLocation();
            default -> null;
        };
    }

    /*
     * Updates the data displayed in the table.
     * * CRITICAL: We must call fireTableDataChanged() after updating the list.
     * This notifies the JTable UI that the data has changed and forces it to
     * repaint.
     */
    public void setProducts(List<Product> products) {
        this.products = products;
        fireTableDataChanged();
    }

    /*
     * Helper Method:
     * Allows retrieving the actual Product object corresponding to a specific row
     * index.
     * Useful when the user selects a row and clicks "Edit" or "Delete".
     */
    public Product getProductAt(int row) {
        return products.get(row);
    }
}
