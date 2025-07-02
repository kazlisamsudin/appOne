package com.app.ai.repository;

import com.app.ai.model.ProductPhoto;
import com.app.ai.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductPhotoRepository extends JpaRepository<ProductPhoto, Long> {
    List<ProductPhoto> findByProduct(Product product);
}

