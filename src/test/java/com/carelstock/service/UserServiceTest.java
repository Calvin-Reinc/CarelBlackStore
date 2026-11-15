/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.carelstock.service;

import com.carelstock.dao.UserDAO;
import com.carelstock.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 *
 * @author squid
 */



class UserServiceTest {

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegisterUser_InvalidUsernameLength() {
        // Username too short (< 3 characters)
        boolean result = userService.registerUser("ab", "SecurePassword123", "ADMIN");
        assertFalse(result, "Registration should fail if username is less than 3 characters.");
        verify(userDAO, never()).createUser(anyString(), anyString(), anyString());
    }

    @Test
    void testRegisterUser_InvalidPasswordLength() {
        // Password too short (< 8 characters)
        boolean result = userService.registerUser("adminUser", "short", "ADMIN");
        assertFalse(result, "Registration should fail if password is less than 8 characters.");
        verify(userDAO, never()).createUser(anyString(), anyString(), anyString());
    }

    @Test
    void testRegisterUser_DuplicateUsername() {
        // Mock existing user found in DB
        when(userDAO.getUserByUsername("existingAdmin")).thenReturn(new User());

        boolean result = userService.registerUser("existingAdmin", "SecurePassword123", "ADMIN");
        assertFalse(result, "Registration should fail if the username already exists.");
        verify(userDAO, never()).createUser(anyString(), anyString(), anyString());
    }

    @Test
    void testAuthenticate_InvalidCredentials() {
        when(userDAO.getUserByUsername("unknown")).thenReturn(null);

        User authenticated = userService.authenticate("unknown", "password123");
        assertNull(authenticated, "Authentication should return null for non-existent users.");
    }
}
