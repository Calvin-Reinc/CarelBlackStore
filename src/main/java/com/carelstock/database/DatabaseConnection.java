/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.carelstock.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author squid
 */


public class DatabaseConnection {
    // Database connection parameters
    private static final String URL = "jdbc:postgresql://localhost:5432/stockflow";
    private static final String USER = "postgres";
    // TODO: In production, store credentials securely (e.g., environment variables or configuration files)
    private static final String PASSWORD = "740325"; 

    // Static method to obtain a connection
    public static Connection getConnection() throws SQLException {
        try {
            // Optional: explicitly load the driver (modern JDBC drivers load automatically via SPI, but it's good practice to ensure it's registered)
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC Driver not found in classpath.", e);
        }
        
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}