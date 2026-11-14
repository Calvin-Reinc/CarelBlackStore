/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.carelstock.config;

import com.carelstock.service.UserService;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
/**
 *
 * @author squid
 */


@WebListener
public class AdminInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        UserService userService = new UserService();
        String defaultAdmin = "admin";
        
        if (userService.getUserByUsername(defaultAdmin) == null) {
            boolean created = userService.registerUser(defaultAdmin, "Admin@CarelStock2026", "ADMIN");
            if (created) {
                System.out.println("[CarelStock Security] Default administrator account provisioned.");
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Cleanup if needed
    }
}
