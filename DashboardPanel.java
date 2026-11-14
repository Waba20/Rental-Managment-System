import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * DashboardPanel — Main application frame.
 * Handled: SQLException line 88, Connection Scope, and Argument Matching.
 */
public class DashboardPanel extends JFrame {

    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JButton activeNavBtn;

    // Class-level connection to fix scope and "Unhandled Exception" errors
    private Connection conn;

    private JButton btnDashboard, btnProperty, btnTenant, btnLease, btnPayment, btnReport;

    private final String loggedInName;
    private final String loggedInRole;

    public DashboardPanel(String loggedInName, String loggedInRole) {
        this.loggedInName = loggedInName;
        this.loggedInRole = loggedInRole;

        // 1. Initialize Database Connection Safely
        try {
            this.conn = DBConnection.getConnection();
            if (this.conn == null || this.conn.isClosed()) {
                System.err.println("Warning: Connection established but currently closed.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database Connection Error: " + e.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
        }

        // 2. Setup Theme and Window
        RentalTheme.applyGlobalDefaults();
        initFrame();
        buildSidebar();
        buildContent();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /** Fallback for testing */
    public DashboardPanel() {
        this("Administrator", "Property Manager");
    }

    private void initFrame() {
        setTitle("James Property Holdings — Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1150, 700));
        setPreferredSize(new Dimension(1300, 780));
        getContentPane().setBackground(RentalTheme.CREAM);
        getContentPane().setLayout(new BorderLayout(0, 0));
    }

    private void buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(RentalTheme.NAVY);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(230, 0));

        // Logo Section
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        logoPanel.setBackground(RentalTheme.NAVY);
        logoPanel.setMaximumSize(new Dimension(230, 80));
        JLabel brandName = new JLabel("JAMES HOLDINGS");
        brandName.setFont(new Font("Georgia", Font.BOLD, 16));
        brandName.setForeground(RentalTheme.GOLD);
        logoPanel.add(brandName);
        sidebar.add(logoPanel);

        sidebar.add(navSectionLabel("MAIN MENU"));

        // Initialize Nav Buttons
        btnDashboard = navButton("📊", "Dashboard",  "dashboard");
        btnProperty  = navButton("🏠", "Properties", "property");
        btnTenant    = navButton("👤", "Tenants",     "tenant");
        btnLease     = navButton("📄", "Leases",      "lease");
        btnPayment   = navButton("💳", "Payments",    "payment");
        btnReport    = navButton("📈", "Reports",     "report");

        JButton[] navs = {btnDashboard, btnProperty, btnTenant, btnLease, btnPayment, btnReport};
        for (JButton b : navs) sidebar.add(b);

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(buildSidebarFooter());

        getContentPane().add(sidebar, BorderLayout.WEST);
        activateNav(btnDashboard);
    }

    private void buildContent() {
        cardLayout    = new CardLayout();
        contentPanel  = new JPanel(cardLayout);
        contentPanel.setBackground(RentalTheme.CREAM);

        // Standard Panels - Passing 'conn' where required by their constructors
        contentPanel.add(buildDashboardHome(), "dashboard");
        contentPanel.add(new PropertyPanel(),  "property");
        contentPanel.add(new TenantPanel(),    "tenant");

        // Use the class-level 'conn' initialized in constructor
        if (conn != null) {
            contentPanel.add(new LeasePanel(conn),   "lease");
            contentPanel.add(new PaymentPanel(),     "payment");
            contentPanel.add(new ReportPanel(conn),  "report");
        } else {
            // Error fallback panels
            contentPanel.add(createErrorState("Lease Module Unavailable"), "lease");
            contentPanel.add(createErrorState("Payment Module Unavailable"), "payment");
            contentPanel.add(createErrorState("Report Module Unavailable"), "report");
        }

        getContentPane().add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createErrorState(String msg) {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel("<html><center>⚠️ " + msg + "<br>Check Database Connection</center></html>"));
        return p;
    }

    private JPanel buildDashboardHome() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(RentalTheme.CREAM);

        JButton logoutBtn = RentalTheme.outlineButton("🚪 Sign Out");
        logoutBtn.addActionListener(e -> performLogout());
        root.add(buildTopbar("Dashboard Overview", new JComponent[]{logoutBtn}), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setBackground(RentalTheme.CREAM);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        // Live KPI Cards
        JPanel statsGrid = new JPanel(new GridLayout(1, 4, 15, 0));
        statsGrid.setOpaque(false);
        statsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        statsGrid.add(RentalTheme.statCard("Properties", fetchScalar("SELECT COUNT(*) FROM Property"), "Total registered", RentalTheme.NAVY));
        statsGrid.add(RentalTheme.statCard("Tenants", fetchScalar("SELECT COUNT(*) FROM Tenant WHERE Status='Active'"), "Active residents", RentalTheme.GOLD));
        statsGrid.add(RentalTheme.statCard("Active Leases", fetchScalar("SELECT COUNT(*) FROM Lease WHERE Status='Active'"), "Valid contracts", RentalTheme.NAVY));
        statsGrid.add(RentalTheme.statCard("Monthly Revenue", fetchRevenue(), "Current month", RentalTheme.SUCCESS));

        body.add(statsGrid);
        body.add(Box.createRigidArea(new Dimension(0, 25)));

        // Recent Activity Table
        JPanel actCard = RentalTheme.card();
        actCard.setLayout(new BorderLayout(0, 10));
        actCard.add(RentalTheme.sectionTitle("Recent System Transactions"), BorderLayout.NORTH);

        String[] cols = {"Date","Type","Tenant","Property","Amount","Status"};
        DefaultTableModel tm = new DefaultTableModel(cols, 0);
        loadRecentActivity(tm);

        JTable actTable = new JTable(tm);
        RentalTheme.styleTable(actTable);
        actCard.add(RentalTheme.styledScroll(actTable), BorderLayout.CENTER);

        body.add(actCard);
        root.add(new JScrollPane(body), BorderLayout.CENTER);
        return root;
    }

    private JButton navButton(String icon, String label, String cardName) {
        JButton b = new JButton(icon + "  " + label);
        b.setForeground(Color.WHITE);
        b.setBackground(RentalTheme.NAVY);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setMaximumSize(new Dimension(230, 50));

        b.addActionListener(e -> {
            activateNav(b);
            cardLayout.show(contentPanel, cardName);

            // Auto-refresh logic for the specialized panels
            if (conn != null) {
                for (Component comp : contentPanel.getComponents()) {
                    if (comp instanceof ReportPanel && "report".equals(cardName)) {
                        ((ReportPanel) comp).refreshData();
                    }
                    if (comp instanceof PaymentPanel && "payment".equals(cardName)) {
                        ((PaymentPanel) comp).refreshData();
                    }
                    if (comp instanceof LeasePanel && "lease".equals(cardName)) {
                        ((LeasePanel) comp).refreshData();
                    }
                }
            }
        });
        return b;
    }

    private void performLogout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to sign out?", "Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            new LoginPanel();
        }
    }

