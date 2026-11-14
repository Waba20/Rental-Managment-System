import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * LeaseDAO — Data Access Object for the Lease table.
 *
 * Responsible for all database operations involving leases.
 *
 * Aligns with:
 *   - Lease.java    (abstract model: leaseID, tenantID, propertyID, startDate,
 *                    endDate, monthlyRent, deposit, dueDay, gracePeriod,
 *                    latePenalty, status)
 *   - Payment.java  (concrete child: extends Lease, implements Billing)
 *   - LeaseService.java (in-memory service layer — this DAO persists those
 *                        same operations to the database)
 *   - DB schema from LeaseDAO.java:
 *       Lease(LeaseID, PropertyID, TenantID, StartDate, EndDate,
 *             MonthlyRent, SecurityDeposit, PaymentDueDate,
 *             GracePeriod, LatePenalty, Status)
 *
 * BUSINESS RULES enforced here:
 *   1. Cannot lease an already-occupied property
 *      (calls PropertyDAO.isOccupied before insert).
 *   2. After creating a lease, property Status is set to "Occupied".
 *   3. After terminating a lease, property Status is set to "Available".
 */
public class LeaseDAO {

    // ─────────────────────────────────────────────────────────────
    // INSERT — Create a new lease
    // Matches the INSERT block in LeaseDAO.java.
    // Also enforces the "no double-leasing" business rule.
    // Returns the auto-generated LeaseID, or -1 on failure.
    // ─────────────────────────────────────────────────────────────

