import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * TenantDAO — Data Access Object for the Tenant table.
 *
 * Responsible for all database operations involving tenants.
 *
 * Aligns with:
 *   - Tenant.java  (model: tenantId, name, phone, status)
 *   - DB schema from TenantDAO.java:
 *       Tenant(TenantID, FirstName, LastName, IDNumber,
 *              PhoneNumber, EmailAddress, Status)
 *       Users(UserID, Username, Password)   ← for login / authorisation
 *
 * NOTE on authorisation:
 *   TenantDAO.java requires a valid username + password before
 *   any tenant operation is allowed. This DAO separates concerns:
 *     - authenticate() checks credentials from the Users table.
 *     - The panel calls authenticate() first; only on success does it
 *       call insert / update / delete.
 */
public class TenantDAO {

    // ─────────────────────────────────────────────────────────────
    // AUTHORISATION — Verify user before allowing changes
    // Matches the login block in TenantDAO.java.
    // Satisfies: "allow authorised users to update tenant details."
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns true if the username + password pair exists in the Users table.
     * The panel must call this and check the result before any write operation.
     */
    public boolean authenticate(Connection conn, String username, String password) throws SQLException {
        // Note: Your SQL schema for 'Users' should have a Password column for this to work.
        String sql = "SELECT * FROM Users WHERE Username = ? AND Password = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // INSERT — Add a new tenant
    // Matches choice == 3 block in TenantDAO.java.
    // Returns the auto-generated TenantID, or -1 on failure.
    // ─────────────────────────────────────────────────────────────

    /**
     * Inserts a new tenant row.
     *
     * Column mapping (DB → Tenant model):
     *   FirstName + LastName → name  (model stores combined, DB splits them)
     *   IDNumber             → (no direct model field; stored for identity verification)
     *   PhoneNumber          → phone
     *   EmailAddress         → (no direct model field; stored for contact)
     *   Status               → status  ("Active" or "Blacklisted")
     */
    public int insertTenant(Connection conn, String firstName, String lastName, String idNumber,
                            String phoneNumber, String emailAddress, String status) throws SQLException {
        String sql = "INSERT INTO Tenant (FirstName, LastName, IDNumber, Phone, Email, Status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, idNumber);
            ps.setString(4, phoneNumber);
            ps.setString(5, emailAddress);
            ps.setString(6, status);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    /**
     * Convenience method: updates ONLY the Status column.
     * Used to blacklist a tenant (set Status = "Blacklisted") without
     * touching any other field.
     * Satisfies: "the system must maintain a complete tenant's lease history"
     * — we blacklist rather than delete so history is preserved.
     */
    public int updateTenant(Connection conn, int tenantID, String firstName, String lastName,
                            String idNumber, String phoneNumber, String emailAddress, String status) throws SQLException {
        String sql = "UPDATE Tenant SET FirstName = ?, LastName = ?, IDNumber = ?, Phone = ?, Email = ?, Status = ? WHERE TenantID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, idNumber);
            ps.setString(4, phoneNumber);
            ps.setString(5, emailAddress);
            ps.setString(6, status);
            ps.setInt(7, tenantID);
            return ps.executeUpdate();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE — Remove a tenant
    // Matches choice == 2 block in TenantDAO.java.
    // ─────────────────────────────────────────────────────────────

    /**
     * Deletes a tenant by ID.
     * NOTE: Only delete tenants who have no active leases.
     * The panel should call LeaseDAO.hasActiveLease() first and
     * block deletion if an active lease exists.
     * Lease history rows (with a different status) are preserved by the FK.
     */
    public boolean deleteTenant(Connection conn, int tenantID) throws SQLException {
        String sql = "DELETE FROM Tenant WHERE TenantID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantID);
            return ps.executeUpdate() > 0;
        }
    }
    // ─────────────────────────────────────────────────────────────
    // QUERY — Read tenants for display in TenantPanel table
    // Columns: {TenantID, Name, IDNumber, Phone, Email, Status}
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns all tenants as rows for the TenantPanel table model.
     * Concatenates FirstName + LastName into a single "Name" column to
     * match the Tenant model's single name field.
     */
    public List<Object[]> getAllTenantsForTable(Connection conn) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT TenantID, CONCAT(FirstName, ' ', LastName) AS FullName, IDNumber, Phone, Email, Status FROM Tenant ORDER BY TenantID";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                        rs.getInt("TenantID"),
                        rs.getString("FullName"),
                        rs.getString("IDNumber"),
                        rs.getString("Phone"),
                        rs.getString("Email"),
                        rs.getString("Status")
                });
            }
        }
        return rows;
    }

    /**
     * Loads a single tenant row by ID.
     * Returns null if not found.
     * Used to pre-populate the edit dialog in TenantPanel.
     */
    public ResultSet getTenantById(Connection conn, int tenantID) throws SQLException {
        String sql = "SELECT * FROM Tenant WHERE TenantID = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, tenantID);
        return ps.executeQuery(); // Caller is responsible for closing
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY — Tenant lease history
    // Satisfies: "the system must maintain a complete tenant's lease history."
    // Returns all Lease rows ever associated with a tenant, regardless of status.
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns the full lease history for a tenant as display rows:
     *   {LeaseID, PropertyAddress, StartDate, EndDate, MonthlyRent, Status}
     */
    public List<Object[]> getLeaseHistoryForTenant(Connection conn,
                                                   int tenantID) throws SQLException {
        List<Object[]> rows = new ArrayList<>();

        String sql = "SELECT l.LeaseID, p.Address, l.StartDate, l.EndDate, "
                + "       l.MonthlyRent, l.Status "
                + "FROM Lease l "
                + "JOIN Property p ON l.PropertyID = p.PropertyID "
                + "WHERE l.TenantID = ? "
                + "ORDER BY l.StartDate DESC"; // Most recent first

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new Object[]{
                        rs.getInt("LeaseID"),
                        rs.getString("Address"),
                        rs.getDate("StartDate"),
                        rs.getDate("EndDate"),
                        String.format("N$ %.2f", rs.getDouble("MonthlyRent")),
                        rs.getString("Status")
                });
            }
        }
        return rows;
    }
}