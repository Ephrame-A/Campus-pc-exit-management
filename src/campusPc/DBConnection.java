package campusPc;

import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
    // URL for connecting to the MySQL server without a specific database
    private static final String BASE_DB_URL = "jdbc:mysql://127.0.0.1:2017/";
    // URL for connecting to the specific database after it's created
    private static final String FULL_DB_URL = "jdbc:mysql://127.0.0.1:2017/campus_pc_management";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "oopproject";

    /**
     * Establishes and returns a connection to the 'campus_pc_management' database.
     * @return A valid SQL Connection.
     * @throws SQLException If a database access error occurs.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(FULL_DB_URL, DB_USER, DB_PASS);
    }

    /**
     * Initializes the database by creating the 'campus_pc_management' database
     */
    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(BASE_DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {

            // 1. Create the database if it doesn't exist
            String createDbSql = "CREATE DATABASE IF NOT EXISTS campus_pc_management";
            stmt.executeUpdate(createDbSql);
            System.out.println("Database 'campus_pc_management' checked/created.");

            // Now connect to the specific database to create tables
            try (Connection dbConn = DriverManager.getConnection(FULL_DB_URL, DB_USER, DB_PASS);
                 Statement dbStmt = dbConn.createStatement()) {

                // 2. Create the students table if it doesn't exist with the new schema
                String createStudentsTableSql = "CREATE TABLE IF NOT EXISTS students_data (" +
                        "id VARCHAR(20) PRIMARY KEY," + // Changed to 'id' as PK, VARCHAR(20)
                        "f_name VARCHAR(50) NOT NULL," +
                        "l_name VARCHAR(50) NOT NULL," +
                        "department VARCHAR(50) NOT NULL," +
                        "campus VARCHAR(50) NOT NULL," +
                        "pcname VARCHAR(50) NOT NULL," +
                        "color VARCHAR(20) NOT NULL," +
                        "registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" + // Renamed to 'registration_date'
                        ")";
                dbStmt.executeUpdate(createStudentsTableSql);
                System.out.println("Table 'students' checked/created with updated schema.");

                // Removed the pc_exits table creation as per your new schema.

            } catch (SQLException e) {
                System.err.println("Error creating tables: " + e.getMessage());

            }

        } catch (SQLException e) {
            System.err.println("Error initializing database connection or creating database: " + e.getMessage());

            JOptionPane.showMessageDialog(null,
                    "Database initialization failed! Ensure MySQL server is running and credentials are correct.\nError: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1); // Exit if database setup fails crucially
        }
    }
}