    /**
     * Creates a new lease and marks the property as "Occupied".
     *
     * Column mapping (Lease model → DB):
     *   leaseID         → LeaseID  (auto-generated)
     *   tenantID        → TenantID
     *   propertyID      → PropertyID
     *   startDate       → StartDate
     *   endDate         → EndDate
     *   monthlyRent     → MonthlyRent
     *   deposit         → SecurityDeposit
     *   dueDay          → PaymentDueDate
     *   gracePeriod     → GracePeriod
     *   latePenalty     → LatePenalty
     *   status          → Status  (always "Active" on creation — matches Lease constructor)
     *
     * @throws IllegalStateException if the property is already occupied.
     */
    public int insertLease(Connection conn,
                           int propertyID,
                           int tenantID,
                           Date startDate,
                           Date endDate,
                           double monthlyRent,
                           double securityDeposit,
                           int paymentDueDate,
                           int gracePeriod,
                           double latePenalty) throws SQLException {

        // BUSINESS RULE: Prevent leasing an already-occupied property.
        // Delegates the occupancy check to PropertyDAO.
        PropertyDAO propertyDAO = new PropertyDAO();
        if (propertyDAO.isOccupied(conn, propertyID)) {
            throw new IllegalStateException(
                    "Property #" + propertyID + " is already occupied. "
                            + "Terminate the existing lease before creating a new one.");
        }

        // BUSINESS RULE: startDate must be before endDate
        // (Mirrors the validation in LeaseService.createLease)
        if (!startDate.toLocalDate().isBefore(endDate.toLocalDate())) {
            throw new IllegalArgumentException(
                    "Lease start date must be before end date.");
        }

        String sql = "INSERT INTO Lease "
                + "(PropertyID, TenantID, StartDate, EndDate, MonthlyRent, "
                + " Deposit, DueDate, GracePeriod, LatePenalty, Status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'Active')";

        // Status is hardcoded to 'Active' — matches Lease constructor behaviour:
        //   this.status = "Active";  (Lease.java line: this.status = "Active")

        int newLeaseID = -1;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, propertyID);
            ps.setInt(2, tenantID);
            ps.setDate(3, startDate);
            ps.setDate(4, endDate);
            ps.setDouble(5, monthlyRent);
            ps.setDouble(6, securityDeposit);
            ps.setInt(7, paymentDueDate);
            ps.setInt(8, gracePeriod);
            ps.setDouble(9, latePenalty);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                newLeaseID = keys.getInt(1);
            }
        }

        // Mark the property as Occupied now that a lease exists.
        propertyDAO.updateStatus(conn, propertyID, "Occupied");

        return newLeaseID;
    }

    // ─────────────────────────────────────────────────────────────
    // TERMINATE — End an active lease early
    // Mirrors Lease.terminate() and LeaseService.terminateLease().
    // Sets Status = "Terminated" and EndDate = today in the DB.
    // Also releases the property back to "Available".
    // ─────────────────────────────────────────────────────────────

    /**
     * Terminates an active lease by ID.
     * Sets Status = "Terminated" and EndDate = today (mirrors Lease.terminate()).
     * Frees the property so a new lease can be created on it.
     *
     * Returns true if a row was updated, false if leaseID was not found.
     */
    public boolean terminateLease(Connection conn, int leaseID) throws SQLException {

        // First get the PropertyID so we can release it after terminating
        int propertyID = getPropertyIDForLease(conn, leaseID);
        if (propertyID == -1) {
            return false; // Lease not found
        }

        String sql = "UPDATE Lease "
                + "SET Status = 'Terminated', EndDate = ? "
                + "WHERE LeaseID = ? AND Status = 'Active'";

        int rows;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // EndDate = today — mirrors Lease.terminate(): this.endDate = LocalDate.now()
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            ps.setInt(2, leaseID);
            rows = ps.executeUpdate();
        }

        if (rows > 0) {
            // Release the property so it can be leased again
            new PropertyDAO().updateStatus(conn, propertyID, "Available");
            return true;
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE — Modify lease terms
    // Mirrors LeaseService.updateMonthlyRent() and the broader
    // "update lease terms" requirement.
    // ─────────────────────────────────────────────────────────────

    /**
     * Updates the financial terms of an existing lease.
     * Only the fields that can legitimately change mid-lease are included.
     * Returns the number of rows updated.
     */
    public int updateLeaseTerm(Connection conn,
                               int leaseID,
                               double monthlyRent,
                               double securityDeposit,
                               int paymentDueDate,
                               int gracePeriod,
                               double latePenalty,
                               Date endDate) throws SQLException {

        String sql = "UPDATE Lease "
                + "SET MonthlyRent = ?, Deposit = ?, "
                + "    DueDate = ?, GracePeriod = ?, "
                + "    LatePenalty = ?, EndDate = ? "
                + "WHERE LeaseID = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, monthlyRent);
            ps.setDouble(2, securityDeposit);
            ps.setInt(3, paymentDueDate);
            ps.setInt(4, gracePeriod);
            ps.setDouble(5, latePenalty);
            ps.setDate(6, endDate);
            ps.setInt(7, leaseID);
            return ps.executeUpdate();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY — Check for active lease on a property
    // Used by insertLease() and the UI to prevent double-leasing.
    // Mirrors the IF EXISTS check from DBConnector.java.
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns true if the property already has an Active lease.
     * PropertyDAO.isOccupied() checks the Status column, while this
     * method queries the Lease table directly — use either as needed.
     */
    public boolean hasActiveLease(Connection conn, int propertyID) throws SQLException {
        String sql = "SELECT 1 FROM Lease WHERE PropertyID = ? AND Status = 'Active'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, propertyID);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    /**
     * Returns true if the TENANT has any active lease.
     * Used by TenantDAO to block deletion of tenants with live leases.
     */
    public boolean tenantHasActiveLease(Connection conn, int tenantID) throws SQLException {
        String sql = "SELECT 1 FROM Lease WHERE TenantID = ? AND Status = 'Active'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantID);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY — Retrieve PropertyID for a given lease
    // Internal helper used by terminateLease().
    // ─────────────────────────────────────────────────────────────

    private int getPropertyIDForLease(Connection conn, int leaseID) throws SQLException {
        String sql = "SELECT PropertyID FROM Lease WHERE LeaseID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, leaseID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("PropertyID");
        }
        return -1; // Not found
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY — Read leases for display in LeasePanel table
    // Columns match the panel:
    //   {LeaseID, Tenant, Property, StartDate, EndDate,
    //    Rent/Month, Deposit, Status}
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns all leases as display rows for the LeasePanel table.
     * Joins Tenant and Property to show names instead of raw IDs.
     */
    public List<Object[]> getAllLeasesForTable(Connection conn) throws SQLException {
        List<Object[]> rows = new ArrayList<>();

        String sql = "SELECT l.LeaseID, "
                + "       CONCAT(t.FirstName, ' ', t.LastName) AS TenantName, "
                + "       p.Address AS PropertyAddress, "
                + "       l.StartDate, l.EndDate, "
                + "       l.MonthlyRent, l.Deposit, l.Status "
                + "FROM Lease l "
                + "JOIN Tenant t ON l.TenantID = t.TenantID "
                + "JOIN Property p ON l.PropertyID = p.PropertyID "
                + "ORDER BY l.LeaseID DESC"; // Most recent first

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                        rs.getInt("LeaseID"),
                        rs.getString("TenantName"),
                        rs.getString("PropertyAddress"),
                        rs.getDate("StartDate"),
                        rs.getDate("EndDate"),
                        String.format("N$ %.2f", rs.getDouble("MonthlyRent")),
                        String.format("N$ %.2f", rs.getDouble("Deposit")),
                        rs.getString("Status")
                });
            }
        }
        return rows;
    }

    /**
     * Loads a single lease row by ID.
     * Used to pre-populate the edit dialog in LeasePanel.
     * Caller is responsible for closing the ResultSet.
     */
    public ResultSet getLeaseById(Connection conn, int leaseID) throws SQLException {
        String sql = "SELECT * FROM Lease WHERE LeaseID = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, leaseID);
        return ps.executeQuery();
    }
    public boolean terminateLease(Connection conn, int leaseID, int propertyID) throws SQLException {
        conn.setAutoCommit(false);
        String updateLease = "UPDATE Lease SET Status = 'Terminated', EndDate = GETDATE() WHERE LeaseID = ?";
        String updateProperty = "UPDATE Property SET Status = 'Available' WHERE PropertyID = ?";

        try (PreparedStatement psLease = conn.prepareStatement(updateLease);
             PreparedStatement psProp = conn.prepareStatement(updateProperty)) {

            psLease.setInt(1, leaseID);
            psLease.executeUpdate();

            psProp.setInt(1, propertyID);
            psProp.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}