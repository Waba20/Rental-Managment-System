import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PropertyDAO — Data Access Object for the Property table.
 *
 * Responsible for all database operations involving properties:
 * insert, update, delete, and queries.
 *
 * Aligns with:
 *   - Property.java   (abstract model: propertyId, address, rentAmount, isOccupied)
 *   - House.java      (subtype: bedrooms / plotSize)
 *   - Flat.java       (subtype: floorNumber)
 *   - Townhouse.java  (subtype: hasGarage / backyard)
 *   - DB schema from PropertyDAO.java:
 *       Property(PropertyID, PropertyTypeID, BuildingSize, Address, Location,
 *                MarketValue, MonthlyRental, Status)
 *       House(PropertyID, PlotSize)
 *       Townhouse(PropertyID, UnitNumber, Backyard)
 *       Flat(PropertyID, UnitNumber, FloorLevel, ElevatorAccess, Backyard)
 */
public class PropertyDAO {

    // ── Connection ────────────────────────────────────────────────
    // Every method receives a Connection rather than opening its own,
    // so the caller (panel / service) controls the connection lifecycle.

    // ── PROPERTY TYPE CONSTANTS ───────────────────────────────────
    // Match the PropertyTypeID values used in PropertyDAO.java
    public static final int TYPE_HOUSE      = 1;
    public static final int TYPE_TOWNHOUSE  = 2;
    public static final int TYPE_FLAT       = 3;

    // ─────────────────────────────────────────────────────────────
    // INSERT — Register a new property
    // Returns the auto-generated PropertyID, or -1 on failure.
    //
    // Matches choice == 1 block in PropertyDAO.java.
    // After inserting into Property, a second insert goes into the
    // matching subtype table (House / Townhouse / Flat).
    // ─────────────────────────────────────────────────────────────

