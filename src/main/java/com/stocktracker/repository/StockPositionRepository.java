package com.stocktracker.repository;

import com.stocktracker.entity.StockPosition;
import com.stocktracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockPositionRepository extends JpaRepository<StockPosition, Long> {
    List<StockPosition> findByUser(User user);
    void deleteByUserAndId(User user, Long id);
}
