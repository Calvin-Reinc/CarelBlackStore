/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.carelstock.database;

/**
 * Thrown by DAOs when a database operation fails for a reason the caller can't fix
 * (connection lost, bad SQL, constraint failure other than the expected ones).
 *
 * Unchecked on purpose: it lets DAO method signatures stay clean, and the servlets'
 * catch-all turns it into a logged, generic 500 response.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}