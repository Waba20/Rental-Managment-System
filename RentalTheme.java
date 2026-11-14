import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;

/**
 * Centralised design tokens matching the HTML reference design.
 * Navy + Gold luxury aesthetic — James Property Holdings.
 */
public class RentalTheme {

    // ── COLOR PALETTE ─────────────────────────────────────────────
    public static final Color GOLD          = new Color(0xC9A84C);
    public static final Color GOLD_LIGHT    = new Color(0xF0DFA0);
    public static final Color GOLD_DARK     = new Color(0x8B6914);
    public static final Color NAVY          = new Color(0x0D1B2A);
    public static final Color NAVY_MID      = new Color(0x1A2D42);
    public static final Color NAVY_SOFT     = new Color(0x243650);
    public static final Color CREAM         = new Color(0xFAF8F3);
    public static final Color CREAM_DARK    = new Color(0xF0EDE4);
    public static final Color WHITE         = Color.WHITE;
    public static final Color TEXT          = new Color(0x1A1A1A);
    public static final Color TEXT_MUTED    = new Color(0x6B7280);
    public static final Color TEXT_LIGHT    = new Color(0x9CA3AF);
    public static final Color BORDER        = new Color(201, 168, 76, 50);
    public static final Color BORDER_STRONG = new Color(201, 168, 76, 115);
    public static final Color SUCCESS       = new Color(0x2D7A4F);
    public static final Color SUCCESS_BG    = new Color(0xEDFAF3);
    public static final Color DANGER        = new Color(0xC0392B);
    public static final Color DANGER_BG     = new Color(0xFDECEA);
    public static final Color WARNING       = new Color(0xB07D00);
    public static final Color WARNING_BG    = new Color(0xFFF8E1);
    public static final Color INFO          = new Color(0x1565C0);
    public static final Color INFO_BG       = new Color(0xE8F0FE);

    // ── FONTS ─────────────────────────────────────────────────────
    public static final Font SERIF_DISPLAY  = new Font("Georgia", Font.PLAIN, 14);
    public static final Font SANS           = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font SANS_BOLD      = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font SANS_SM        = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font TITLE_FONT     = new Font("Georgia", Font.BOLD, 20);
    public static final Font STAT_VALUE     = new Font("Georgia", Font.BOLD, 28);
    public static final Font LABEL_CAPS     = new Font("Segoe UI", Font.BOLD, 10);

