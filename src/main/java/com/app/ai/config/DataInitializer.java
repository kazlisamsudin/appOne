package com.app.ai.config;

import com.app.ai.model.Role;
import com.app.ai.model.User;
import com.app.ai.model.Category;
import com.app.ai.model.Product;
import com.app.ai.repository.RoleRepository;
import com.app.ai.repository.UserRepository;
import com.app.ai.repository.CategoryRepository;
import com.app.ai.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== DataInitializer: Starting data initialization ===");

        // Create roles if they don't exist
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ADMIN");
                    Role saved = roleRepository.save(role);
                    System.out.println("Created ADMIN role with ID: " + saved.getId());
                    return saved;
                });

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("USER");
                    Role saved = roleRepository.save(role);
                    System.out.println("Created USER role with ID: " + saved.getId());
                    return saved;
                });

        System.out.println("Roles available - ADMIN: " + adminRole.getId() + ", USER: " + userRole.getId());

        // Create admin user if doesn't exist
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@example.com");
            String encodedPassword = passwordEncoder.encode("admin123");
            admin.setPassword(encodedPassword);
            admin.setEnabled(true);
            admin.setRoles(Set.of(adminRole, userRole));
            User savedAdmin = userRepository.save(admin);
            System.out.println("=== ADMIN USER CREATED ===");
            System.out.println("Username: admin");
            System.out.println("Password: admin123");
            System.out.println("Encoded password: " + encodedPassword);
            System.out.println("User ID: " + savedAdmin.getId());
            System.out.println("Roles: " + savedAdmin.getRoles().size());
        } else {
            System.out.println("Admin user already exists");
        }

        // Create test user if doesn't exist
        if (!userRepository.existsByUsername("user")) {
            User testUser = new User();
            testUser.setUsername("user");
            testUser.setEmail("user@example.com");
            String encodedPassword = passwordEncoder.encode("user123");
            testUser.setPassword(encodedPassword);
            testUser.setEnabled(true);
            testUser.setRoles(Set.of(userRole));
            User savedUser = userRepository.save(testUser);
            System.out.println("=== TEST USER CREATED ===");
            System.out.println("Username: user");
            System.out.println("Password: user123");
            System.out.println("Encoded password: " + encodedPassword);
            System.out.println("User ID: " + savedUser.getId());
        } else {
            System.out.println("Test user already exists");
        }

        // Update any existing users with plain text passwords
        updateExistingUsersPasswords();

        // Create sample categories and products
        createSampleData();

        System.out.println("=== DataInitializer: Completed data initialization ===");
    }

    private void updateExistingUsersPasswords() {
        // This method handles existing users who might have plain text passwords
        userRepository.findAll().forEach(user -> {
            String currentPassword = user.getPassword();

            // Check if password is already BCrypt encoded (BCrypt hashes start with $2a$, $2b$, or $2y$)
            if (!currentPassword.startsWith("$2a$") && !currentPassword.startsWith("$2b$") && !currentPassword.startsWith("$2y$")) {
                // Re-encode the plain text password
                user.setPassword(passwordEncoder.encode(currentPassword));
                userRepository.save(user);
                System.out.println("Updated password encoding for user: " + user.getUsername());
            }
        });
    }

    private void createSampleData() {
        System.out.println("=== Creating sample categories and products ===");

        // Check if we have CategoryRepository and ProductRepository
        try {
            // This is a simple way to create sample data - you may need to adjust based on your actual repository structure
            System.out.println("Sample data creation - Categories and Products will be created if repositories are available");

            // Create sample categories
            createSampleCategories();

            // Create sample products
            createSampleProducts();

        } catch (Exception e) {
            System.out.println("Note: Could not create sample products/categories. Repositories may not be configured yet.");
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void createSampleCategories() {
        System.out.println("Creating sample categories...");

        // Create Electronics category
        if (!categoryRepository.existsByName("Electronics")) {
            Category electronics = new Category();
            electronics.setName("Electronics");
            electronics.setDescription("Electronic devices and gadgets");
            categoryRepository.save(electronics);
            System.out.println("Created category: Electronics");
        }

        // Create Clothing category
        if (!categoryRepository.existsByName("Clothing")) {
            Category clothing = new Category();
            clothing.setName("Clothing");
            clothing.setDescription("Apparel and fashion items");
            categoryRepository.save(clothing);
            System.out.println("Created category: Clothing");
        }

        // Create Books category
        if (!categoryRepository.existsByName("Books")) {
            Category books = new Category();
            books.setName("Books");
            books.setDescription("Books and literature");
            categoryRepository.save(books);
            System.out.println("Created category: Books");
        }

        // Create Home & Garden category
        if (!categoryRepository.existsByName("Home & Garden")) {
            Category homeGarden = new Category();
            homeGarden.setName("Home & Garden");
            homeGarden.setDescription("Home improvement and garden supplies");
            categoryRepository.save(homeGarden);
            System.out.println("Created category: Home & Garden");
        }
    }

    private void createSampleProducts() {
        System.out.println("Creating sample products...");

        // Get categories
        Category electronics = categoryRepository.findByName("Electronics").orElse(null);
        Category clothing = categoryRepository.findByName("Clothing").orElse(null);
        Category books = categoryRepository.findByName("Books").orElse(null);
        Category homeGarden = categoryRepository.findByName("Home & Garden").orElse(null);

        // Create Electronics products
        if (electronics != null) {
            createProductIfNotExists("Smartphone", "Latest model smartphone with advanced features",
                                   new BigDecimal("699.99"), electronics, 50);
            createProductIfNotExists("Laptop", "High-performance laptop for work and gaming",
                                   new BigDecimal("1299.99"), electronics, 25);
            createProductIfNotExists("Wireless Headphones", "Noise-canceling wireless headphones",
                                   new BigDecimal("199.99"), electronics, 100);
        }

        // Create Clothing products
        if (clothing != null) {
            createProductIfNotExists("T-Shirt", "Comfortable cotton t-shirt",
                                   new BigDecimal("19.99"), clothing, 200);
            createProductIfNotExists("Jeans", "Classic blue denim jeans",
                                   new BigDecimal("59.99"), clothing, 150);
            createProductIfNotExists("Sneakers", "Comfortable running sneakers",
                                   new BigDecimal("89.99"), clothing, 75);
        }

        // Create Books products
        if (books != null) {
            createProductIfNotExists("Programming Guide", "Complete guide to modern programming",
                                   new BigDecimal("39.99"), books, 80);
            createProductIfNotExists("Fiction Novel", "Bestselling fiction novel",
                                   new BigDecimal("14.99"), books, 120);
            createProductIfNotExists("Cookbook", "Delicious recipes for home cooking",
                                   new BigDecimal("24.99"), books, 60);
        }

        // Create Home & Garden products
        if (homeGarden != null) {
            createProductIfNotExists("Coffee Maker", "Automatic coffee maker with timer",
                                   new BigDecimal("79.99"), homeGarden, 40);
            createProductIfNotExists("Garden Tools Set", "Complete set of garden tools",
                                   new BigDecimal("49.99"), homeGarden, 30);
            createProductIfNotExists("LED Light Bulbs", "Energy-efficient LED light bulbs (4-pack)",
                                   new BigDecimal("12.99"), homeGarden, 200);
        }

        System.out.println("Sample products creation completed!");
    }

    private void createProductIfNotExists(String name, String description, BigDecimal price,
                                        Category category, int stockQuantity) {
        // Check if product already exists (simple check by name)
        if (productRepository.findByNameContaining(name).isEmpty()) {
            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setPrice(price);
            product.setCategory(category);
            product.setStockQuantity(stockQuantity);
            product.setAvailable(true);
            productRepository.save(product);
            System.out.println("Created product: " + name + " - $" + price);
        }
    }
}
