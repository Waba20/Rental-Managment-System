import java.sql.*;
import java.util.Scanner;
public class DBconn {
    public static void main(String[] args) throws SQLException {

                Scanner sc=new Scanner(System.in);
                String connectionUrl = "jdbc:sqlserver://localhost\\MSSQLSERVER03;"
                        + "databaseName=PropertyRentalDB;"
                        + "integratedSecurity=true;"
                        + "trustServerCertificate=true;";
                Connection conn = DriverManager.getConnection(connectionUrl);
    }
}