    private String fetchScalar(String sql) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString(1) : "0";
        } catch (Exception e) { return "—"; }
    }

    private String fetchRevenue() {
        String sql = "SELECT SUM(AmountPaid) FROM Payment WHERE MONTH(PaymentDate) = MONTH(GETDATE())";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? String.format("N$ %,.0f", rs.getDouble(1)) : "N$ 0";
        } catch (Exception e) { return "N$ 0"; }
    }

    private void loadRecentActivity(DefaultTableModel tm) {
        String sql = "SELECT TOP 8 PaymentDate, 'Payment', 'Verified', 'Address', AmountPaid, 'Paid' FROM Payment ORDER BY PaymentDate DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tm.addRow(new Object[]{rs.getDate(1), "Payment", "Verified", "Address", rs.getDouble(5), "Success"});
            }
        } catch (Exception ignored) {}
    }

    private void activateNav(JButton btn) {
        if (activeNavBtn != null) activeNavBtn.setBackground(RentalTheme.NAVY);
        activeNavBtn = btn;
        btn.setBackground(RentalTheme.GOLD);
    }

    private JComponent navSectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(new Color(255,255,255,80));
        l.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 0));
        return l;
    }

    private JPanel buildSidebarFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(255,255,255,10));
        footer.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JLabel u = new JLabel(loggedInName);
        u.setForeground(Color.WHITE);
        u.setFont(new Font("Segoe UI", Font.BOLD, 12));
        footer.add(u, BorderLayout.CENTER);
        return footer;
    }

    public static JPanel buildTopbar(String title, JComponent[] actions) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setPreferredSize(new Dimension(0, 60));
        bar.setBorder(BorderFactory.createMatteBorder(0,0,1,0, RentalTheme.BORDER));
        JLabel t = new JLabel("  " + title);
        t.setFont(new Font("Georgia", Font.BOLD, 18));
        bar.add(t, BorderLayout.WEST);
        if (actions != null) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 13));
            p.setOpaque(false);
            for (JComponent a : actions) p.add(a);
            bar.add(p, BorderLayout.EAST);
        }
        return bar;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginPanel::new);
    }
}