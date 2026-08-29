package xyz.e3ndr.localrouter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class LR {
    public static Connection database;

    public static void init() throws SQLException {
        database = DriverManager.getConnection("jdbc:sqlite:db.sqlite");
    }

}
