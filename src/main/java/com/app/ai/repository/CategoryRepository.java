package com.app.ai.repository;

import com.app.ai.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT c FROM Category c WHERE c.name LIKE %:keyword% OR c.description LIKE %:keyword%")
    Page<Category> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM Category c LEFT JOIN c.products p GROUP BY c ORDER BY COUNT(p) DESC")
    List<Category> findCategoriesOrderByProductCount();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    Long countProductsByCategory(@Param("categoryId") Long categoryId);
}
