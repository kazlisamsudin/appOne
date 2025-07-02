package com.app.ai.controller;

import com.app.ai.model.Category;
import com.app.ai.model.Product;
import com.app.ai.model.ProductPhoto;
import com.app.ai.model.StockHistory;
import com.app.ai.service.ProductService;
import com.app.ai.service.CategoryService;
import com.app.ai.service.StockHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private StockHistoryService stockHistoryService;

    @GetMapping("/list")
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "name") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String priceRange,
            @RequestParam(required = false) Boolean inStock,
            Model model) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Apply filters if provided
        Page<Product> productPage;
        if (search != null || categoryId != null || priceRange != null || inStock != null) {
            productPage = productService.findFilteredProducts(search, categoryId, priceRange, inStock, pageable);
        } else {
            productPage = productService.findPaginatedProducts(pageable);
        }

        // Create a map of product IDs to their first photo for efficient lookup
        Map<Long, ProductPhoto> productPhotos = new HashMap<>();
        for (Product product : productPage.getContent()) {
            List<ProductPhoto> photos = productService.getProductPhotos(product);
            if (!photos.isEmpty()) {
                productPhotos.put(product.getId(), photos.get(0)); // Get first photo
            }
        }

        // Get all categories for filter dropdown
        List<Category> categories = categoryService.findAllCategories();

        model.addAttribute("productPage", productPage);
        model.addAttribute("productPhotos", productPhotos);
        model.addAttribute("categories", categories);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        model.addAttribute("search", search);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("priceRange", priceRange);
        model.addAttribute("inStock", inStock);

        return "product/list";
    }

    @GetMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String showAddForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.findAllCategories());
        return "product/form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN')")
    public String saveProduct(@ModelAttribute Product product, @RequestParam(value = "photo", required = false) MultipartFile photo, RedirectAttributes redirectAttributes) {
        boolean isNew = (product.getId() == null);
        Product savedProduct = productService.saveProduct(product);
        if (photo != null && !photo.isEmpty()) {
            try {
                productService.saveProductPhoto(savedProduct, photo);
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Product saved, but failed to upload photo.");
                return "redirect:/products/list";
            }
        }
        redirectAttributes.addFlashAttribute("successMessage",
            isNew ? "Product added successfully!" : "Product updated successfully!");
        return "redirect:/products/list";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Product product = productService.findProductById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product ID: " + id));
            List<ProductPhoto> photos = productService.getProductPhotos(product);
            model.addAttribute("product", product);
            model.addAttribute("photos", photos);
            model.addAttribute("categories", categoryService.findAllCategories());
            return "product/form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/products/list";
        }
    }

    @GetMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting product: " + e.getMessage());
        }
        return "redirect:/products/list";
    }

    @GetMapping("/view/{id}")
    public String viewProduct(@PathVariable Long id, Model model) {
        Optional<Product> productOpt = productService.findProductById(id);
        if (productOpt.isEmpty()) {
            return "redirect:/products/list";
        }
        Product product = productOpt.get();
        List<ProductPhoto> photos = productService.getProductPhotos(product);

        // Get stock history for this product
        List<StockHistory> stockHistory = stockHistoryService.getProductStockHistory(id);

        model.addAttribute("product", product);
        model.addAttribute("photos", photos);
        model.addAttribute("stockHistory", stockHistory);
        return "product/view";
    }

    @PostMapping("/upload-photo/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String uploadProductPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        Optional<Product> productOpt = productService.findProductById(id);
        if (productOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Product not found.");
            return "redirect:/products/view/" + id;
        }
        try {
            productService.saveProductPhoto(productOpt.get(), file);
            redirectAttributes.addFlashAttribute("successMessage", "Photo uploaded successfully!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload photo.");
        }
        return "redirect:/products/view/" + id;
    }

    @GetMapping("/photo/{photoId}")
    public ResponseEntity<byte[]> getProductPhoto(@PathVariable Long photoId) {
        Optional<ProductPhoto> photoOpt = productService.getProductPhotoById(photoId);
        if (photoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ProductPhoto photo = photoOpt.get();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + photo.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(photo.getFileType()))
                .body(photo.getData());
    }

    @PostMapping("/photo/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteProductPhoto(@PathVariable("id") Long photoId, RedirectAttributes redirectAttributes) {
        Optional<ProductPhoto> photoOpt = productService.getProductPhotoById(photoId);
        if (photoOpt.isPresent()) {
            Product product = photoOpt.get().getProduct();
            productService.deleteProductPhoto(photoId);
            redirectAttributes.addFlashAttribute("successMessage", "Photo removed successfully!");
            return "redirect:/products/view/" + product.getId();
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Photo not found.");
            return "redirect:/products/list";
        }
    }
}
