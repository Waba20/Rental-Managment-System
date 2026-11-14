import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.List;

public class PaymentPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private final PaymentDAO dao = new PaymentDAO();

    // Stats Labels to update dynamically
    private JLabel lblTotalCollected, lblOutstanding, lblOnTime, lblLate;

    // Theme Colors
    private final Color NAVY = new Color(13, 27, 42);
    private final Color GOLD = new Color(197, 153, 83);

    public PaymentPanel() {
        setLayout(new BorderLayout(0, 20));
        setBackground(new Color(243, 244, 246));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 1. Header
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("📋 Payment Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.setOpaque(false);
        JButton btnRec = new JButton("Record Payment");
        JButton btnLedger = new JButton("Tenant Ledger");

        btnRec.addActionListener(e -> recordPaymentAction());
        btnLedger.addActionListener(e -> showLedgerDropdown());

        btns.add(btnRec); btns.add(btnLedger);
        top.add(title, BorderLayout.WEST); top.add(btns, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // 2. Stats Grid
        add(buildStatsGrid(), BorderLayout.CENTER);

        // 3. Table Card (Placed in South or wrapped in a container)
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);
        tableContainer.setPreferredSize(new Dimension(0, 400));

        String[] columnNames = {"ID", "Tenant", "Property", "Due", "Paid", "Penalty", "Balance", "Date", "Due Date", "Status", "Actions"};
        model = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(35);

        tableContainer.add(new JScrollPane(table), BorderLayout.CENTER);

        // Wrap stats and table
        JPanel centerWrapper = new JPanel(new BorderLayout(0, 20));
        centerWrapper.setOpaque(false);
        centerWrapper.add(buildStatsGrid(), BorderLayout.NORTH);
        centerWrapper.add(tableContainer, BorderLayout.CENTER);

        add(centerWrapper, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel buildStatsGrid() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 15, 0));
        grid.setOpaque(false);

        lblTotalCollected = new JLabel("N$ 0.00");
        lblOnTime = new JLabel("0");
        lblLate = new JLabel("0");
        lblOutstanding = new JLabel("N$ 0.00");

        grid.add(createMetricCard("TOTAL COLLECTED", lblTotalCollected, "This Month", new Color(34, 197, 94)));
        grid.add(createMetricCard("ON-TIME PAYMENTS", lblOnTime, "Payments", new Color(59, 130, 246)));
        grid.add(createMetricCard("LATE PAYMENTS", lblLate, "This Month", new Color(234, 179, 8)));
        grid.add(createMetricCard("OUTSTANDING", lblOutstanding, "Total Due", new Color(239, 68, 68)));
        return grid;
    }

    private JPanel createMetricCard(String title, JLabel valLabel, String sub, Color accent) {
        JPanel p = new JPanel(new GridLayout(3, 1));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, accent),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));

        JLabel t = new JLabel(title); t.setFont(new Font("Segoe UI", Font.BOLD, 10)); t.setForeground(Color.GRAY);
        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 18)); valLabel.setForeground(accent);
        JLabel s = new JLabel(sub); s.setFont(new Font("Segoe UI", Font.PLAIN, 10)); s.setForeground(Color.GRAY);

        p.add(t); p.add(valLabel); p.add(s);
        return p;
    }

    public void refreshData() {
        model.setRowCount(0);
        double totalPaid = 0;
        double totalDue = 0;

        try (Connection conn = DBConnection.getConnection()) {
            List<Object[]> rows = dao.getAllPaymentsForTable(conn);
            for (Object[] row : rows) {
                Object[] uiRow = new Object[11]; // Matching column length
                uiRow[0] = row[0]; // ID
                uiRow[1] = row[1]; // Tenant
                uiRow[2] = row[2]; // Property
                uiRow[3] = String.format("N$ %,.2f", row[3]); // Due
                uiRow[4] = String.format("N$ %,.2f", row[4]); // Paid
                uiRow[5] = "N$ 0.00";
                uiRow[6] = String.format("N$ %,.2f", row[6]); // Balance
                uiRow[7] = row[7]; // Date
                uiRow[8] = row[7]; // Due Date placeholder
                uiRow[9] = row[9]; // Status
                uiRow[10] = "...";

                model.addRow(uiRow);

                // Update math for stats
                totalPaid += (double)row[4];
                totalDue += (double)row[6];
            }

            // Update the Dashboard labels
            lblTotalCollected.setText(String.format("N$ %,.2f", totalPaid));
            lblOutstanding.setText(String.format("N$ %,.2f", totalDue));
            lblOnTime.setText(String.valueOf(rows.size()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showLedgerDropdown() {
        try (Connection conn = DBConnection.getConnection()) {
            List<String> tenants = dao.getTenantsWithPayments(conn);
            if (tenants.isEmpty()) return;

            JComboBox<String> tenantCombo = new JComboBox<>(tenants.toArray(new String[0]));
            tenantCombo.insertItemAt("-- Show All --", 0);
            tenantCombo.setSelectedIndex(0);

            if (JOptionPane.showConfirmDialog(this, tenantCombo, "Select Tenant", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                String selected = (String) tenantCombo.getSelectedItem();
                TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
                table.setRowSorter(sorter);
                if (selected.equals("-- Show All --")) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("^" + selected + "$", 1));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void recordPaymentAction() {
        try (Connection conn = DBConnection.getConnection()) {
            List<String[]> leases = dao.getActiveLeasesForDropdown(conn);
            JComboBox<String> combo = new JComboBox<>();
            for (String[] s : leases) combo.addItem(s[1]);

            JTextField amt = new JTextField();
            Object[] fields = {"Select Lease:", combo, "Amount Paid (N$):", amt};

            if (JOptionPane.showConfirmDialog(this, fields, "Record", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                int id = Integer.parseInt(leases.get(combo.getSelectedIndex())[0]);
                dao.insertPayment(conn, id, Double.parseDouble(amt.getText().trim()));
                refreshData();
            }
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }
}