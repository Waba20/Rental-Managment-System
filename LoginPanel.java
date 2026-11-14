import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

/**
 * LoginPanel — Full-screen login window for James Property Holdings.
 *
 * ● Verifies credentials against the Users table via TenantDAO.authenticate().
 * ● On success, disposes itself and launches DashboardPanel.
 * ● On failure, shows a styled error badge (no plain JOptionPane).
 * ● Password field shows/hides with the 👁 toggle button.
 *
 * DB table required (SQL Server Management Studio):
 *
 *   CREATE TABLE Users (
 *       UserID   INT IDENTITY PRIMARY KEY,
 *       Username NVARCHAR(100) NOT NULL UNIQUE,
 *       Password NVARCHAR(255) NOT NULL,   -- store hashed in production
 *       FullName NVARCHAR(200),
 *       Role     NVARCHAR(50) DEFAULT 'Manager'
 *   );
 *   INSERT INTO Users (Username, Password, FullName, Role)
 *   VALUES ('admin', 'admin123', 'James Holding', 'Property Manager');
 */
public class LoginPanel extends JFrame {

    // ── Colours ──────────────────────────────────────────────────────────────
    private static final Color NAVY        = new Color(13, 27, 42);
    private static final Color NAVY_SOFT   = new Color(22, 45, 70);
    private static final Color GOLD        = new Color(201, 168, 76);
    private static final Color WHITE       = Color.WHITE;
    private static final Color CREAM       = new Color(248, 247, 243);
    private static final Color BORDER_CLR  = new Color(220, 218, 210);
    private static final Color TEXT_MUTED  = new Color(110, 108, 100);
    private static final Color DANGER      = new Color(185, 28, 28);
    private static final Color DANGER_BG   = new Color(255, 235, 235);

    // ── State ────────────────────────────────────────────────────────────────
    private JTextField     tfUsername;
    private JPasswordField pfPassword;
    private JLabel         lblError;
    private JButton        btnLogin;

    // ── DAO ──────────────────────────────────────────────────────────────────
    private final TenantDAO tenantDAO = new TenantDAO();

