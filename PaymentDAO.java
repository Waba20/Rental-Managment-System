import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    // Fetches unique tenant names for the Ledger Dropdown
    public List<String> getTenantsWithPayments(Connection conn) throws SQLException {
        List<String> tenants = new ArrayList<>();
        String sql = "SELECT DISTINCT t.FirstName + ' ' + t.LastName as FullName " +
                "FROM Payment p " +
                "JOIN Lease l ON p.LeaseID = l.LeaseID " +
                "JOIN Tenant t ON l.TenantID = t.TenantID";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tenants.add(rs.getString("FullName"));
            }
        }
        return tenants;
    }

    // Fixed: Pulls all data for the Main Table
    public List<Object[]> getAllPaymentsForTable(Connection conn) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT p.PaymentID, t.FirstName + ' ' + t.LastName as Tenant, " +
                "pr.Address, p.AmountDue, p.AmountPaid, p.Outstanding, p.PaymentStatus, p.PaymentDate " +
                "FROM Payment p " +
                "JOIN Lease l ON p.LeaseID = l.LeaseID " +
                "JOIN Tenant t ON l.TenantID = t.TenantID " +
                "JOIN Property pr ON l.PropertyID = pr.PropertyID " +
                "ORDER BY p.PaymentDate DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                        "P-" + rs.getInt("PaymentID"),
                        rs.getString("Tenant"),
                        rs.getString("Address"),
                        rs.getDouble("AmountDue"),
                        rs.getDouble("AmountPaid"),
                        0.00, // Penalty placeholder
                        rs.getDouble("Outstanding"),
                        rs.getTimestamp("PaymentDate"),
                        rs.getTimestamp("PaymentDate"), // Due Date
                        rs.getString("PaymentStatus")
                });
            }
        }
        return rows;
    }

    // Logic for the Record Payment Dropdown
    public List<String[]> getActiveLeasesForDropdown(Connection conn) throws SQLException {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT l.LeaseID, t.FirstName + ' ' + t.LastName + ' (' + pr.Address + ')' as Display " +
                "FROM Lease l JOIN Tenant t ON l.TenantID = t.TenantID " +
                "JOIN Property pr ON l.PropertyID = pr.PropertyID WHERE l.Status = 'Active'";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new String[]{String.valueOf(rs.getInt("LeaseID")), rs.getString("Display")});
            }
        }
        return list;
    }

    public void insertPayment(Connection conn, int leaseID, double amountPaid) throws SQLException {
        double rent = 0;
        String s = "SELECT MonthlyRent FROM Lease WHERE LeaseID = ?";
        try(PreparedStatement ps = conn.prepareStatement(s)){
            ps.setInt(1, leaseID);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) rent = rs.getDouble("MonthlyRent");
        }
        double bal = Math.max(0, rent - amountPaid);
        String sql = "INSERT INTO Payment (LeaseID, AmountPaid, AmountDue, Outstanding, PaymentStatus, PaymentDate) VALUES (?,?,?,?,?,GETDATE())";
        try(PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1, leaseID);
            ps.setDouble(2, amountPaid);
            ps.setDouble(3, rent);
            ps.setDouble(4, bal);
            ps.setString(5, bal <= 0 ? "Paid" : "Partial");
            ps.executeUpdate();
        }
    }
}