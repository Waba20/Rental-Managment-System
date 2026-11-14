import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;

/**
 * TenantPanel — Tenant Management
 *
 * FIXED: Register, Edit, Remove, and view Lease History now all
 * call TenantDAO / LeaseDAO to persist changes and read data from
 * the SQL Server database via DBConnector.
 * The table is loaded from the DB on construction and refreshed
 * after every insert / update / delete.
 */
public class TenantPanel extends JPanel {

    private JTable table;
    private DefaultTableModel model;

    private final TenantDAO tenantDAO = new TenantDAO();
    private final LeaseDAO  leaseDAO  = new LeaseDAO();

    public TenantPanel() {
        setLayout(new BorderLayout());

        // ─── TOP TOOLBAR ──────────────────────────────────────────────
        JButton addBtn = new JButton("+ Register New Tenant");
        addBtn.addActionListener(e -> registerTenant());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(addBtn);
        add(toolbar, BorderLayout.NORTH);

        // ─── TABLE COLUMNS ────────────────────────────────────────────
        String[] cols = {
                "ID", "First Name", "Last Name",
                "ID / Passport", "Phone", "Email",
                "Status", "Actions"
        };

        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 7; // only Actions column is clickable
            }
        };

        table = new JTable(model);
        table.setRowHeight(34);
        table.getTableHeader().setReorderingAllowed(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);
        table.getColumnModel().getColumn(5).setPreferredWidth(150);
        table.getColumnModel().getColumn(6).setPreferredWidth(90);
        table.getColumnModel().getColumn(7).setPreferredWidth(200);

        table.getColumnModel().getColumn(6).setCellRenderer(new StatusRenderer());
        table.getColumnModel().getColumn(7).setCellRenderer(new TenantActionRenderer());
        table.getColumnModel().getColumn(7).setCellEditor(new TenantActionEditor(this));

        add(new JScrollPane(table), BorderLayout.CENTER);

        // ── Load data from DB ─────────────────────────────────────────
        loadTableFromDB();
    }

    // ── Load / Refresh table from database ───────────────────────────
    private void loadTableFromDB() {
        model.setRowCount(0);
        try {
            Connection conn = DBConnector.getConnection();
            // DAO returns: {TenantID, FullName, IDNumber, PhoneNumber, EmailAddress, Status}
            // We need separate First/Last for the table — split FullName on first space.
            List<Object[]> rows = tenantDAO.getAllTenantsForTable(conn);
            for (Object[] row : rows) {
                String fullName = row[1].toString();
                int spaceIdx = fullName.indexOf(' ');
                String firstName = spaceIdx > 0 ? fullName.substring(0, spaceIdx) : fullName;
                String lastName  = spaceIdx > 0 ? fullName.substring(spaceIdx + 1) : "";
                model.addRow(new Object[]{
                        row[0],      // TenantID
                        firstName,
                        lastName,
                        row[2],      // IDNumber
                        row[3],      // PhoneNumber
                        row[4],      // EmailAddress
                        row[5],      // Status
                        ""
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load tenants from database:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================
    // REGISTER NEW TENANT
    // =========================================================
    public void registerTenant() {
        JTextField firstName  = new JTextField();
        JTextField lastName   = new JTextField();
        JTextField idPassport = new JTextField();
        JTextField phone      = new JTextField();
        JTextField email      = new JTextField();
        JComboBox<String> status = new JComboBox<>(new String[]{"Active", "Blacklisted"});

        JPanel panel = buildFormPanel(
                new String[]{"First Name", "Last Name", "ID / Passport Number",
                        "Phone Number", "Email Address", "Status"},
                new JComponent[]{firstName, lastName, idPassport, phone, email, status}
        );

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Register New Tenant", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            if (firstName.getText().trim().isEmpty() || lastName.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "First Name and Last Name are required.", "Validation Error",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                Connection conn = DBConnector.getConnection();
                int newId = tenantDAO.insertTenant(conn,
                        firstName.getText().trim(),
                        lastName.getText().trim(),
                        idPassport.getText().trim(),
                        phone.getText().trim(),
                        email.getText().trim(),
                        (String) status.getSelectedItem());

                loadTableFromDB(); // Refresh table from DB

                JOptionPane.showMessageDialog(this,
                        "Tenant registered successfully with ID: " + newId,
                        "Success", JOptionPane.INFORMATION_MESSAGE);

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this,
                        "Database error while registering tenant:\n" + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // =========================================================
    // EDIT TENANT
    // =========================================================
    public void editTenant(int row) {
        int tenantID = (int) model.getValueAt(row, 0);

        JTextField firstName  = new JTextField(model.getValueAt(row, 1).toString());
        JTextField lastName   = new JTextField(model.getValueAt(row, 2).toString());
        JTextField idPassport = new JTextField(model.getValueAt(row, 3).toString());
        JTextField phone      = new JTextField(model.getValueAt(row, 4).toString());
        JTextField email      = new JTextField(model.getValueAt(row, 5).toString());
        JComboBox<String> status = new JComboBox<>(new String[]{"Active", "Blacklisted"});
        status.setSelectedItem(model.getValueAt(row, 6));

        JPanel panel = buildFormPanel(
                new String[]{"First Name", "Last Name", "ID / Passport Number",
                        "Phone Number", "Email Address", "Status"},
                new JComponent[]{firstName, lastName, idPassport, phone, email, status}
        );

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Edit Tenant – ID " + tenantID,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            if (firstName.getText().trim().isEmpty() || lastName.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "First Name and Last Name are required.", "Validation Error",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                Connection conn = DBConnector.getConnection();
                tenantDAO.updateTenant(conn, tenantID,
                        firstName.getText().trim(),
                        lastName.getText().trim(),
                        idPassport.getText().trim(),
                        phone.getText().trim(),
                        email.getText().trim(),
                        (String) status.getSelectedItem());

                loadTableFromDB(); // Refresh table from DB

                JOptionPane.showMessageDialog(this,
                        "Tenant updated successfully.", "Success",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this,
                        "Database error while updating tenant:\n" + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // =========================================================
    // REMOVE TENANT
    // =========================================================
    public void removeTenant(int row) {
        int tenantID = (int) model.getValueAt(row, 0);
        String name  = model.getValueAt(row, 1) + " " + model.getValueAt(row, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove tenant: " + name + "?\n" +
                        "This action cannot be undone.",
                "Confirm Removal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Connection conn = DBConnector.getConnection();

                // Safety check: block delete if tenant has an active lease
                if (leaseDAO.tenantHasActiveLease(conn, tenantID)) {
                    JOptionPane.showMessageDialog(this,
                            "Cannot delete " + name + " — they have an active lease.\n" +
                                    "Please terminate the lease first.",
                            "Cannot Delete", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                tenantDAO.deleteTenant(conn, tenantID);
                loadTableFromDB(); // Refresh table from DB

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this,
                        "Database error while removing tenant:\n" + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // =========================================================
    // VIEW LEASE HISTORY  (reads from DB via TenantDAO)
    // =========================================================
    public void viewLeaseHistory(int row) {
        int tenantID = (int) model.getValueAt(row, 0);
        String tenantName = model.getValueAt(row, 1) + " " + model.getValueAt(row, 2);

        // Columns: LeaseID, PropertyAddress, StartDate, EndDate, MonthlyRent, Status
        String[] cols = {"Lease ID", "Property / Address", "Start Date", "End Date", "Monthly Rent", "Status"};
        DefaultTableModel leaseModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        try {
            Connection conn = DBConnector.getConnection();
            // DAO returns: {LeaseID, PropertyAddress, StartDate, EndDate, MonthlyRent, Status}
            List<Object[]> history = tenantDAO.getLeaseHistoryForTenant(conn, tenantID);
            for (Object[] record : history) {
                leaseModel.addRow(record);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load lease history:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTable leaseTable = new JTable(leaseModel);
        leaseTable.setRowHeight(28);
        leaseTable.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(leaseTable);
        scrollPane.setPreferredSize(new Dimension(620, 200));

        JPanel histPanel = new JPanel(new BorderLayout(0, 8));
        histPanel.add(new JLabel("<html><b>" + tenantName + "</b> – Complete Lease History</html>",
                SwingConstants.CENTER), BorderLayout.NORTH);
        histPanel.add(scrollPane, BorderLayout.CENTER);

        if (leaseModel.getRowCount() == 0) {
            histPanel.add(new JLabel("No lease records found for this tenant.",
                    SwingConstants.CENTER), BorderLayout.SOUTH);
        }

        JOptionPane.showMessageDialog(this, histPanel,
                "Lease History – " + tenantName, JOptionPane.PLAIN_MESSAGE);
    }

    // =========================================================
    // HELPER: Build a two-column label/field form panel
    // =========================================================
    private JPanel buildFormPanel(String[] labels, JComponent[] fields) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.weightx = 0;
            panel.add(new JLabel(labels[i] + ":"), gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            if (fields[i] instanceof JTextField) {
                ((JTextField) fields[i]).setPreferredSize(new Dimension(220, 28));
            }
            panel.add(fields[i], gbc);
        }
        return panel;
    }

    // =========================================================
    // STATUS COLOUR RENDERER
    // =========================================================
    static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            setHorizontalAlignment(SwingConstants.CENTER);
            String status = value != null ? value.toString() : "";
            if (!isSelected) {
                switch (status) {
                    case "Active":
                        setForeground(new Color(0, 130, 60));
                        setBackground(new Color(220, 255, 235));
                        break;
                    case "Blacklisted":
                        setForeground(new Color(180, 0, 0));
                        setBackground(new Color(255, 220, 220));
                        break;
                    default:
                        setForeground(Color.DARK_GRAY);
                        setBackground(Color.WHITE);
                }
            }
            return this;
        }
    }

    // =========================================================
    // RENDERER – Show Edit / Remove / History buttons
    // =========================================================
    class TenantActionRenderer extends JPanel implements TableCellRenderer {
        JButton edit    = new JButton("Edit");
        JButton remove  = new JButton("Remove");
        JButton history = new JButton("History");

        public TenantActionRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 4, 3));
            styleBtn(edit,    new Color(30, 120, 200));
            styleBtn(remove,  new Color(200, 50, 50));
            styleBtn(history, new Color(100, 100, 100));
            add(edit); add(remove); add(history);
        }

        private void styleBtn(JButton btn, Color color) {
            btn.setForeground(Color.WHITE);
            btn.setBackground(color);
            btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            btn.setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }

    // =========================================================
    // EDITOR – Handle button clicks
    // =========================================================
    class TenantActionEditor extends AbstractCellEditor implements TableCellEditor {
        JPanel panel;
        JButton editBtn, removeBtn, historyBtn;
        int row;

        public TenantActionEditor(TenantPanel parent) {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 3));

            editBtn    = new JButton("Edit");
            removeBtn  = new JButton("Remove");
            historyBtn = new JButton("History");

            styleBtn(editBtn,    new Color(30, 120, 200));
            styleBtn(removeBtn,  new Color(200, 50, 50));
            styleBtn(historyBtn, new Color(100, 100, 100));

            editBtn.addActionListener(e -> {
                fireEditingStopped();
                parent.editTenant(row);
            });

            removeBtn.addActionListener(e -> {
                fireEditingStopped();
                parent.removeTenant(row);
            });

            historyBtn.addActionListener(e -> {
                fireEditingStopped();
                parent.viewLeaseHistory(row);
            });

            panel.add(editBtn);
            panel.add(removeBtn);
            panel.add(historyBtn);
        }

        private void styleBtn(JButton btn, Color color) {
            btn.setForeground(Color.WHITE);
            btn.setBackground(color);
            btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            btn.setOpaque(true);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            this.row = row;
            return panel;
        }

        @Override
        public Object getCellEditorValue() { return ""; }
    }

    // =========================================================
    // MAIN – quick standalone test
    // =========================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Tenant Management");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 420);
            frame.setLocationRelativeTo(null);
            frame.add(new TenantPanel());
            frame.setVisible(true);
        });
    }
}
