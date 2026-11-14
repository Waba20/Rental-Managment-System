
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnector {

    private static final String URL =
            "jdbc:sqlserver://localhost\\SQLEXPRESS;"
                    + "databaseName=PropertyRentalDB;"
                    + "integratedSecurity=true;"
                    + "encrypt=true;"
                    + "trustServerCertificate=true;";

    public static Connection getConnection() throws SQLException {

        return DriverManager.getConnection(URL);
    }
}
