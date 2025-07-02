package com.app.ai.service;

import com.app.ai.model.Category;
import com.app.ai.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> findCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    public Page<Category> findPaginatedCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    public boolean existsByName(String name) {
        return categoryRepository.existsByName(name);
    }

    public Optional<Category> findByName(String name) {
        return categoryRepository.findByName(name);
    }

    public Page<Category> searchCategories(String keyword, Pageable pageable) {
        return categoryRepository.findByKeyword(keyword, pageable);
    }

    public List<Category> getCategoriesOrderedByProductCount() {
        return categoryRepository.findCategoriesOrderByProductCount();
    }

    public Long getProductCountByCategory(Long categoryId) {
        return categoryRepository.countProductsByCategory(categoryId);
    }
}
