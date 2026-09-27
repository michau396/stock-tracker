package com.stocktracker.service;

import com.stocktracker.entity.StockPosition;
import com.stocktracker.entity.User;
import com.stocktracker.exception.InvalidStockException;
import com.stocktracker.exception.StockNotFoundException;
import com.stocktracker.repository.StockPositionRepository;
import com.stocktracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    private final StockPositionRepository stockPositionRepository;
    private final StockService stockService;
    private final UserRepository userRepository;
    private final String defaultApiKey;

    public PortfolioService(StockPositionRepository stockPositionRepository, 
                          StockService stockService,
                          UserRepository userRepository,
                          @org.springframework.beans.factory.annotation.Value("${finnhub.api.key}") String defaultApiKey) {
        this.stockPositionRepository = stockPositionRepository;
        this.stockService = stockService;
        this.userRepository = userRepository;
        this.defaultApiKey = defaultApiKey;
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<StockPosition> getUserPositions(User user) {
        return stockPositionRepository.findByUser(user);
    }

    public StockPosition addPosition(User user, String symbol, BigDecimal quantity, BigDecimal purchasePrice, LocalDate purchaseDate) {
        // Validate input parameters
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Stock symbol cannot be empty");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (purchasePrice == null || purchasePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Purchase price cannot be negative");
        }
        if (purchaseDate == null) {
            throw new IllegalArgumentException("Purchase date cannot be null");
        }
        if (purchaseDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Purchase date cannot be in the future");
        }

        String normalizedSymbol = symbol.toUpperCase().trim();
        
        // Validate that the stock actually exists by checking its price
        try {
            Boolean stockExists = stockService.validateStockExists(normalizedSymbol, defaultApiKey).block();
            if (stockExists == null || !stockExists) {
                throw new InvalidStockException("Stock symbol '" + normalizedSymbol + "' not found or invalid. Please check the symbol and try again.");
            }
            
            BigDecimal actualPrice = stockService.getStockPrice(normalizedSymbol, defaultApiKey).block();
            if (actualPrice == null || actualPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidStockException("Invalid stock price for '" + normalizedSymbol + "'. The stock may not be actively traded.");
            }
            
            StockPosition position = new StockPosition(user, normalizedSymbol, quantity, purchasePrice, actualPrice, purchaseDate);
            return stockPositionRepository.save(position);
        } catch (InvalidStockException e) {
            throw e;
        } catch (Exception e) {
            throw new StockNotFoundException("Error adding position: " + e.getMessage());
        }
    }

    @Transactional
    public void deletePosition(User user, Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Position ID cannot be null");
        }
        stockPositionRepository.deleteByUserAndId(user, id);
    }

    public void updateStockPricesIfNeeded(User user) {
        List<StockPosition> positions = stockPositionRepository.findByUser(user);
        LocalDate today = LocalDate.now();
        
        for (StockPosition position : positions) {
            if (needsPriceUpdate(position, today)) {
                try {
                    BigDecimal newPrice = stockService.getStockPrice(position.getSymbol(), defaultApiKey).block();
                    if (newPrice != null && newPrice.compareTo(BigDecimal.ZERO) > 0) {
                        position.setActualPrice(newPrice);
                        position.setActualValue(newPrice.multiply(position.getQuantity()));
                        position.setLastPriceUpdate(today);
                        stockPositionRepository.save(position);
                    } else {
                        System.err.println("Invalid price for " + position.getSymbol() + ", skipping update");
                    }
                } catch (Exception e) {
                    System.err.println("Error updating price for " + position.getSymbol() + ": " + e.getMessage());
                }
            }
        }
    }

    private boolean needsPriceUpdate(StockPosition position, LocalDate today) {
        return position.getLastPriceUpdate() == null || position.getLastPriceUpdate().isBefore(today);
    }

    public Map<LocalDate, BigDecimal> getPortfolioValueHistory(User user, String period) {
        List<StockPosition> positions = stockPositionRepository.findByUser(user);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = getStartDate(period);
        
        Map<LocalDate, BigDecimal> history = new TreeMap<>();
        
        // Initialize with zeros for all dates in range
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            history.put(current, BigDecimal.ZERO);
            current = current.plusDays(1);
        }
        
        // For each position, add its value from its purchase date onwards
        for (StockPosition position : positions) {
            if (position.getPurchaseDate() != null) {
                LocalDate positionStart = position.getPurchaseDate();
                if (positionStart.isBefore(startDate)) {
                    positionStart = startDate;
                }
                
                LocalDate date = positionStart;
                while (!date.isAfter(endDate)) {
                    BigDecimal currentValue = history.get(date);
                    history.put(date, currentValue.add(position.getActualValue() != null ? position.getActualValue() : BigDecimal.ZERO));
                    date = date.plusDays(1);
                }
            }
        }
        
        return history;
    }
    
    private LocalDate getStartDate(String period) {
        LocalDate now = LocalDate.now();
        switch (period.toLowerCase()) {
            case "week":
                return now.minusWeeks(1);
            case "month":
                return now.minusMonths(1);
            case "year":
                return now.minusYears(1);
            default:
                return now.minusWeeks(1);
        }
    }
}
