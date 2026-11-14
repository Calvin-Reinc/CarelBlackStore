/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.carelstock.service;


import com.carelstock.dao.CategoryDAO;
import com.carelstock.model.Category;
/**
 *
 * @author squid
 * 
 **/


import java.util.List;

public class CategoryService {
    private final CategoryDAO categoryDAO;

    public CategoryService() {
        this.categoryDAO = new CategoryDAO();
    }

    // Create a new category with basic validation
    public boolean createCategory(Category category) {
        if (category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
            return false;
        }
        return categoryDAO.createCategory(category);
    }

    // Get a single category by ID
    public Category getCategoryById(int categoryId) {
        return categoryDAO.getCategoryById(categoryId);
    }

    // Get all categories
    public List<Category> getAllCategories() {
        return categoryDAO.getAllCategories();
    }

    // Update an existing category
    public boolean updateCategory(Category category) {
        if (category.getCategoryId() <= 0 || category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
            return false;
        }
        return categoryDAO.updateCategory(category);
    }

    // Delete a category
    public boolean deleteCategory(int categoryId) {
        return categoryDAO.deleteCategory(categoryId);
    }
}
