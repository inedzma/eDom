package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // 1. Dodali smo "jdbc:" na početak
    // 2. Promijenili smo "defaultdb" u "edom_db" (ili samo "edom" ako si je tako nazvala)
    // 3. Promijenili smo parametar za SSL da odgovara Java driveru (sslMode=REQUIRED)
    private static final String URL = "jdbc:mysql://mysql-10b41bc7-size-e1bf.h.aivencloud.com:28740/edom?sslMode=REQUIRED";

    private static final String USERNAME = "avnadmin";

    private static final String PASSWORD = "AVNS_FWFxZ8Aax8kMSB59RwS";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}