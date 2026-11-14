import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.List;

public class LeasePanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private final LeaseDAO dao = new LeaseDAO();
    private Connection conn;

    public LeasePanel(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout(0, 20));
        setBackground(new Color(243, 244, 246));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("📜 Lease Agreements");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        top.add(title, BorderLayout.WEST);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnGroup.setOpaque(false);

        JButton btnAdd = new JButton("➕ New Lease Agreement");
        btnAdd.setBackground(new Color(197, 153, 83));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.addActionListener(e -> openAddLeaseDialog());

        JButton btnRefresh = new JButton("🔄 Refresh");
        btnRefresh.addActionListener(e -> refreshData());

        btnGroup.add(btnRefresh);
        btnGroup.add(btnAdd);
        top.add(btnGroup, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        String[] cols = {"Lease ID", "Tenant", "Property", "PropertyID", "Rent", "Start Date", "Status", "Actions"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);
        table.setRowHeight(45);
        table.getColumnModel().getColumn(3).setMinWidth(0);
        table.getColumnModel().getColumn(3).setMaxWidth(0);

        setupActionColumn();
        add(new JScrollPane(table), BorderLayout.CENTER);
        refreshData();
    }

    private void setupActionColumn() {
        table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                return new JButton("Manage ⚙");
            }
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && table.columnAtPoint(e.getPoint()) == 7) {
                    showActionMenu(e.getComponent(), e.getX(), e.getY(), row);
                }
            }
        });
    }

    private void showActionMenu(Component invoker, int x, int y, int row) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem terminate = new JMenuItem("❌ Terminate Lease");
        terminate.addActionListener(e -> {
            try {
                int leaseID = Integer.parseInt(model.getValueAt(row, 0).toString());
                int propID = Integer.parseInt(model.getValueAt(row, 3).toString());
                if (dao.terminateLease(DBConnection.getConnection(), leaseID, propID)) {
                    JOptionPane.showMessageDialog(this, "Lease Terminated.");
                    refreshData();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });
        menu.add(terminate);
        menu.show(invoker, x, y);
    }

    private void openAddLeaseDialog() {
        Window win = SwingUtilities.windowForComponent(this);
        if (win instanceof Frame) {
            AddLeaseDialog dialog = new AddLeaseDialog((Frame) win, conn);
            dialog.setVisible(true);
            refreshData();
        }
    }

    public void refreshData() {
        model.setRowCount(0);
        try {
            List<Object[]> data = dao.getAllLeasesForTable(DBConnection.getConnection());
            for (Object[] row : data) {
                if (row.length == 8) model.addRow(row);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}