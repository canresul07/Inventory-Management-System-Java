# Inventory Management System (Design Patterns Project)

Option 8 implementation: composite category tree, observer-driven UI updates, configurable stock alert strategies, warehouse location tracking, and SQLite persistence.

## Features
- Category/Product tree using Composite; nested categories roll up totals automatically.
- Stock alerts via Strategy (fixed threshold or reorder-point), surfaced through Observer callbacks in the UI.
- Warehouse layout support with per-product location (aisle/shelf) visible in table and detail pane.
- SQLite persistence (`inventory.db`) with auto schema creation and safe column migration for location.
- Swing UI with toolbar actions, category tree, product table, and detail panel.
- Search/filter in table and CSV export of the current view.
- Summary export (category totals) and sample data loader for quick demo.
- Role toggle (Manager/Viewer) to lock editing in demo scenarios.

## Design Patterns
- Composite: `ProductComponent`, `Category`, `Product` for hierarchical inventory.
- Observer: `InventoryManager` + `StockObserver` notify UI on stock changes.
- Strategy: `StockAlertStrategy` with `FixedThresholdStrategy` and `ReorderPointStrategy` selectable from the toolbar.
- Singleton (supporting): `InventoryManager` centralizes state and DB persistence.

## Run Locally
1) Requirements: Java 17+, SQLite JDBC (already in `lib/sqlite-jdbc-3.51.0.0.jar`).
2) Compile:
   - macOS/Linux: `javac -cp "lib/sqlite-jdbc-3.51.0.0.jar" -d bin src/*.java`
   - Windows (PowerShell): `javac -cp "lib/sqlite-jdbc-3.51.0.0.jar" -d bin src\\*.java`
3) Run:
   - macOS/Linux: `java -cp "bin:lib/sqlite-jdbc-3.51.0.0.jar" App`
   - Windows: `java -cp "bin;lib\\sqlite-jdbc-3.51.0.0.jar" App`

Notes:
- Data is stored in `inventory.db` beside the project root.
- The default alert strategy is fixed threshold (<=5); switch strategies from the toolbar.
- See `docs/design.md` for UML and detailed design decisions.
- Smoke tests: `java -cp "bin:lib/sqlite-jdbc-3.51.0.0.jar" InventorySmokeTest`
- Deliverables reminder: source code + design docs + UML + YouTube demo link (10 dk). Add group members before submission.
- Troubleshooting: ensure `lib/sqlite-jdbc-3.51.0.0.jar` is on classpath; delete `inventory.db` if schema mismatch occurs (or use fresh test DB URL in code).

## Quick User Flows
- Add/edit/delete categories/products via toolbar (Manager mode); Viewer mode disables edits.
- Set per-product alert threshold in add/edit dialogs; choose alert strategy from toolbar.
- Search/filter table by text; export current view to CSV; export category totals via "Export Summary".
- Load sample data via toolbar button for quick demo; save on exit persists to SQLite.
