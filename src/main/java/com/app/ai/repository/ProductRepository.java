package com.app.ai.repository;

import com.app.ai.model.Product;
import com.app.ai.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory(Category category);

    List<Product> findByNameContaining(String name);

    @Query("SELECT p FROM Product p WHERE " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:inStock IS NULL OR (:inStock = true AND p.quantity > 0) OR (:inStock = false AND p.quantity = 0)) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> findFilteredProducts(@Param("search") String search,
                                     @Param("categoryId") Long categoryId,
                                     @Param("inStock") Boolean inStock,
                                     @Param("minPrice") BigDecimal minPrice,
                                     @Param("maxPrice") BigDecimal maxPrice,
                                     Pageable pageable);

    // Overloaded method for price range string parsing
    default Page<Product> findFilteredProducts(String search, Long categoryId, String priceRange, Boolean inStock, Pageable pageable) {
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;

        if (priceRange != null && !priceRange.isEmpty()) {
            switch (priceRange) {
                case "0-25":
                    minPrice = BigDecimal.ZERO;
                    maxPrice = new BigDecimal("25");
                    break;
                case "25-50":
                    minPrice = new BigDecimal("25");
                    maxPrice = new BigDecimal("50");
                    break;
                case "50-100":
                    minPrice = new BigDecimal("50");
                    maxPrice = new BigDecimal("100");
                    break;
                case "100-200":
                    minPrice = new BigDecimal("100");
                    maxPrice = new BigDecimal("200");
                    break;
                case "200+":
                    minPrice = new BigDecimal("200");
                    break;
            }
        }

        return findFilteredProducts(search, categoryId, inStock, minPrice, maxPrice, pageable);
    }
}