    /**
     * Registers a base Property row and returns its generated ID.
     * Call one of the subtype insert methods afterwards.
     */
    public int insertProperty(Connection conn,
                              int propertyTypeID,
                              double buildingSize,
                              String address,
                              String location,
                              double marketValue,
                              double monthlyRental,
                              String status) throws SQLException {

        String sql = "INSERT INTO Property "
                + "(PropertyTypeID, BuildingSize, Address, Location, "
                + " MarketValue, MonthlyRental, Status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        // RETURN_GENERATED_KEYS lets us retrieve the auto-increment PropertyID
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, propertyTypeID);
            ps.setDouble(2, buildingSize);
            ps.setString(3, address);
            ps.setString(4, location);
            ps.setDouble(5, marketValue);
            ps.setDouble(6, monthlyRental);
            ps.setString(7, status);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1); // Return the new PropertyID
            }
        }
        return -1; // Should not reach here on success
    }

    /**
     * Inserts into the House subtype table.
     * Maps to House(PropertyID, PlotSize).
     * The Property model stores plot size as the equivalent of "bedrooms"
     * for this subtype — here we use the DB column PlotSize directly.
     */
    public void insertHouse(Connection conn,
                            int propertyID,
                            double plotSize) throws SQLException {

        String sql = "INSERT INTO House (PropertyID, PlotSize) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, propertyID);
            ps.setDouble(2, plotSize);
            ps.executeUpdate();
        }
    }

    /**
     * Inserts into the Townhouse subtype table.
     * Maps to Townhouse(PropertyID, UnitNumber, Backyard).
     * The Townhouse model's hasGarage flag is stored as the Backyard column
     * using "yes"/"no" strings to match the existing schema convention.
     */
    public void insertTownhouse(Connection conn,
                                int propertyID,
                                String unitNumber,
                                boolean hasGarage) throws SQLException {

        String sql = "INSERT INTO Townhouse (PropertyID, UnitNumber, Backyard) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, propertyID);
            ps.setString(2, unitNumber);
            // Townhouse model uses boolean hasGarage; DB stores "yes"/"no"
            ps.setString(3, hasGarage ? "yes" : "no");
            ps.executeUpdate();
        }
    }

    /**
     * Inserts into the Flat subtype table.
     * Maps to Flat(PropertyID, UnitNumber, FloorLevel, ElevatorAccess, Backyard).
     * Flat model's floorNumber maps to FloorLevel.
     */
    public void insertFlat(Connection conn,
                           int propertyID,
                           String unitNumber,
                           int floorLevel,
                           boolean elevatorAccess,
                           boolean backyard) throws SQLException {

        String sql = "INSERT INTO Flat "
                + "(PropertyID, UnitNumber, FloorLevel, ElevatorAccess, Backyard) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, propertyID);
            ps.setString(2, unitNumber);
            ps.setInt(3, floorLevel);
            ps.setString(4, elevatorAccess ? "yes" : "no");
            ps.setString(5, backyard ? "yes" : "no");
            ps.executeUpdate();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE — Modify an existing property
    // Matches choice == 2 block in PropertyDAO.java.
    // Returns the number of rows updated (1 = success, 0 = not found).
    // ─────────────────────────────────────────────────────────────

    /**
     * Updates the core Property fields for an existing property.
     * The Status field is used by LeaseDAO to flip "Available"/"Occupied"
     * when a lease is created or terminated.
     */
    public int updateProperty(Connection conn,
                              int propertyID,
                              double buildingSize,
                              String address,
                              String location,
                              double marketValue,
                              double monthlyRental,
                              String status) throws SQLException {

        String sql = "UPDATE Property "
                + "SET BuildingSize = ?, Address = ?, Location = ?, "
                + "    MarketValue = ?, MonthlyRental = ?, Status = ? "
                + "WHERE PropertyID = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, buildingSize);
            ps.setString(2, address);
            ps.setString(3, location);
            ps.setDouble(4, marketValue);
            ps.setDouble(5, monthlyRental);
            ps.setString(6, status);
            ps.setInt(7, propertyID);
            return ps.executeUpdate();
        }
    }

    /**
     * Convenience method: updates ONLY the Status column.
     * Used by LeaseDAO to mark a property "Occupied" when a lease is created,
     * and "Available" when a lease is terminated — preventing double-leasing.
     * This satisfies the requirement: "prevent leasing of already-occupied properties."
     */
    public int updateStatus(Connection conn,
                            int propertyID,
                            String status) throws SQLException {

        String sql = "UPDATE Property SET Status = ? WHERE PropertyID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, propertyID);
            return ps.executeUpdate();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE — Remove a property
    // Matches choice == 3 block in PropertyDAO.java.
    // ─────────────────────────────────────────────────────────────

    /**
     * Deletes a property by ID.
     * The DB should have ON DELETE CASCADE on subtype tables (House/Flat/Townhouse)
     * so those rows are removed automatically.
     */
    public int deleteProperty(Connection conn, int propertyID) throws SQLException {
        String sql = "DELETE FROM Property WHERE PropertyID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, propertyID);
            return ps.executeUpdate();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY — Check occupancy before creating a lease
    // Satisfies: "prevent leasing of already-occupied properties."
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns true if the property's Status is "Occupied".
     * LeaseDAO calls this before creating any new lease.
     */
    public boolean isOccupied(Connection conn, int propertyID) throws SQLException {
        String sql = "SELECT Status FROM Property WHERE PropertyID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, propertyID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return "Occupied".equalsIgnoreCase(rs.getString("Status"));
            }
        }
        return false; // Property not found — treat as not occupied
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY — Read properties for display in PropertyPanel table
    // Returns a list of raw Object[] rows matching the panel columns:
    //   {ID, Type, Address, Location, Rent/Month, MarketValue, Status}
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns all properties as rows ready for the PropertyPanel table model.
     * Joins with the PropertyType table to get the type name (House/Flat/Townhouse).
     */
    public List<Object[]> getAllPropertiesForTable(Connection conn) throws SQLException {
        List<Object[]> rows = new ArrayList<>();

        String sql = "SELECT p.PropertyID, pt.TypeName, p.Address, p.Location, "
                + "       p.MonthlyRental, p.MarketValue, p.Status "
                + "FROM Property p "
                + "JOIN PropertyType pt ON p.PropertyTypeID = pt.PropertyTypeID "
                + "ORDER BY p.PropertyID";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                        rs.getInt("PropertyID"),
                        rs.getString("TypeName"),
                        rs.getString("Address"),
                        rs.getString("Location"),
                        String.format("N$ %.2f", rs.getDouble("MonthlyRental")),
                        String.format("N$ %.2f", rs.getDouble("MarketValue")),
                        rs.getString("Status")
                });
            }
        }
        return rows;
    }

    /**
     * Loads a single property row by ID.
     * Returns null if not found.
     * Used to pre-populate the edit dialog in PropertyPanel.
     */
    public ResultSet getPropertyById(Connection conn, int propertyID) throws SQLException {
        String sql = "SELECT * FROM Property WHERE PropertyID = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, propertyID);
        return ps.executeQuery(); // Caller is responsible for closing
    }
}