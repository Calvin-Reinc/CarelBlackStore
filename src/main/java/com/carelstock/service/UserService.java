/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.carelstock.service;

import com.carelstock.dao.UserDAO;
import com.carelstock.model.User;
/**
 *
 * @author squid
 */



public class UserService {
    private final UserDAO userDAO;
    
    public UserService() {
        this.userDAO = new UserDAO();
    }

    // Constructor injection for testing/mocking
    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // Register a new user with input validation
    public boolean registerUser(String username, String plainPassword, String role) {
        if (username == null || username.trim().length() < 3) {
            return false; // Username must be at least 3 characters
        }
        if (plainPassword == null || plainPassword.length() < 8) {
            return false; // Enforce minimum password length for security
        }

        // Prevent duplicate usernames
        if (userDAO.getUserByUsername(username.trim()) != null) {
            return false;
        }

        return userDAO.createUser(username.trim(), plainPassword, role);
    }

    // Authenticate credentials and return the User object if valid
    public User authenticate(String username, String plainPassword) {
        if (username == null || username.trim().isEmpty() || plainPassword == null || plainPassword.isEmpty()) {
            return null;
        }

        User user = userDAO.getUserByUsername(username.trim());
        if (user == null) {
            return null;
        }

        if (userDAO.verifyPassword(plainPassword, user.getPasswordHash())) {
            return user;
        }

        return null;
    }

    public User getUserByUsername(String username) {
        return userDAO.getUserByUsername(username);
    }
}
