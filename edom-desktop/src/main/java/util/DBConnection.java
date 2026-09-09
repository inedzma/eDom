package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // 1. Dodali smo "jdbc:" na početak
    // 2. Promijenili smo "defaultdb" u "edom_db" (ili samo "edom" ako si je tako nazvala)
    // 3. Promijenili smo parametar za SSL da odgovara Java driveru (sslMode=REQUIRED)
    private static final String URL = "jdbc:mysql://localhost:3306/defaultdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static final String USERNAME = "root";// this is a placeholder username, replace it with your actual username

    private static final String PASSWORD = "Root.1234";// this is a placeholder password, replace it with your actual password

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}