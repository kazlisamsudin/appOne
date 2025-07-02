package com.app.ai.controller;

import com.app.ai.model.Category;
import com.app.ai.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.Optional;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/list")
    public String listCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            Model model) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Category> categoryPage;
        if (search != null && !search.trim().isEmpty()) {
            categoryPage = categoryService.searchCategories(search, pageable);
            model.addAttribute("search", search);
        } else {
            categoryPage = categoryService.findPaginatedCategories(pageable);
        }

        model.addAttribute("categoryPage", categoryPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        return "category/list";
    }

    @GetMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String showAddForm(Model model) {
        model.addAttribute("category", new Category());
        return "category/form";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Category> categoryOpt = categoryService.findCategoryById(id);
        if (categoryOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Category not found.");
            return "redirect:/categories/list";
        }
        model.addAttribute("category", categoryOpt.get());
        return "category/form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN')")
    public String saveCategory(@Valid @ModelAttribute Category category, BindingResult result,
                              RedirectAttributes redirectAttributes, Model model) {

        // Check for duplicate category name
        if (category.getId() == null && categoryService.existsByName(category.getName())) {
            result.rejectValue("name", "error.category", "Category name already exists");
        }

        if (result.hasErrors()) {
            return "category/form";
        }

        try {
            categoryService.saveCategory(category);
            String message = category.getId() == null ? "Category added successfully!" : "Category updated successfully!";
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving category: " + e.getMessage());
        }

        return "redirect:/categories/list";
    }

    @GetMapping("/view/{id}")
    public String viewCategory(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Category> categoryOpt = categoryService.findCategoryById(id);
        if (categoryOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Category not found.");
            return "redirect:/categories/list";
        }

        Category category = categoryOpt.get();
        Long productCount = categoryService.getProductCountByCategory(id);

        model.addAttribute("category", category);
        model.addAttribute("productCount", productCount);
        return "category/view";
    }

    @GetMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long productCount = categoryService.getProductCountByCategory(id);
            if (productCount > 0) {
                redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot delete category. It contains " + productCount + " products.");
                return "redirect:/categories/list";
            }

            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("successMessage", "Category deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting category: " + e.getMessage());
        }
        return "redirect:/categories/list";
    }
}
