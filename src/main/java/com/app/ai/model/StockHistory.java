package com.app.ai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_history")
public class StockHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "previous_quantity", nullable = false)
    private Integer previousQuantity;

    @Column(name = "new_quantity", nullable = false)
    private Integer newQuantity;

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange; // negative for decrease, positive for increase

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private ChangeType changeType;

    @Column(name = "reason")
    private String reason;

    @Column(name = "changed_by")
    private String changedBy; // username or system

    @Column(name = "change_date", nullable = false)
    private LocalDateTime changeDate;

    @Column(name = "order_id")
    private Long orderId; // if change was due to an order

    public enum ChangeType {
        MANUAL_ADJUSTMENT,    // Admin manually updated stock
        ORDER_PURCHASE,       // Stock reduced due to order
        ORDER_CANCELLATION,   // Stock restored due to order cancellation
        INVENTORY_RESTOCK,    // Stock increased due to new inventory
        DAMAGE_LOSS,          // Stock reduced due to damage/loss
        RETURN_REFUND         // Stock increased due to product return
    }

    // Constructors
    public StockHistory() {}

    public StockHistory(Product product, Integer previousQuantity, Integer newQuantity,
                       ChangeType changeType, String reason, String changedBy) {
        this.product = product;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
        this.quantityChange = newQuantity - previousQuantity;
        this.changeType = changeType;
        this.reason = reason;
        this.changedBy = changedBy;
        this.changeDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getPreviousQuantity() {
        return previousQuantity;
    }

    public void setPreviousQuantity(Integer previousQuantity) {
        this.previousQuantity = previousQuantity;
    }

    public Integer getNewQuantity() {
        return newQuantity;
    }

    public void setNewQuantity(Integer newQuantity) {
        this.newQuantity = newQuantity;
    }

    public Integer getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(Integer quantityChange) {
        this.quantityChange = quantityChange;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(LocalDateTime changeDate) {
        this.changeDate = changeDate;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}
