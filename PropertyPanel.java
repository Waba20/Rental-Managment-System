import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.List;

/**
 * PropertyPanel — luxury navy/gold design matching the HTML reference.
 *
 * FIXED: All buttons (Add, Edit, Delete) now call PropertyDAO to persist
 * changes to the SQL Server database via DBConnector.
 * The table is loaded from the DB on construction and refreshed after
 * every insert / update / delete.
 */
public class PropertyPanel extends JPanel {

    private JTable table;
    private DefaultTableModel model;

    // DAO used for all DB operations
    private final PropertyDAO dao = new PropertyDAO();

    public PropertyPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(RentalTheme.CREAM);

        // ── Top bar ──────────────────────────────────────────────
        JButton btnAdd = RentalTheme.primaryButton("＋  Add Property");
        btnAdd.addActionListener(e -> showPropertyDialog(null));

        // ── Filter bar ───────────────────────────────────────────
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterBar.setBackground(RentalTheme.WHITE);
        filterBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, RentalTheme.BORDER));

        filterBar.add(filterLabel("Type:"));
        filterBar.add(RentalTheme.styledCombo(new String[]{"All Types", "House", "Townhouse", "Flat/Apartment"}));
        filterBar.add(Box.createHorizontalStrut(6));
        filterBar.add(filterLabel("Status:"));
        filterBar.add(RentalTheme.styledCombo(new String[]{"All Status", "Available", "Occupied"}));
        filterBar.add(Box.createHorizontalStrut(6));
        filterBar.add(filterLabel("Location:"));
        filterBar.add(RentalTheme.styledCombo(new String[]{"All Locations", "Klein Windhoek", "Katutura", "Eros", "Olympia", "Ludwigsdorf"}));

        JTextField searchField = RentalTheme.styledField("Search properties...");
        searchField.setPreferredSize(new Dimension(180, 34));
        filterBar.add(Box.createHorizontalStrut(10));
        filterBar.add(searchField);

        // Wrap north components
        JPanel northWrap = new JPanel(new BorderLayout());
        northWrap.setBackground(RentalTheme.WHITE);
        northWrap.add(DashboardPanel.buildTopbar("Properties", new JComponent[]{btnAdd}), BorderLayout.NORTH);
        northWrap.add(filterBar, BorderLayout.SOUTH);
        add(northWrap, BorderLayout.NORTH);

        // ── Table ────────────────────────────────────────────────
        String[] cols = {"ID", "Type", "Address", "Location", "Rent / Month", "Market Value", "Status", "Actions"};
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return c == 7; }
        };
        table = new JTable(model);
        RentalTheme.styleTable(table);

        table.getColumnModel().getColumn(6).setCellRenderer(new RentalTheme.StatusRenderer());
        table.getColumnModel().getColumn(7).setCellRenderer(new ActionButtonRenderer());
        table.getColumnModel().getColumn(7).setCellEditor(new ActionButtonEditor(new JCheckBox()));
        table.getColumnModel().getColumn(7).setPreferredWidth(130);

        int[] widths = {50, 110, 160, 120, 120, 120, 90, 130};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.setRowHeight(36);

        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setBackground(RentalTheme.WHITE);
        tableWrapper.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));
        tableWrapper.add(RentalTheme.styledScroll(table), BorderLayout.CENTER);
        tableWrapper.add(buildSummaryBar(), BorderLayout.NORTH);
        add(tableWrapper, BorderLayout.CENTER);

        // ── Load real data from DB ────────────────────────────────
        loadTableFromDB();
    }

    // ── Load / Refresh table from database ───────────────────────
    private void loadTableFromDB() {
        model.setRowCount(0);
        try {
            Connection conn = DBConnector.getConnection();
            List<Object[]> rows = dao.getAllPropertiesForTable(conn);
            for (Object[] row : rows) {
                // DAO returns: {PropertyID, TypeName, Address, Location,
                //               "N$ X.XX" MonthlyRental, "N$ X.XX" MarketValue, Status}
                // Table cols:  {ID, Type, Address, Location, Rent/Month, Market Value, Status, Actions}
                model.addRow(new Object[]{
                        row[0], row[1], row[2], row[3],
                        row[4], row[5], row[6], ""
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load properties from database:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Summary bar ──────────────────────────────────────────────
    private JPanel buildSummaryBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        p.setOpaque(false);
        p.add(summaryChip(model.getRowCount() + " Total",   RentalTheme.NAVY,      RentalTheme.WHITE));
        return p;
    }

    private JLabel summaryChip(String text, Color fg, Color bg) {
        JLabel l = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 60));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(fg);
        l.setOpaque(false);
        l.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        return l;
    }

    private JLabel filterLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(RentalTheme.TEXT_MUTED);
        return l;
    }

    // ─────────────────────────────────────────────────────────────
    // DIALOG — Add / Edit Property
    // ─────────────────────────────────────────────────────────────
    private void showPropertyDialog(Object[] existingRow) {
        boolean isEdit = (existingRow != null);
        String title = isEdit ? "Edit Property" : "Add New Property";

        JDialog dialog = new StyledFormDialog(SwingUtilities.getWindowAncestor(this), title);

        JPanel form = new JPanel(new BorderLayout(0, 0));
        form.setBackground(RentalTheme.WHITE);
        form.add(RentalTheme.panelHeader(title), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(RentalTheme.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(16, 24, 8, 24));

        body.add(buildSectionHeader("Property Details"));

        JPanel baseFields = new JPanel(new GridBagLayout());
        baseFields.setBackground(RentalTheme.WHITE);
        GridBagConstraints g = defaultGbc();

        JComboBox<String> typeCombo = buildStyledCombo("House", "Townhouse", "Flat/Apartment");
        addFormRow(baseFields, g, 0, "Property Type *", typeCombo);

        JTextField buildingSizeField = RentalTheme.styledField("e.g. 120");
        addFormRow(baseFields, g, 1, "Building Size (m²) *", buildingSizeField);

        JTextField addressField = RentalTheme.styledField("e.g. 45 Independence Ave");
        addFormRow(baseFields, g, 2, "Street Address *", addressField);

        JComboBox<String> locationCombo = buildStyledCombo(
                "Katutura", "Kleine Kuppe", "Klein Windhoek", "Eros",
                "Olympia", "Ludwigsdorf", "Pioneers Park",
                "Hochland Park", "Academia", "Rocky Crest");
        addFormRow(baseFields, g, 3, "Location *", locationCombo);

        JTextField marketValueField = RentalTheme.styledField("0.00");
        addFormRow(baseFields, g, 4, "Market Value (N$)", marketValueField);

        JTextField rentField = RentalTheme.styledField("0.00");
        addFormRow(baseFields, g, 5, "Monthly Rent (N$) *", rentField);

        JComboBox<String> statusCombo = buildStyledCombo("Available", "Occupied");
        addFormRow(baseFields, g, 6, "Status", statusCombo);

        body.add(baseFields);
        body.add(Box.createVerticalStrut(8));
        body.add(buildSectionHeader("Type-Specific Details"));

        JPanel subtypeCard = new JPanel(new CardLayout());
        subtypeCard.setBackground(RentalTheme.WHITE);

        // House panel
        JPanel housePanel = new JPanel(new GridBagLayout());
        housePanel.setBackground(RentalTheme.WHITE);
        GridBagConstraints gh = defaultGbc();
        JTextField plotSizeField = RentalTheme.styledField("e.g. 500");
        addFormRow(housePanel, gh, 0, "Plot Size (m²) *", plotSizeField);
        subtypeCard.add(housePanel, "House");

        // Townhouse panel
        JPanel townhousePanel = new JPanel(new GridBagLayout());
        townhousePanel.setBackground(RentalTheme.WHITE);
        GridBagConstraints gt = defaultGbc();
        JTextField thUnitField = RentalTheme.styledField("e.g. T-04");
        addFormRow(townhousePanel, gt, 0, "Unit Number *", thUnitField);
        JComboBox<String> thBackyardCombo = buildStyledCombo("Yes", "No");
        addFormRow(townhousePanel, gt, 1, "Backyard?", thBackyardCombo);
        subtypeCard.add(townhousePanel, "Townhouse");

        // Flat panel
        JPanel flatPanel = new JPanel(new GridBagLayout());
        flatPanel.setBackground(RentalTheme.WHITE);
        GridBagConstraints gf = defaultGbc();
        JTextField flatUnitField    = RentalTheme.styledField("e.g. F-12");
        JTextField floorLevelField  = RentalTheme.styledField("e.g. 3");
        JComboBox<String> elevatorCombo = buildStyledCombo("Yes", "No");
        JComboBox<String> flatBackyardCombo = buildStyledCombo("Yes", "No");
        addFormRow(flatPanel, gf, 0, "Unit Number *",     flatUnitField);
        addFormRow(flatPanel, gf, 1, "Floor Level *",     floorLevelField);
        addFormRow(flatPanel, gf, 2, "Elevator Access?",  elevatorCombo);
        addFormRow(flatPanel, gf, 3, "Backyard?",         flatBackyardCombo);
        subtypeCard.add(flatPanel, "Flat/Apartment");

        body.add(subtypeCard);

        typeCombo.addActionListener(e -> {
            CardLayout cl = (CardLayout) subtypeCard.getLayout();
            cl.show(subtypeCard, (String) typeCombo.getSelectedItem());
            dialog.pack();
            dialog.setLocationRelativeTo(PropertyPanel.this);
        });

        // Pre-populate when editing
        if (isEdit) {
            String existingType = existingRow[1].toString();
            typeCombo.setSelectedItem(existingType);
            addressField.setText(existingRow[2].toString());
            locationCombo.setSelectedItem(existingRow[3].toString());
            rentField.setText(existingRow[4].toString().replace("N$ ", "").replace(",", ""));
            marketValueField.setText(existingRow[5].toString().replace("N$ ", "").replace(",", ""));
            statusCombo.setSelectedItem(existingRow[6].toString());
        }

        ((CardLayout) subtypeCard.getLayout()).show(subtypeCard,
                (String) typeCombo.getSelectedItem());

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(RentalTheme.WHITE);
        form.add(scroll, BorderLayout.CENTER);

        // ── Footer buttons ───────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(RentalTheme.CREAM_DARK);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, RentalTheme.BORDER));

        JButton cancel = RentalTheme.outlineButton("Cancel");
        cancel.addActionListener(e -> dialog.dispose());

        String confirmLabel = isEdit ? "Save Changes" : "Save Property";
        JButton confirm = RentalTheme.primaryButton(confirmLabel);

        confirm.addActionListener(e -> {
            // ── Validation ────────────────────────────────────────
            if (addressField.getText().trim().isEmpty()) {
                showError(dialog, "Street Address is required."); return;
            }
            if (rentField.getText().trim().isEmpty()) {
                showError(dialog, "Monthly Rent is required."); return;
            }

            double buildingSize, marketValue, monthlyRent;
            try { buildingSize = Double.parseDouble(buildingSizeField.getText().trim()); }
            catch (NumberFormatException ex) { showError(dialog, "Building Size must be a number."); return; }
            try { marketValue = Double.parseDouble(marketValueField.getText().trim()); }
            catch (NumberFormatException ex) { showError(dialog, "Market Value must be a number."); return; }
            try { monthlyRent = Double.parseDouble(rentField.getText().trim()); }
            catch (NumberFormatException ex) { showError(dialog, "Monthly Rent must be a number."); return; }

            String selectedType = (String) typeCombo.getSelectedItem();

            if ("House".equals(selectedType) && plotSizeField.getText().trim().isEmpty()) {
                showError(dialog, "Plot Size is required for a House."); return;
            }
            if ("Townhouse".equals(selectedType) && thUnitField.getText().trim().isEmpty()) {
                showError(dialog, "Unit Number is required for a Townhouse."); return;
            }
            if ("Flat/Apartment".equals(selectedType)) {
                if (flatUnitField.getText().trim().isEmpty()) {
                    showError(dialog, "Unit Number is required for a Flat."); return;
                }
                if (floorLevelField.getText().trim().isEmpty()) {
                    showError(dialog, "Floor Level is required for a Flat."); return;
                }
                try { Integer.parseInt(floorLevelField.getText().trim()); }
                catch (NumberFormatException ex) {
                    showError(dialog, "Floor Level must be a whole number (e.g. 3)."); return;
                }
            }

            // ── Determine type ID ─────────────────────────────────
            int typeId;
            switch (selectedType) {
                case "House":         typeId = PropertyDAO.TYPE_HOUSE;     break;
                case "Townhouse":     typeId = PropertyDAO.TYPE_TOWNHOUSE; break;
                default:              typeId = PropertyDAO.TYPE_FLAT;      break;
            }

            // ── Persist to DB ─────────────────────────────────────
            try {
                Connection conn = DBConnector.getConnection();

                if (isEdit) {
                    // existingRow[0] is the PropertyID
                    int propertyID = (int) existingRow[0];
                    dao.updateProperty(conn, propertyID, buildingSize,
                            addressField.getText().trim(),
                            (String) locationCombo.getSelectedItem(),
                            marketValue, monthlyRent,
                            (String) statusCombo.getSelectedItem());
                } else {
                    int newId = dao.insertProperty(conn, typeId, buildingSize,
                            addressField.getText().trim(),
                            (String) locationCombo.getSelectedItem(),
                            marketValue, monthlyRent,
                            (String) statusCombo.getSelectedItem());

                    if (newId == -1) {
                        showError(dialog, "Insert failed — no ID returned from database.");
                        return;
                    }

                    // Insert subtype row
                    if (typeId == PropertyDAO.TYPE_HOUSE) {
                        dao.insertHouse(conn, newId,
                                Double.parseDouble(plotSizeField.getText().trim()));
                    } else if (typeId == PropertyDAO.TYPE_TOWNHOUSE) {
                        dao.insertTownhouse(conn, newId,
                                thUnitField.getText().trim(),
                                "Yes".equals(thBackyardCombo.getSelectedItem()));
                    } else {
                        dao.insertFlat(conn, newId,
                                flatUnitField.getText().trim(),
                                Integer.parseInt(floorLevelField.getText().trim()),
                                "Yes".equals(elevatorCombo.getSelectedItem()),
                                "Yes".equals(flatBackyardCombo.getSelectedItem()));
                    }
                }

                // Refresh table from DB so the new row appears
                loadTableFromDB();

                JOptionPane.showMessageDialog(dialog,
                        "✓  Property " + (isEdit ? "updated" : "saved") + " successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Database error:\n" + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        footer.add(cancel);
        footer.add(confirm);
        form.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(form);
        dialog.setSize(500, 560);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ── Helpers ───────────────────────────────────────────────────
    private JLabel buildSectionHeader(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(RentalTheme.NAVY);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, RentalTheme.BORDER),
                BorderFactory.createEmptyBorder(4, 0, 6, 0)));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return l;
    }

    private GridBagConstraints defaultGbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets  = new Insets(5, 0, 5, 12);
        g.anchor  = GridBagConstraints.WEST;
        g.fill    = GridBagConstraints.NONE;
        return g;
    }

    private void addFormRow(JPanel panel, GridBagConstraints g,
                            int row, String labelText, JComponent input) {
        g.gridx = 0; g.gridy = row; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        panel.add(RentalTheme.formLabel(labelText), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        panel.add(input, g);
        g.fill = GridBagConstraints.NONE;
    }

    private JComboBox<String> buildStyledCombo(String... items) {
        return RentalTheme.styledCombo(items);
    }

    private void showError(JDialog parent, String message) {
        JOptionPane.showMessageDialog(parent, message,
                "Validation Error", JOptionPane.WARNING_MESSAGE);
    }

    // ── Shared helpers for other panels ──────────────────────────
    static JComponent buildFormInput(String spec) {
        if (spec.startsWith("combo:")) {
            return RentalTheme.styledCombo(spec.substring(6).split(","));
        } else {
            String def = spec.startsWith("text:") ? spec.substring(5) : "";
            JTextField f = RentalTheme.styledField(def);
            if (!def.isEmpty()) f.setText(def);
            return f;
        }
    }

    static JPanel buildDialogFooter(JDialog dialog, String confirmText) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        p.setBackground(RentalTheme.CREAM_DARK);
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, RentalTheme.BORDER));
        JButton cancel = RentalTheme.outlineButton("Cancel");
        cancel.addActionListener(e -> dialog.dispose());
        JButton confirm = RentalTheme.primaryButton(confirmText);
        confirm.addActionListener(e -> {
            JOptionPane.showMessageDialog(dialog, "✓  " + confirmText + " — success!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();
        });
        p.add(cancel); p.add(confirm);
        return p;
    }

    // ── Action column renderer ────────────────────────────────────
    static class ActionButtonRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            return buildActionPanel(sel);
        }
    }

    // ── Action column editor ──────────────────────────────────────
    class ActionButtonEditor extends DefaultCellEditor {
        private JPanel panel;
        private int editingRow;

        public ActionButtonEditor(JCheckBox cb) {
            super(cb);
            setClickCountToStart(1);
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable t, Object v, boolean sel, int row, int col) {
            editingRow = row;
            panel = buildActionPanel(true);

            // Edit button
            ((JButton) panel.getComponent(0)).addActionListener(e -> {
                stopCellEditing();
                Object[] rowData = new Object[model.getColumnCount()];
                for (int i = 0; i < model.getColumnCount(); i++)
                    rowData[i] = model.getValueAt(editingRow, i);
                showPropertyDialog(rowData);
            });

            // Delete button — calls PropertyDAO.deleteProperty()
            ((JButton) panel.getComponent(1)).addActionListener(e -> {
                stopCellEditing();
                int propertyID = (int) model.getValueAt(editingRow, 0);
                String address  = model.getValueAt(editingRow, 2).toString();
                int confirm = JOptionPane.showConfirmDialog(
                        PropertyPanel.this,
                        "Delete property at: " + address + "?\nThis cannot be undone.",
                        "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        Connection conn = DBConnector.getConnection();
                        dao.deleteProperty(conn, propertyID);
                        loadTableFromDB();
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(PropertyPanel.this,
                                "Could not delete property:\n" + ex.getMessage(),
                                "Database Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            return panel;
        }

        @Override public Object getCellEditorValue() { return ""; }
    }

    private static JPanel buildActionPanel(boolean selected) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
        p.setBackground(selected ? new Color(201, 168, 76, 30) : RentalTheme.WHITE);

        JButton edit = new JButton("Edit");
        edit.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        edit.setForeground(RentalTheme.INFO);
        edit.setBackground(RentalTheme.INFO_BG);
        edit.setBorder(BorderFactory.createCompoundBorder(
                new RentalTheme.RoundedBorder(4, RentalTheme.INFO, 1f),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        edit.setFocusPainted(false);
        edit.setOpaque(true);

        JButton del = new JButton("Delete");
        del.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        del.setForeground(RentalTheme.DANGER);
        del.setBackground(RentalTheme.DANGER_BG);
        del.setBorder(BorderFactory.createCompoundBorder(
                new RentalTheme.RoundedBorder(4, RentalTheme.DANGER, 1f),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        del.setFocusPainted(false);
        del.setOpaque(true);

        p.add(edit);
        p.add(del);
        return p;
    }
}

/** Reusable styled dialog shell */
class StyledFormDialog extends JDialog {
    public StyledFormDialog(Window owner, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setBackground(RentalTheme.WHITE);
        getRootPane().putClientProperty("apple.awt.draggableWindowBackground", Boolean.FALSE);
    }
}
