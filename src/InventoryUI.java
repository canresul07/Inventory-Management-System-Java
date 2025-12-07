/*
 * This class is the Presentation Layer (UI) of the application.
 * It connects the User to the Backend Logic (InventoryManager).
 *
 * Design Patterns Used Here:
 * 1. OBSERVER PATTERN: This class implements 'StockObserver'. It registers itself
 * with the InventoryManager to receive automatic updates when data changes.
 * 2. COMPOSITE PATTERN: It visualizes the recursive tree structure using 'buildTreeNode'.
 * 3. STRATEGY PATTERN: It provides a UI (dropdown) to switch alert algorithms at runtime.
 */

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.swing.table.TableRowSorter;
import java.io.FileWriter;
import java.io.IOException;

public class InventoryUI implements StockObserver {
    private JFrame frame;
    private JTree categoryTree;
    private DefaultTreeModel treeModel;
    private JTable productTable;
    private ProductTableModel tableModel;
    private TableRowSorter<ProductTableModel> rowSorter;
    private JButton addCategoryBtn, addProductBtn, editCategoryBtn, editProductBtn, deleteProductBtn, deleteCategoryBtn,
            copyProductBtn;
    private JComboBox<String> alertSelector;
    private JTextField searchField;
    private JComboBox<String> roleSelector;
    private InventoryManager manager;
    private JPanel detailsPanel;
    private JLabel detailsImage;
    private JLabel detailsName;
    private JLabel detailsQty;
    private JLabel detailsPrice;
    private JLabel detailsLocation;
    private JLabel detailsThreshold;

    private Map<String, StockAlertStrategy> alertStrategies;

    private void buildAlertStrategies() {
        alertStrategies = new HashMap<>();
        alertStrategies.put("Fixed (<=5)", new FixedThresholdStrategy(5));
        alertStrategies.put("Fixed (<=10)", new FixedThresholdStrategy(10));
        alertStrategies.put("Reorder (10/3)", new ReorderPointStrategy(10, 3));
        alertStrategies.put("Per-product (default 5)", new PerProductThresholdStrategy(5));
    }

    // constructor:
    public InventoryUI() {
        manager = InventoryManager.getInstance(); // get the inventory with .getInstance() static method
        manager.addObserver(this); // then add InventoryIU class as a observer to InventoryManager Subject class
        buildAlertStrategies();
        createUI();
    }