    // ── GLOBAL SETUP ──────────────────────────────────────────────
    public static void applyGlobalDefaults() {
        UIManager.put("Panel.background", CREAM);
        UIManager.put("OptionPane.background", WHITE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("Button.font", SANS);
        UIManager.put("Label.font", SANS);
        UIManager.put("TextField.font", SANS);
        UIManager.put("ComboBox.font", SANS);
        UIManager.put("Table.font", SANS);
        UIManager.put("TableHeader.font", SANS_BOLD);
        UIManager.put("ScrollBar.width", 8);
    }

    // ── FACTORY METHODS ───────────────────────────────────────────

    /** Primary gold-filled button */
    public static JButton primaryButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) g2.setColor(GOLD_DARK);
                else if (getModel().isRollover()) g2.setColor(GOLD.brighter());
                else g2.setColor(GOLD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(NAVY);
                g2.setFont(SANS_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false);
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(b.getPreferredSize().width + 20, 34));
        return b;
    }

    /** Outline/ghost button */
    public static JButton outlineButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(201, 168, 76, 20));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.setColor(BORDER_STRONG);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.setColor(TEXT_MUTED);
                g2.setFont(SANS);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false);
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(b.getPreferredSize().width + 20, 34));
        return b;
    }

    /** Danger/red small button */
    public static JButton dangerButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? DANGER : new Color(192, 57, 43, 200));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(WHITE);
                g2.setFont(SANS_SM);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false);
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Styled text field */
    public static JTextField styledField(String placeholder) {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BORDER_STRONG);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        f.setOpaque(false); f.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        f.setFont(SANS); f.setForeground(TEXT);
        f.setPreferredSize(new Dimension(200, 34));
        return f;
    }

    /** Styled combo box */
    public static JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(SANS); c.setBackground(WHITE);
        c.setForeground(TEXT); c.setPreferredSize(new Dimension(160, 34));
        c.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(8, BORDER_STRONG, 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        return c;
    }

    /** Panel card with rounded border and shadow-like effect */
    public static JPanel card() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(13, 27, 42, 18));
                g2.fillRoundRect(3, 4, getWidth()-4, getHeight()-4, 14, 14);
                g2.setColor(WHITE);
                g2.fillRoundRect(0, 0, getWidth()-2, getHeight()-2, 12, 12);
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-3, getHeight()-3, 12, 12);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        return p;
    }

    /** Stat card for dashboard */
    public static JPanel statCard(String label, String value, String sub, Color accentColor) {
        JPanel card = card();
        card.setLayout(new BorderLayout(4, 4));

        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(LABEL_CAPS); lbl.setForeground(TEXT_MUTED);

        JLabel val = new JLabel(value);
        val.setFont(STAT_VALUE); val.setForeground(accentColor != null ? accentColor : NAVY);

        JLabel subLbl = new JLabel(sub);
        subLbl.setFont(SANS_SM); subLbl.setForeground(TEXT_MUTED);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false); top.add(lbl, BorderLayout.WEST);
        card.add(top, BorderLayout.NORTH);
        card.add(val, BorderLayout.CENTER);
        card.add(subLbl, BorderLayout.SOUTH);
        return card;
    }

    /** Badge/pill label */
    public static JLabel badge(String text, Color bg, Color fg) {
        JLabel l = new JLabel(text, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                super.paintComponent(g);
                g2.dispose();
            }
        };
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(fg); l.setOpaque(false);
        l.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        return l;
    }

    /** Style a JTable to match the HTML design */
    public static void styleTable(JTable table) {
        table.setFont(SANS); table.setForeground(TEXT);
        table.setRowHeight(38); table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(240, 237, 228));
        table.setBackground(WHITE); table.setSelectionBackground(new Color(201, 168, 76, 30));
        table.setSelectionForeground(TEXT); table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setBackground(CREAM_DARK);
        table.getTableHeader().setForeground(TEXT_MUTED);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_STRONG));
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        table.setIntercellSpacing(new Dimension(12, 0));
    }

    /** Styled scroll pane */
    public static JScrollPane styledScroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(WHITE);
        sp.setBackground(WHITE);
        return sp;
    }

    /** Panel section header label */
    public static JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Georgia", Font.BOLD, 15));
        l.setForeground(NAVY);
        return l;
    }

    /** Divider line */
    public static JSeparator divider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        return sep;
    }

    // ── INNER: Rounded Border ──────────────────────────────────────
    public static class RoundedBorder extends AbstractBorder {
        private final int radius; private final Color color; private final float stroke;
        public RoundedBorder(int r, Color c, float s) { radius = r; color = c; stroke = s; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color); g2.setStroke(new BasicStroke(stroke));
            g2.drawRoundRect(x, y, w-1, h-1, radius, radius);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(4, 8, 4, 8); }
    }

    // ── INNER: Status badge renderer ──────────────────────────────
    public static class StatusRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            String status = v == null ? "" : v.toString();
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setFont(new Font("Segoe UI", Font.BOLD, 10));
            l.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            switch (status.toLowerCase()) {
                case "active":    l.setForeground(SUCCESS); l.setBackground(sel ? new Color(45,122,79,40) : SUCCESS_BG); break;
                case "occupied":  l.setForeground(SUCCESS); l.setBackground(sel ? new Color(45,122,79,40) : SUCCESS_BG); break;
                case "paid":      l.setForeground(SUCCESS); l.setBackground(sel ? new Color(45,122,79,40) : SUCCESS_BG); break;
                case "blacklisted":
                case "terminated":
                case "danger":    l.setForeground(DANGER); l.setBackground(sel ? new Color(192,57,43,40) : DANGER_BG); break;
                case "overdue":
                case "late":      l.setForeground(DANGER); l.setBackground(sel ? new Color(192,57,43,40) : DANGER_BG); break;
                case "pending":
                case "partial":   l.setForeground(WARNING); l.setBackground(sel ? new Color(176,125,0,40) : WARNING_BG); break;
                case "available": l.setForeground(INFO); l.setBackground(sel ? new Color(21,101,192,40) : INFO_BG); break;
                default:          l.setForeground(TEXT_MUTED); l.setBackground(sel ? new Color(107,114,128,30) : CREAM);
            }
            l.setOpaque(true);
            return l;
        }
    }

    /** Build a standard form dialog */
    public static JDialog formDialog(String title, Component parent) {
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(parent), title, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        d.getContentPane().setBackground(WHITE);
        return d;
    }

    /** Build a form label */
    public static JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(TEXT_MUTED);
        return l;
    }

    /** Panel header bar with gold-accented left border */
    public static JPanel panelHeader(String title) {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(GOLD);
                g2.fillRect(0, 0, 3, getHeight());
                g2.dispose();
            }
        };
        p.setBackground(CREAM_DARK);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_STRONG),
                BorderFactory.createEmptyBorder(12, 20, 12, 16)));
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Georgia", Font.BOLD, 15));
        lbl.setForeground(NAVY);
        p.add(lbl, BorderLayout.WEST);
        return p;
    }
}