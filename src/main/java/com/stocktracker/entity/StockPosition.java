package com.stocktracker.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_positions")
public class StockPosition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String symbol;
    
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal quantity;
    
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal purchasePrice;
    
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal actualPrice;
    
    @Column(precision = 12, scale = 4)
    private BigDecimal actualValue;
    
    @Column(nullable = false)
    private LocalDate purchaseDate;
    
    @Column
    private LocalDate lastPriceUpdate;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Transient
    private BigDecimal profit;
    
    public StockPosition() {}
    
    public StockPosition(User user, String symbol, BigDecimal quantity, BigDecimal purchasePrice, BigDecimal actualPrice, LocalDate purchaseDate) {
        this.user = user;
        this.symbol = symbol;
        this.quantity = quantity != null ? quantity : BigDecimal.ZERO;
        this.purchasePrice = purchasePrice != null ? purchasePrice : BigDecimal.ZERO;
        this.actualPrice = actualPrice != null ? actualPrice : BigDecimal.ZERO;
        this.purchaseDate = purchaseDate != null ? purchaseDate : LocalDate.now();
        this.lastPriceUpdate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
        this.actualValue = this.actualPrice.multiply(this.quantity);
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public BigDecimal getQuantity() {
        return quantity;
    }
    
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }
    
    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }
    
    public BigDecimal getActualPrice() {
        return actualPrice;
    }
    
    public void setActualPrice(BigDecimal actualPrice) {
        this.actualPrice = actualPrice;
    }
    
    public BigDecimal getActualValue() {
        return actualValue;
    }
    
    public void setActualValue(BigDecimal actualValue) {
        this.actualValue = actualValue;
    }
    
    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }
    
    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
    
    public LocalDate getLastPriceUpdate() {
        return lastPriceUpdate;
    }
    
    public void setLastPriceUpdate(LocalDate lastPriceUpdate) {
        this.lastPriceUpdate = lastPriceUpdate;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public BigDecimal getProfit() {
        if (profit == null && actualPrice != null && purchasePrice != null && quantity != null) {
            this.profit = actualPrice.subtract(purchasePrice).multiply(quantity);
        }
        return profit;
    }
    
    public void calculateActualValue() {
        this.actualValue = actualPrice.multiply(quantity);
    }
}