    private void createUI() {
        frame = new JFrame("Inventory Management System");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(1400, 720);
        frame.setMinimumSize(new Dimension(1400, 720));
        frame.setLayout(new BorderLayout(10, 10));

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Save to SQLite
                manager.saveToDatabase();

                frame.dispose();
                System.exit(0);
            }
        });

        // Create Top Menu Bar
        JMenuBar menuBar = new JMenuBar();

        // [File Menu] - For Exports and Exit
        JMenu fileMenu = new JMenu("File");
        JMenuItem exportCsvItem = new JMenuItem("Export to CSV");
        exportCsvItem.addActionListener(e -> exportCurrentTableToCsv());
        JMenuItem exportSumItem = new JMenuItem("Export Summary");
        exportSumItem.addActionListener(e -> exportSummary());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> {
            manager.saveToDatabase();
            System.exit(0);
        });

        fileMenu.add(exportCsvItem);
        fileMenu.add(exportSumItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // [Tools Menu] - For Sample Data
        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem loadSampleItem = new JMenuItem("Load Sample Data");
        loadSampleItem.addActionListener(e -> {
            SampleDataLoader.loadSampleData(manager);
            refreshTree();
            refreshTable();
            JOptionPane.showMessageDialog(frame, "Sample data loaded!");
        });
        toolsMenu.add(loadSampleItem);

        // Add Menus to Bar
        menuBar.add(fileMenu);
        menuBar.add(toolsMenu);

        // Attach to Frame
        frame.setJMenuBar(menuBar);

        // --- 2. Toolbar (WinRAR Style - FIXED) ---
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.setBorder(new EmptyBorder(5, 5, 5, 5)); // Slightly smaller border

        // B. Main Action Buttons
        addCategoryBtn = makeButton("Add Category", "folder_add.png", "addCat");
        editCategoryBtn = makeButton("Edit Category", "folder_edit.png", "editCat");
        deleteCategoryBtn = makeButton("Delete Category", "folder_delete.png", "delCat");

        addProductBtn = makeButton("Add Product", "box_add.png", "addProd");
        copyProductBtn = makeButton("Copy Product", "box_copy.png", "copyProd");
        editProductBtn = makeButton("Edit Product", "box_edit.png", "editProd");
        deleteProductBtn = makeButton("Delete Product", "box_delete.png", "delProd");

        toolBar.add(addCategoryBtn);
        toolBar.add(editCategoryBtn);
        toolBar.add(deleteCategoryBtn);

        toolBar.addSeparator(new Dimension(20, 48)); // Vertical Divider

        toolBar.add(addProductBtn);
        toolBar.add(copyProductBtn);
        toolBar.add(editProductBtn);
        toolBar.add(deleteProductBtn);

        // RIGHT SIDE CONTROLS

        // 1. Search
        JLabel searchLabel = new JLabel("Search: ");
        searchField = new JTextField(10);
        fixComponentSize(searchField, 100, 25);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applyFilter();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applyFilter();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applyFilter();
            }
        });

        toolBar.add(searchLabel);
        toolBar.add(searchField);
        toolBar.add(Box.createHorizontalStrut(15));

        // 2. Alert Policy
        JLabel alertLabel = new JLabel("Alerts: ");
        alertSelector = new JComboBox<>(alertStrategies.keySet().toArray(new String[0]));
        fixComponentSize(alertSelector, 120, 25); // Prevent stretching

        alertSelector.addActionListener(e -> switchAlertStrategy((String) alertSelector.getSelectedItem()));

        toolBar.add(alertLabel);
        toolBar.add(alertSelector);
        toolBar.add(Box.createHorizontalStrut(15));

        // 4. Role & Sample
        JLabel roleLabel = new JLabel("Role: ");
        roleSelector = new JComboBox<>(new String[] { "Manager", "Viewer" });
        fixComponentSize(roleSelector, 90, 25);
        roleSelector.addActionListener(e -> applyRolePermissions());

        toolBar.add(roleLabel);
        toolBar.add(roleSelector);
        toolBar.add(Box.createHorizontalStrut(10));
        // toolBar.add(loadSampleBtn);

        frame.add(toolBar, BorderLayout.NORTH);

        // --- 2. Left Tree (Styled) ---
        DefaultMutableTreeNode rootNode = buildTreeNode(manager.getRootCategory());
        treeModel = new DefaultTreeModel(rootNode);
        categoryTree = new JTree(treeModel);
        // Increase row height for better readability
        categoryTree.setRowHeight(25);
        JScrollPane treeScroll = new JScrollPane(categoryTree);
        treeScroll.setPreferredSize(new Dimension(300, 600));
        // Add a border to the scroll pane for visual separation
        treeScroll.setBorder(BorderFactory.createTitledBorder("Categories"));
        frame.add(treeScroll, BorderLayout.WEST);

        // --- CENTER: Table ---
        tableModel = new ProductTableModel();
        productTable = new JTable(tableModel);
        productTable.setRowHeight(25);
        productTable.setShowGrid(true);
        productTable.setGridColor(Color.LIGHT_GRAY);
        rowSorter = new TableRowSorter<>(tableModel);
        productTable.setRowSorter(rowSorter);

        // IMPORTANT: Set Selection Mode to Single Selection
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane tableScroll = new JScrollPane(productTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Product List"));
        frame.add(tableScroll, BorderLayout.CENTER);

        // --- EAST: Details Panel (NEW) ---
        detailsPanel = createDetailsPanel();
        frame.add(detailsPanel, BorderLayout.EAST);

        // --- LISTENER: Update Details on Click (NEW) ---
        productTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { // Prevent double events
                updateDetailsPanel();
            }
        });

        // --- Actions ---
        addCategoryBtn.addActionListener(e -> addCategory());
        editCategoryBtn.addActionListener(e -> editCategory());
        deleteCategoryBtn.addActionListener(e -> deleteCategory());

        addProductBtn.addActionListener(e -> addProduct());
        copyProductBtn.addActionListener(e -> copyProduct());
        editProductBtn.addActionListener(e -> editProduct());
        deleteProductBtn.addActionListener(e -> deleteProduct());
        categoryTree.addTreeSelectionListener(e -> refreshTable());

        frame.setVisible(true);
        refreshTable();
    }

    // --- Tree helpers ---

    private JButton makeButton(String text, String iconName, String actionCommand) {
        JButton btn = new JButton(text);

        // DIRECT PATH: Just tell Java where the file is relative to your project folder
        // No 'new File()', no 'URL', no 'try-catch' needed.
        String path = "icons/" + iconName;

        // ImageIcon automatically checks the file system for this path
        ImageIcon icon = new ImageIcon(path);

        // Check if the icon actually loaded (status 8 means complete/loaded)
        if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
            Image img = icon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
            btn.setIcon(new ImageIcon(img));
        } else {
            System.err.println("Icon not found at path: " + path);
        }

        // WinRAR Style Settings
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setMargin(new Insets(2, 2, 2, 2));
        btn.setFocusPainted(false);

        return btn;
    }

    // --- STRATEGY PATTERN LOGIC ---

    /*
     * Switches the active Alert Strategy at runtime.
     * CRITICAL: Immediately re-scans the inventory (checkAllAlerts) to see
     * if any products violate the NEW rule.
     */
    // 1. The Method Called by the Dropdown Listener
    private void switchAlertStrategy(String key) {
        StockAlertStrategy strategy = alertStrategies.get(key);
        if (strategy != null) {
            manager.setAlertStrategy(strategy);
            // Immediately check all products against the new rule
            checkAllAlerts();
        }
    }

    // 2. Scans the entire inventory tree and shows a SUMMARY popup
    private void checkAllAlerts() {
        StringBuilder alertReport = new StringBuilder();

        // Pass the StringBuilder to the recursive method to collect all warnings
        checkAlertsRecursive(manager.getRootCategory(), alertReport);

        // If we found any alerts, show them all in ONE single dialog
        if (alertReport.length() > 0) {
            JTextArea textArea = new JTextArea(alertReport.toString());
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);

            // Wrap in scroll pane in case the list is huge
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(400, 300));

            JOptionPane.showMessageDialog(frame, scrollPane,
                    "Stock Alert Report", JOptionPane.WARNING_MESSAGE);
        }
    }

    // 3. Recursive helper that collects messages
    private void checkAlertsRecursive(ProductComponent comp, StringBuilder report) {
        if (comp instanceof Product) {
            // Ask Manager: Does this product violate the CURRENT strategy?
            Optional<String> alert = manager.evaluateStock((Product) comp);

            // If yes, append to our report list (with a newline)
            alert.ifPresent(msg -> report.append(msg).append("\n"));

        } else if (comp instanceof Category) {
            // If it's a category, dive deeper into its children
            for (ProductComponent child : ((Category) comp).getChildren()) {
                checkAlertsRecursive(child, report);
            }
        }
    }

    private void applyFilter() {
        if (rowSorter == null)
            return;
        String text = searchField.getText();
        if (text == null || text.isBlank()) {
            rowSorter.setRowFilter(null);
        } else {
            rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
        }
    }

    private void applyRolePermissions() {
        boolean managerMode = roleSelector.getSelectedItem().equals("Manager");
        addCategoryBtn.setEnabled(managerMode);
        editCategoryBtn.setEnabled(managerMode);
        deleteCategoryBtn.setEnabled(managerMode);
        addProductBtn.setEnabled(managerMode);
        copyProductBtn.setEnabled(managerMode);
        editProductBtn.setEnabled(managerMode);
        deleteProductBtn.setEnabled(managerMode);
    }

    private void exportCurrentTableToCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export current view to CSV");
        int option = chooser.showSaveDialog(frame);
        if (option != JFileChooser.APPROVE_OPTION)
            return;

        try (FileWriter writer = new FileWriter(chooser.getSelectedFile())) {
            // Header
            writer.write("Name,Quantity,Price,Location\n");
            for (int viewRow = 0; viewRow < productTable.getRowCount(); viewRow++) {
                int modelRow = productTable.convertRowIndexToModel(viewRow);
                Product p = tableModel.getProductAt(modelRow);
                writer.write(String.format("\"%s\",%d,%.2f,\"%s\"%n",
                        p.getName().replace("\"", "\"\""),
                        p.getQuantity(),
                        p.getPrice(),
                        p.getLocation().replace("\"", "\"\"")));
            }
            JOptionPane.showMessageDialog(frame, "Exported " + productTable.getRowCount() + " rows.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Export failed: " + ex.getMessage());
        }
    }

    private void exportSummary() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export summary to text");
        int option = chooser.showSaveDialog(frame);
        if (option != JFileChooser.APPROVE_OPTION)
            return;

        try (FileWriter writer = new FileWriter(chooser.getSelectedFile())) {
            writer.write("Category\tTotal Qty\tTotal Value\n");
            Category root = manager.getRootCategory();
            for (ProductComponent pc : root.getChildren()) {
                if (pc instanceof Category cat) {
                    int qty = cat.getQuantity();
                    double value = cat.getPrice();
                    writer.write(String.format("%s\t%d\t%.2f%n", cat.getName(), qty, value));
                }
            }
            JOptionPane.showMessageDialog(frame, "Summary exported.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Export failed: " + ex.getMessage());
        }
    }

    private DefaultMutableTreeNode buildTreeNode(ProductComponent comp) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(comp);
        if (comp instanceof Category) {
            for (ProductComponent child : ((Category) comp).getChildren()) {
                node.add(buildTreeNode(child));
            }
        }
        return node;
    }

    private void refreshTree() {
        // 1. Save the currently expanded Categories
        java.util.List<Category> expandedCategories = new java.util.ArrayList<>();

        // Get the root path
        TreePath rootPath = new TreePath(treeModel.getRoot());

        // Get an enumeration of all expanded paths
        java.util.Enumeration<TreePath> expanded = categoryTree.getExpandedDescendants(rootPath);

        if (expanded != null) {
            while (expanded.hasMoreElements()) {
                TreePath path = expanded.nextElement();
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObj = node.getUserObject();
                // If a category is expanded, remember it
                if (userObj instanceof Category) {
                    expandedCategories.add((Category) userObj);
                }
            }
        }

        // 2. Rebuild the tree (standard reload)
        DefaultMutableTreeNode rootNode = buildTreeNode(manager.getRootCategory());
        treeModel.setRoot(rootNode);
        treeModel.reload();

        // 3. Restore the expansion state
        restoreExpansion(rootNode, expandedCategories);
    }

    private Category getSelectedCategory() {
        TreePath path = categoryTree.getSelectionPath();
        if (path == null)
            return manager.getRootCategory();

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObj = node.getUserObject();

        // Case 1: User selected a Category
        if (userObj instanceof Category) {
            return (Category) userObj;
        }
        // Case 2: User selected a Product -> Return its Parent Category
        else if (userObj instanceof Product) {
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
            if (parentNode != null && parentNode.getUserObject() instanceof Category) {
                return (Category) parentNode.getUserObject();
            }
        }

        return null;
    }

    // --- Table ---
    private void refreshTable() {
        Category cat = getSelectedCategory();
        if (cat == null)
            return;

        tableModel.setProducts(cat.getChildren().stream()
                .filter(c -> c instanceof Product) // <--- LOOK HERE
                .map(c -> (Product) c)
                .toList());
        applyFilter();
    }

    /*
     * OBSERVER PATTERN CALLBACK:
     * This method is called automatically by the InventoryManager whenever
     * a product's state changes.
     */
    @Override
    public void update(Product product) {
        refreshTable();
        Optional<String> alert = manager.evaluateStock(product);
        alert.ifPresent(msg -> JOptionPane.showMessageDialog(frame, msg, "Stock Alert", JOptionPane.WARNING_MESSAGE));
    }

    private void restoreExpansion(DefaultMutableTreeNode node, java.util.List<Category> expandedCategories) {
        // If this node represents a Category that was previously open, expand it
        if (node.getUserObject() instanceof Category) {
            Category cat = (Category) node.getUserObject();
            if (expandedCategories.contains(cat)) {
                categoryTree.expandPath(new TreePath(node.getPath()));
            }
        }

        // Recursively check all children
        for (int i = 0; i < node.getChildCount(); i++) {
            restoreExpansion((DefaultMutableTreeNode) node.getChildAt(i), expandedCategories);
        }
    }

    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20)); // Padding
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(250, 0)); // Fixed width of 250px

        // 1. Temporary Image Placeholder
        // Creating a simple gray box as a placeholder image
        BufferedImage img = new BufferedImage(200, 150, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(0, 0, 200, 150);
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawString("No Image", 70, 75); // Text in center
        g2d.dispose();

        detailsImage = new JLabel(new ImageIcon(img));
        detailsImage.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 2. Property Labels
        detailsName = new JLabel("Name: -");
        detailsName.setFont(new Font("Arial", Font.BOLD, 18));
        detailsName.setAlignmentX(Component.CENTER_ALIGNMENT);

        detailsQty = new JLabel("Quantity: -");
        detailsQty.setFont(new Font("Arial", Font.PLAIN, 14));
        detailsQty.setAlignmentX(Component.CENTER_ALIGNMENT);

        detailsPrice = new JLabel("Price: -");
        detailsPrice.setFont(new Font("Arial", Font.PLAIN, 14));
        detailsPrice.setAlignmentX(Component.CENTER_ALIGNMENT);

        detailsLocation = new JLabel("Location: -");
        detailsLocation.setFont(new Font("Arial", Font.PLAIN, 14));
        detailsLocation.setAlignmentX(Component.CENTER_ALIGNMENT);

        detailsThreshold = new JLabel("Alert threshold: -");
        detailsThreshold.setFont(new Font("Arial", Font.PLAIN, 14));
        detailsThreshold.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 3. Add spacing and components
        panel.add(detailsImage);
        panel.add(Box.createVerticalStrut(20)); // Spacing
        panel.add(detailsName);
        panel.add(Box.createVerticalStrut(10));
        panel.add(detailsQty);
        panel.add(Box.createVerticalStrut(5));
        panel.add(detailsPrice);
        panel.add(Box.createVerticalStrut(5));
        panel.add(detailsLocation);
        panel.add(Box.createVerticalStrut(5));
        panel.add(detailsThreshold);

        // Push everything up
        panel.add(Box.createVerticalGlue());

        // Start hidden until a selection is made
        panel.setVisible(false);
        return panel;
    }

    private void updateDetailsPanel() {
        int viewRow = productTable.getSelectedRow();

        if (viewRow >= 0) {
            // --- SAFE FIX: Handle filtered table rows correctly ---
            int modelRow = productTable.convertRowIndexToModel(viewRow);
            Product p = tableModel.getProductAt(modelRow);
            // -----------------------------------------------------

            detailsName.setText(p.getName());
            detailsQty.setText("Quantity: " + p.getQuantity());
            detailsPrice.setText(String.format("Price: $%.2f", p.getPrice()));
            detailsLocation.setText("Location: " + p.getLocation());

            String thresholdInfo = (p.getAlertThresholdOverride() == null) ? "default"
                    : String.valueOf(p.getAlertThresholdOverride());
            detailsThreshold.setText("Alert threshold: " + thresholdInfo);

            detailsPanel.setVisible(true);
            detailsPanel.revalidate();
            detailsPanel.repaint();
        } else {
            // Nothing selected -> Hide Panel
            detailsPanel.setVisible(false);
        }
    }

    // Helper to stop Toolbar items from stretching vertically
    private void fixComponentSize(JComponent comp, int width, int height) {
        Dimension size = new Dimension(width, height);
        comp.setMaximumSize(size);
        comp.setPreferredSize(size);
        comp.setMinimumSize(size);
        comp.setAlignmentY(Component.CENTER_ALIGNMENT); // Keeps it centered vertically
    }

    // ----------------------------------------CATEGORY-----------------------------
    private void addCategory() {
        String name = JOptionPane.showInputDialog(frame, "Category Name:");
        if (name == null)
            return;
        if (name.isBlank()) {
            JOptionPane.showMessageDialog(frame, "Category name cannot be empty.");
            return;
        }

        Category newCat = new Category(name);
        Category parent = getSelectedCategory();
        if (parent == null)
            parent = manager.getRootCategory();
        parent.add(newCat);
        refreshTree();
    }

    private void editCategory() {
        TreePath path = categoryTree.getSelectionPath();
        if (path == null)
            return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObj = node.getUserObject();

        if (!(userObj instanceof Category)) {
            JOptionPane.showMessageDialog(frame, "Selected item is a Product.\nPlease use 'Edit Product'.");
            return;
        }

        Category cat = (Category) userObj;

        if (node.getParent() == null) {
            JOptionPane.showMessageDialog(frame, "Cannot rename the Root category.");
            return;
        }

        String newName = JOptionPane.showInputDialog(frame, "Rename Category:", cat.getName());
        if (newName != null && !newName.trim().isEmpty()) {
            cat.setName(newName);
            refreshTree();
        } else {
            JOptionPane.showMessageDialog(frame, "Category name cannot be empty.");
        }
    }

    private void deleteCategory() {
        TreePath path = categoryTree.getSelectionPath();
        if (path == null) {
            JOptionPane.showMessageDialog(frame, "Please select a category to delete.");
            return;
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObj = node.getUserObject();

        if (!(userObj instanceof Category)) {
            JOptionPane.showMessageDialog(frame,
                    "Selected item is a Product.\nPlease use the 'Delete Product' button.");
            return;
        }

        if (node.getParent() == null) {
            JOptionPane.showMessageDialog(frame, "You cannot delete the Root category.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(frame,
                "Are you sure you want to delete category '" + userObj + "' and all its contents?",
                "Delete Category",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            Category catToDelete = (Category) userObj;
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
            Category parentCat = (Category) parentNode.getUserObject();

            parentCat.remove(catToDelete);

            // Reset selection to parent so the tree doesn't break
            categoryTree.setSelectionPath(new TreePath(parentNode.getPath()));
            refreshTree();
            refreshTable();
        }
    }

    // ----------------------------------------PRODUCT-----------------------------

    private void addProduct() {
        Category cat = getSelectedCategory();
        if (cat == null)
            return;

        JTextField nameField = new JTextField();
        JTextField qtyField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField locationField = new JTextField();
        JTextField thresholdField = new JTextField();
        Object[] fields = {
                "Product Name:", nameField,
                "Quantity:", qtyField,
                "Price:", priceField,
                "Location (aisle/shelf):", locationField,
                "Alert threshold override (optional):", thresholdField
        };
        int option = JOptionPane.showConfirmDialog(frame, fields, "Add Product", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText();
                int qty = Integer.parseInt(qtyField.getText());
                double price = Double.parseDouble(priceField.getText());
                String location = locationField.getText();
                if (name == null || name.isBlank()) {
                    throw new IllegalArgumentException("Name is required");
                }
                Product p = new Product(name, qty, price, location);
                if (!thresholdField.getText().isBlank()) {
                    p.setAlertThresholdOverride(Integer.parseInt(thresholdField.getText()));
                }
                cat.add(p);
                refreshTree();
                refreshTable();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Invalid number input.");
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage());
            }
        }
    }

    private void copyProduct() {
        Product productToCopy = null;
        Category parentCategory = null;

        int viewRow = productTable.getSelectedRow();
        if (viewRow >= 0) {
            int modelRow = productTable.convertRowIndexToModel(viewRow);
            productToCopy = tableModel.getProductAt(modelRow);
            parentCategory = getSelectedCategory(); // Helper gets the category of the list
        }

        else {
            TreePath path = categoryTree.getSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObj = node.getUserObject();

                if (userObj instanceof Product) {
                    productToCopy = (Product) userObj;
                    // Find parent category
                    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
                    if (parentNode != null && parentNode.getUserObject() instanceof Category) {
                        parentCategory = (Category) parentNode.getUserObject();
                    }
                }
            }
        }

        if (productToCopy != null && parentCategory != null) {
            Product clone = new Product(
                    productToCopy.getName() + " - Copy",
                    productToCopy.getQuantity(),
                    productToCopy.getPrice(),
                    productToCopy.getLocation());

            clone.setAlertThresholdOverride(productToCopy.getAlertThresholdOverride());

            parentCategory.add(clone);

            // Refresh UI
            refreshTree();
            refreshTable();
            JOptionPane.showMessageDialog(frame, "Product copied successfully!");
        } else {
            JOptionPane.showMessageDialog(frame, "Please select a product to copy (from Table or Tree).");
        }
    }

    private void editProduct() {
        Product productToEdit = null;

        int viewRow = productTable.getSelectedRow();
        if (viewRow >= 0) {
            int modelRow = productTable.convertRowIndexToModel(viewRow);
            productToEdit = tableModel.getProductAt(modelRow);
        } else {
            TreePath path = categoryTree.getSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObj = node.getUserObject();
                if (userObj instanceof Product) {
                    productToEdit = (Product) userObj;
                }
            }
        }

        if (productToEdit == null) {
            JOptionPane.showMessageDialog(frame, "Please select a product to edit (from Table or Tree).");
            return;
        }

        JTextField nameField = new JTextField(productToEdit.getName());
        JTextField qtyField = new JTextField("" + productToEdit.getQuantity());
        JTextField priceField = new JTextField("" + productToEdit.getPrice());
        JTextField locationField = new JTextField(productToEdit.getLocation());
        JTextField thresholdField = new JTextField(productToEdit.getAlertThresholdOverride() == null ? ""
                : "" + productToEdit.getAlertThresholdOverride());

        Object[] fields = {
                "Product Name:", nameField,
                "Quantity:", qtyField,
                "Price:", priceField,
                "Location (aisle/shelf):", locationField,
                "Alert threshold override (optional):", thresholdField
        };

        int option = JOptionPane.showConfirmDialog(frame, fields, "Edit Product", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            try {
                if (nameField.getText() == null || nameField.getText().isBlank()) {
                    throw new IllegalArgumentException("Name is required");
                }
                productToEdit.setName(nameField.getText());
                productToEdit.setQuantity(Integer.parseInt(qtyField.getText()));
                productToEdit.setPrice(Double.parseDouble(priceField.getText()));
                productToEdit.setLocation(locationField.getText());
                if (thresholdField.getText().isBlank()) {
                    productToEdit.setAlertThresholdOverride(null);
                } else {
                    productToEdit.setAlertThresholdOverride(Integer.parseInt(thresholdField.getText()));
                }

                refreshTree();
                refreshTable();
                updateDetailsPanel(); // Refresh side panel too
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Invalid number input.");
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error updating product: " + ex.getMessage());
            }
        }
    }

    private void deleteProduct() {
        Product productToDelete = null;
        Category parentCategory = null;

        int viewRow = productTable.getSelectedRow();
        if (viewRow >= 0) {
            int modelRow = productTable.convertRowIndexToModel(viewRow);
            productToDelete = tableModel.getProductAt(modelRow);
            parentCategory = getSelectedCategory();
        } else {
            TreePath path = categoryTree.getSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObj = node.getUserObject();

                if (userObj instanceof Product) {
                    productToDelete = (Product) userObj;
                    // Find the parent category of this tree node
                    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
                    if (parentNode != null && parentNode.getUserObject() instanceof Category) {
                        parentCategory = (Category) parentNode.getUserObject();
                    }
                }
            }
        }

        if (productToDelete != null && parentCategory != null) {
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "Delete product '" + productToDelete.getName() + "'?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                parentCategory.remove(productToDelete);
                refreshTree();
                refreshTable();
                detailsPanel.setVisible(false);
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Please select a product to delete (from Table or Tree).");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(InventoryUI::new);
    }
}
