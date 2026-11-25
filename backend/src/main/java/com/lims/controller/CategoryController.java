package com.lims.controller;

import com.lims.entity.Category;
import com.lims.repository.CategoryRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class CategoryController {
    private final CategoryRepository categories;

    public CategoryController(CategoryRepository categories) {
        this.categories = categories;
    }

    @GetMapping
    public List<Category> getCategories() {
        return categories.findAll();
    }
}
