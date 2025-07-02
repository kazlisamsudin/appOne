package com.app.ai.repository;

import com.app.ai.model.OrderItem;
import com.app.ai.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder(Order order);

    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT p.name, SUM(oi.quantity) as totalSold " +
           "FROM OrderItem oi JOIN oi.product p " +
           "GROUP BY p.id, p.name " +
           "ORDER BY totalSold DESC")
    List<Object[]> findMostPopularProducts();

    @Query("SELECT p.name, SUM(oi.totalPrice) as totalRevenue " +
           "FROM OrderItem oi JOIN oi.product p " +
           "GROUP BY p.id, p.name " +
           "ORDER BY totalRevenue DESC")
    List<Object[]> findTopRevenueProducts();
}