    // ─────────────────────────────────────────────────────────────────────────
    public LoginPanel() {
        setTitle("James Property Holdings — Sign In");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setUndecorated(false);
        getContentPane().setBackground(CREAM);
        setLayout(new GridBagLayout());     // centre the card

        add(buildCard());

        pack();
        setMinimumSize(new Dimension(440, 560));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CARD
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new ShadowBorder(),
                BorderFactory.createEmptyBorder(36, 40, 36, 40)));
        card.setMaximumSize(new Dimension(360, 500));

        // ── Logo / brand ──────────────────────────────────────────────────
        card.add(buildBrand());
        card.add(Box.createRigidArea(new Dimension(0, 28)));

        // ── Form title ────────────────────────────────────────────────────
        JLabel title = new JLabel("Sign in to your account");
        title.setFont(new Font("Georgia", Font.BOLD, 17));
        title.setForeground(NAVY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 6)));

        JLabel sub = new JLabel("James Property Holdings — Rental Management");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(sub);
        card.add(Box.createRigidArea(new Dimension(0, 24)));

        // ── Error badge (hidden by default) ──────────────────────────────
        lblError = buildErrorLabel();
        card.add(lblError);

        // ── Username ──────────────────────────────────────────────────────
        card.add(fieldLabel("Username"));
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        tfUsername = buildTextField("Enter your username");
        card.add(tfUsername);
        card.add(Box.createRigidArea(new Dimension(0, 14)));

        // ── Password ──────────────────────────────────────────────────────
        card.add(fieldLabel("Password"));
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        card.add(buildPasswordRow());
        card.add(Box.createRigidArea(new Dimension(0, 24)));

        // ── Login button ──────────────────────────────────────────────────
        btnLogin = buildLoginButton();
        card.add(btnLogin);
        card.add(Box.createRigidArea(new Dimension(0, 16)));

        // ── Footer note ───────────────────────────────────────────────────
        JLabel footer = new JLabel("Contact your administrator if you cannot log in.");
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        footer.setForeground(TEXT_MUTED);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(footer);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BRAND LOGO AREA
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildBrand() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        p.setOpaque(false);

        // Gold diamond icon (matches sidebar)
        JLabel icon = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GOLD);
                int[] xs = {20, 38, 20, 2};
                int[] ys = {2,  20, 38, 20};
                g2.fillPolygon(xs, ys, 4);
                g2.setColor(new Color(NAVY.getRed(), NAVY.getGreen(), NAVY.getBlue(), 80));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawPolygon(xs, ys, 4);
                g2.dispose();
            }
        };
        icon.setPreferredSize(new Dimension(42, 42));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        JLabel name = new JLabel("James Property Holdings");
        name.setFont(new Font("Georgia", Font.BOLD, 15));
        name.setForeground(NAVY);
        JLabel role = new JLabel("RENTAL MANAGEMENT SYSTEM");
        role.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        role.setForeground(TEXT_MUTED);
        text.add(name);
        text.add(Box.createRigidArea(new Dimension(0, 2)));
        text.add(role);

        p.add(icon);
        p.add(text);
        return p;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FORM HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(NAVY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTextField buildTextField(String placeholder) {
        JTextField tf = new JTextField();
        tf.putClientProperty("JTextField.placeholderText", placeholder);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setPreferredSize(new Dimension(280, 40));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR, 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        tf.setBackground(WHITE);
        tf.addActionListener(e -> doLogin());   // Enter key triggers login
        return tf;
    }

    private JPanel buildPasswordRow() {
        JPanel row = new JPanel(new BorderLayout(0, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        pfPassword = new JPasswordField();
        pfPassword.putClientProperty("JTextField.placeholderText", "Enter your password");
        pfPassword.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pfPassword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR, 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        pfPassword.setBackground(WHITE);
        pfPassword.addActionListener(e -> doLogin());

        // 👁 toggle button
        JButton toggle = new JButton("👁");
        toggle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        toggle.setFocusPainted(false);
        toggle.setContentAreaFilled(false);
        toggle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR, 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.setBackground(new Color(245, 245, 245));
        toggle.setOpaque(true);
        final boolean[] visible = {false};
        toggle.addActionListener(e -> {
            visible[0] = !visible[0];
            pfPassword.setEchoChar(visible[0] ? (char) 0 : '•');
            toggle.setText(visible[0] ? "🙈" : "👁");
        });

        row.add(pfPassword, BorderLayout.CENTER);
        row.add(toggle, BorderLayout.EAST);
        return row;
    }

    private JLabel buildErrorLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(DANGER);
        l.setBackground(DANGER_BG);
        l.setOpaque(true);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DANGER, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        l.setVisible(false);   // hidden until an error occurs
        return l;
    }

    private JButton buildLoginButton() {
        JButton btn = new JButton("Sign In") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed()
                        ? new Color(150, 120, 50)
                        : getModel().isRollover() ? new Color(220, 185, 90) : GOLD;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(NAVY);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(280, 44));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addActionListener(e -> doLogin());
        return btn;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGIN LOGIC  →  calls TenantDAO.authenticate() → SQL Server
    // ─────────────────────────────────────────────────────────────────────────
    private void doLogin() {
        String username = tfUsername.getText().trim();
        String password = new String(pfPassword.getPassword()).trim();

        // ── Client-side empty field check ──────────────────────────────────
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Signing in…");
        lblError.setVisible(false);

        // ── Run DB check on background thread so UI stays responsive ───────
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            String dbError = null;
            String loggedInName = "User";
            String loggedInRole = "Manager";

            @Override
            protected Boolean doInBackground() {
                try {
                    Connection conn = DBConnection.getConnection();
                    // Use TenantDAO.authenticate() — checks Users table
                    boolean ok = tenantDAO.authenticate(conn, username, password);
                    if (ok) {
                        // Fetch display name and role for the sidebar footer
                        String sql = "SELECT * FROM Users WHERE Username= ?";
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, username);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                loggedInName = rs.getString("Username");

                            }
                        }
                    }
                    return ok;
                } catch (SQLException ex) {
                    dbError = "Database error: " + ex.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                btnLogin.setEnabled(true);
                btnLogin.setText("Sign In");
                try {
                    boolean success = get();
                    if (success) {
                        dispose();
                        new DashboardPanel(loggedInName, loggedInRole);
                    } else {
                        showError(dbError != null ? dbError
                                : "Invalid username or password. Please try again.");
                        pfPassword.setText("");
                        pfPassword.requestFocus();
                    }
                } catch (Exception ex) {
                    showError("Unexpected error: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void showError(String message) {
        lblError.setText("⚠  " + message);
        lblError.setVisible(true);
        pack();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHADOW BORDER (card drop-shadow)
    // ─────────────────────────────────────────────────────────────────────────
    static class ShadowBorder extends AbstractBorder {
        private static final int SHADOW = 6;
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int i = 0; i < SHADOW; i++) {
                int alpha = 20 - i * 3;
                if (alpha < 0) alpha = 0;
                g2.setColor(new Color(0, 0, 0, alpha));
                g2.drawRoundRect(x + i, y + i, w - i * 2 - 1, h - i * 2 - 1, 12, 12);
            }
            g2.setColor(new Color(220, 218, 210));
            g2.drawRoundRect(x + SHADOW - 1, y + SHADOW - 1,
                    w - (SHADOW - 1) * 2 - 1, h - (SHADOW - 1) * 2 - 1, 10, 10);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) {
            return new Insets(SHADOW, SHADOW, SHADOW, SHADOW);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginPanel::new);
    }
}
