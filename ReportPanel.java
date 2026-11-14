import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.sql.*;
import java.util.List;

public class ReportPanel extends JPanel {

    // James Holdings Branding
    private static final Color NAVY = new Color(13, 27, 42);
    private static final Color GOLD = new Color(197, 153, 83);
    private static final Color CREAM = new Color(243, 244, 246);
    private static final Color SUCCESS = new Color(34, 197, 94);
    private static final Color DANGER = new Color(239, 68, 68);

    private JTabbedPane tabs;
    private Connection dbConn;
    private final PropertyDAO propertyDAO = new PropertyDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    public ReportPanel(Connection conn) {
        this.dbConn = conn;
        setLayout(new BorderLayout());
        setBackground(CREAM);

        // 1. Navigation & Export Bar
        add(buildActionBar(), BorderLayout.NORTH);

        // 2. Main Content
        tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        add(tabs, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(NAVY);
        bar.setPreferredSize(new Dimension(100, 80));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 25));

        JLabel title = new JLabel("PORTFOLIO ANALYTICS");
        title.setFont(new Font("Serif", Font.BOLD, 24));
        title.setForeground(GOLD);
        bar.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 20));
        actions.setOpaque(false);

        // Export Buttons
        JButton btnPDF = createStyledButton("📕 Export PDF", Color.WHITE);
        JButton btnCSV = createStyledButton("📄 Export CSV", Color.WHITE);
        JButton btnRefresh = createStyledButton("🔄 Refresh", GOLD);

        btnPDF.addActionListener(e -> exportToPDF());
        btnCSV.addActionListener(e -> exportToCSV());
        btnRefresh.addActionListener(e -> refreshData());

        actions.add(btnRefresh);
        actions.add(btnCSV);
        actions.add(btnPDF);

        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    public void refreshData() {
        tabs.removeAll();
        try {
            // RE-FETCH the connection to ensure it hasn't timed out
            Connection activeConn = DBConnection.getConnection();

            if (activeConn == null || activeConn.isClosed()) {
                throw new SQLException("Database connection is lost or closed.");
            }

            // Use activeConn for the DAO calls
            List<Object[]> propData = propertyDAO.getAllPropertiesForTable(activeConn);
            List<Object[]> paymentData = paymentDAO.getAllPaymentsForTable(activeConn);

            tabs.addTab("📊 Portfolio Dashboard", buildDashboardView(propData, paymentData));
            tabs.addTab("🏠 Property Inventory", createTableTab(new String[]{"ID", "Type", "Address", "Location", "Rent", "Value", "Status"}, propData));
            tabs.addTab("💳 Payment Ledger", createTableTab(new String[]{"Ref", "Tenant", "Lease ID", "Amount Due", "Amount Paid", "Balance", "Status", "Date"}, paymentData));

        } catch (Exception e) {
            // Show a helpful message instead of crashing
            JPanel errorPanel = new JPanel(new BorderLayout());
            errorPanel.add(new JLabel("⚠️ Sync Error: " + e.getMessage(), SwingConstants.CENTER));
            tabs.addTab("❌ Connection Error", errorPanel);
            e.printStackTrace();
        }
        revalidate();
        repaint();
    }
    private JScrollPane buildDashboardView(List<Object[]> propData, List<Object[]> payData) {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(CREAM);
        body.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // Math for Advanced Stats
        double totalCollected = payData.stream().mapToDouble(r -> (double)r[4]).sum();
        double totalOutstanding = payData.stream().mapToDouble(r -> (double)r[6]).sum();
        double collectionRate = (totalCollected + totalOutstanding == 0) ? 0 : (totalCollected / (totalCollected + totalOutstanding)) * 100;

        // 1. STATS ROW
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 20, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(2000, 110));

        statsRow.add(createMetricCard("TOTAL REVENUE", String.format("N$ %,.0f", totalCollected), "Collected", SUCCESS));
        statsRow.add(createMetricCard("OUTSTANDING", String.format("N$ %,.0f", totalOutstanding), "Due", DANGER));
        statsRow.add(createMetricCard("OCCUPANCY", calculateOccupancy(propData), "Capacity", GOLD));
        statsRow.add(createMetricCard("HEALTH", String.format("%.1f%%", collectionRate), "Collection Rate", NAVY));

        body.add(statsRow);
        body.add(Box.createVerticalStrut(30));

        // 2. GRAPH ROW
        JPanel graphRow = new JPanel(new GridLayout(1, 2, 25, 0));
        graphRow.setOpaque(false);
        graphRow.setMaximumSize(new Dimension(2000, 320));

        graphRow.add(createOccupancyChart("Unit Allocation", propData));
        graphRow.add(createFinancialSummary("Financial Overview", totalCollected, totalOutstanding));

        body.add(graphRow);
        return new JScrollPane(body);
    }

    private JPanel createOccupancyChart(String title, List<Object[]> data) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel t = new JLabel(title); t.setFont(new Font("Segoe UI", Font.BOLD, 16));
        card.add(t, BorderLayout.NORTH);

        long occupied = data.stream().filter(r -> r[6].toString().equalsIgnoreCase("Occupied")).count();
        long vacant = data.size() - occupied;

        JPanel barPainter = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int barW = getWidth() - 40;
                int barH = 45;
                double ratio = (data.isEmpty()) ? 0 : (double) occupied / data.size();

                g2.setColor(new Color(230, 230, 230));
                g2.fill(new RoundRectangle2D.Float(20, 60, barW, barH, 20, 20));
                g2.setColor(GOLD);
                g2.fill(new RoundRectangle2D.Float(20, 60, (int)(barW * ratio), barH, 20, 20));

                g2.setColor(NAVY);
                g2.drawString("Occupied (" + occupied + ")", 25, 125);
                g2.setColor(Color.LIGHT_GRAY);
                g2.drawString("Vacant (" + vacant + ")", barW - 60, 125);
            }
        };
        card.add(barPainter, BorderLayout.CENTER);
        return card;
    }

    private JPanel createFinancialSummary(String title, double collected, double outstanding) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel t = new JLabel(title); t.setFont(new Font("Segoe UI", Font.BOLD, 16));
        card.add(t, BorderLayout.NORTH);

        JPanel info = new JPanel(new GridLayout(2, 1));
        info.setOpaque(false);
        JLabel l1 = new JLabel("✓ Successfully Processed: N$ " + String.format("%,.2f", collected));
        l1.setForeground(SUCCESS);
        JLabel l2 = new JLabel("⚠ Pending Collection: N$ " + String.format("%,.2f", outstanding));
        l2.setForeground(DANGER);

        info.add(l1); info.add(l2);
        card.add(info, BorderLayout.CENTER);
        return card;
    }

    private void exportToCSV() {
        Component c = tabs.getSelectedComponent();
        if (!(c instanceof JScrollPane) || !(((JScrollPane)c).getViewport().getView() instanceof JTable)) return;

        JTable table = (JTable)((JScrollPane)c).getViewport().getView();
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(chooser.getSelectedFile() + ".csv")) {
                for (int i=0; i<table.getColumnCount(); i++) pw.print(table.getColumnName(i) + (i==table.getColumnCount()-1?"":","));
                pw.println();
                for (int i=0; i<table.getRowCount(); i++) {
                    for (int j=0; j<table.getColumnCount(); j++) pw.print(table.getValueAt(i, j).toString().replace(",", "") + (j==table.getColumnCount()-1?"":","));
                    pw.println();
                }
                JOptionPane.showMessageDialog(this, "CSV Exported!");
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    private void exportToPDF() {
        Component c = tabs.getSelectedComponent();
        if (!(c instanceof JScrollPane) || !(((JScrollPane)c).getViewport().getView() instanceof JTable)) return;

        JTable table = (JTable)((JScrollPane)c).getViewport().getView();
        try {
            table.print(JTable.PrintMode.FIT_WIDTH, new java.text.MessageFormat("James Holdings - Financial Report"), new java.text.MessageFormat("Page {0}"));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private JPanel createMetricCard(String title, String val, String sub, Color accent) {
        JPanel p = new JPanel(new GridLayout(3, 1));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, accent), BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        JLabel t = new JLabel(title); t.setFont(new Font("Segoe UI", Font.BOLD, 10)); t.setForeground(Color.GRAY);
        JLabel v = new JLabel(val); v.setFont(new Font("Segoe UI", Font.BOLD, 22)); v.setForeground(NAVY);
        JLabel s = new JLabel(sub); s.setFont(new Font("Segoe UI", Font.PLAIN, 10)); s.setForeground(accent);
        p.add(t); p.add(v); p.add(s);
        return p;
    }

    private String calculateOccupancy(List<Object[]> data) {
        if (data.isEmpty()) return "0%";
        long occupied = data.stream().filter(r -> r[6].toString().equalsIgnoreCase("Occupied")).count();
        return (occupied * 100 / data.size()) + "%";
    }

    private JScrollPane createTableTab(String[] cols, List<Object[]> data) {
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        for (Object[] row : data) model.addRow(row);
        JTable table = new JTable(model);
        table.setRowHeight(35);
        return new JScrollPane(table);
    }

    private JButton createStyledButton(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(fg);
        b.setContentAreaFilled(false);
        b.setBorder(BorderFactory.createLineBorder(fg, 1));
        return b;
    }
}