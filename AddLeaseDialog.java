import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class AddLeaseDialog extends JDialog {
    private JComboBox<String> tenantCombo, propertyCombo;
    private JTextField rentField;
    private JButton btnSave, btnCancel;
    private Connection conn;

    public AddLeaseDialog(Frame owner, Connection conn) {
        super(owner, "James Holdings - New Lease Agreement", true);
        this.conn = conn;

        setLayout(new BorderLayout());
        setSize(500, 380);
        setLocationRelativeTo(owner);

        JPanel container = new JPanel(new BorderLayout(15, 15));
        container.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 25));

        // 1. Tenant: Concatenating FirstName and LastName
        form.add(new JLabel("Select Tenant:"));
        tenantCombo = new JComboBox<>(fetchTenants());
        form.add(tenantCombo);

        // 2. Property: Fetching ID and Address
        form.add(new JLabel("Select Property (Address):"));
        propertyCombo = new JComboBox<>(fetchProperties());
        form.add(propertyCombo);

        form.add(new JLabel("Monthly Rent (N$):"));
        rentField = new JTextField();
        form.add(rentField);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnSave = new JButton("Confirm Agreement");
        btnSave.setBackground(new Color(34, 197, 94));
        btnSave.setForeground(Color.WHITE);
        btnCancel = new JButton("Cancel");

        btnSave.addActionListener(e -> saveLease());
        btnCancel.addActionListener(e -> dispose());

        footer.add(btnCancel);
        footer.add(btnSave);

        container.add(new JLabel("<html><b>Register Lease - PropertyRentalDB</b></html>"), BorderLayout.NORTH);
        container.add(form, BorderLayout.CENTER);
        container.add(footer, BorderLayout.SOUTH);

        add(container);
    }

    private Vector<String> fetchTenants() {
        Vector<String> items = new Vector<>();
        // Concatenating names for the dropdown display
        String sql = "SELECT TenantID, FirstName + ' ' + LastName FROM Tenant";
        return executeFetch(sql);
    }

    private Vector<String> fetchProperties() {
        Vector<String> items = new Vector<>();
        // Explicitly picking PropertyID and Address per your schema
        String sql = "SELECT PropertyID, Address FROM Property WHERE Status='Available'";
        return executeFetch(sql);
    }

    private Vector<String> executeFetch(String sql) {
        Vector<String> items = new Vector<>();
        try {
            Connection activeConn = DBConnection.getConnection();
            try (Statement st = activeConn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    items.add(rs.getString(1) + " - " + rs.getString(2));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (items.isEmpty()) items.add("No Data Available");
        return items;
    }

    private void saveLease() {
        String tSel = tenantCombo.getSelectedItem().toString();
        String pSel = propertyCombo.getSelectedItem().toString();

        if (rentField.getText().isEmpty() || tSel.contains("No Data") || pSel.contains("No Data")) {
            JOptionPane.showMessageDialog(this, "Please select a valid Tenant and Property.");
            return;
        }

        try {
            int tenantID = Integer.parseInt(tSel.split(" - ")[0]);
            int propertyID = Integer.parseInt(pSel.split(" - ")[0]);
            double rent = Double.parseDouble(rentField.getText());

            // Mapping to your columns: MonthlyRental (from Property table name) or MonthlyRent (Lease table)?
            // Assuming your Lease table has 'MonthlyRent' as the column name
            String sql = "INSERT INTO Lease (TenantID, PropertyID, MonthlyRent, StartDate, Status) VALUES (?, ?, ?, GETDATE(), 'Active')";
            String updateProp = "UPDATE Property SET Status = 'Occupied' WHERE PropertyID = ?";

            Connection activeConn = DBConnection.getConnection();
            activeConn.setAutoCommit(false);

            try (PreparedStatement ps1 = activeConn.prepareStatement(sql);
                 PreparedStatement ps2 = activeConn.prepareStatement(updateProp)) {

                ps1.setInt(1, tenantID);
                ps1.setInt(2, propertyID);
                ps1.setDouble(3, rent);
                ps1.executeUpdate();

                ps2.setInt(1, propertyID);
                ps2.executeUpdate();

                activeConn.commit();
                JOptionPane.showMessageDialog(this, "Lease Created Successfully!");
                dispose();
            } catch (SQLException ex) {
                activeConn.rollback();
                JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
            } finally {
                activeConn.setAutoCommit(true);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}