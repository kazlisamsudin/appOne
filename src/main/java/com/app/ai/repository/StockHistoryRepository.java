package com.app.ai.repository;

import com.app.ai.model.StockHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {

    // Find stock history for a specific product
    List<StockHistory> findByProductIdOrderByChangeDateDesc(Long productId);

    // Find stock history for a specific product with pagination
    Page<StockHistory> findByProductIdOrderByChangeDateDesc(Long productId, Pageable pageable);

    // Find recent stock changes across all products
    Page<StockHistory> findAllByOrderByChangeDateDesc(Pageable pageable);

    // Find stock changes by change type
    List<StockHistory> findByChangeTypeOrderByChangeDateDesc(StockHistory.ChangeType changeType);

    // Find stock changes within date range
    @Query("SELECT sh FROM StockHistory sh WHERE sh.changeDate BETWEEN :startDate AND :endDate ORDER BY sh.changeDate DESC")
    List<StockHistory> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    // Find stock changes by specific user
    List<StockHistory> findByChangedByOrderByChangeDateDesc(String changedBy);

    // Get summary of stock changes for a product
    @Query("SELECT COUNT(sh), SUM(sh.quantityChange) FROM StockHistory sh WHERE sh.product.id = :productId")
    Object[] getStockChangeSummary(@Param("productId") Long productId);
}
