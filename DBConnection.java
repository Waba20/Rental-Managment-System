import java.sql.*;

/**
 * DBConnection — Singleton helper for SQL Server Management Studio.
 *
 * Update SERVER, DATABASE, USER, and PASSWORD to match your SSMS instance.
 * Uses the Microsoft JDBC Driver for SQL Server (mssql-jdbc).
 *
 * Add the driver JAR to your project:
 *   https://learn.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server
 */
public class DBConnection {

    // ── Connection settings — update these to match your SSMS instance ──────
    private static final String SERVER   = "localhost";          // or 127.0.0.1\SQLEXPRESS
    private static final String DATABASE = "PropertyRentalDB";    // your DB name
    private static final String USER     = "sa";                 // SQL Server login
    private static final String PASSWORD = "YourPassword123";    // SQL Server password

    private static final String URL = "jdbc:sqlserver://localhost\\SQLEXPRESS;"
            + "databaseName=PropertyRentalDB;"
            + "integratedSecurity=true;"
            + "trustServerCertificate=true;";

    private static Connection instance = null;

    /**
     * Returns a singleton Connection to SQL Server.
     * Re-opens the connection automatically if it was closed.
     */
    public static Connection getConnection() throws SQLException {
        if (instance == null || instance.isClosed()) {
            try {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            } catch (ClassNotFoundException e) {
                throw new SQLException(
                        "SQL Server JDBC driver not found.\n"
                        + "Add mssql-jdbc-*.jar to your project's classpath.", e);
            }
            instance = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return instance;
    }

    /**
     * Closes the singleton connection (call on application exit).
     */
    public static void closeConnection() {
        if (instance != null) {
            try { instance.close(); } catch (SQLException ignored) {}
            instance = null;
        }
    }

    /** Silently closes a ResultSet — use in finally blocks. */
    public static void close(ResultSet rs) {
        if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
    }

    /** Silently closes a PreparedStatement — use in finally blocks. */
    public static void close(PreparedStatement ps) {
        if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
    }
}
