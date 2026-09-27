package com.stocktracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktracker.dto.StockSearchResult;
import com.stocktracker.exception.InvalidStockException;
import com.stocktracker.exception.StockNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public StockService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl("https://finnhub.io/api/v1")
                .build();
        this.objectMapper = objectMapper;
    }

    public Mono<BigDecimal> getStockPrice(String symbol, String apiKey) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/quote")
                        .queryParam("symbol", symbol)
                        .queryParam("token", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parsePrice)
                .onErrorMap(e -> new StockNotFoundException("Error fetching stock data for '" + symbol + "': " + e.getMessage()));
    }

    public Mono<Boolean> validateStockExists(String symbol, String apiKey) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/quote")
                        .queryParam("symbol", symbol)
                        .queryParam("token", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parsePriceForValidation)
                .map(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                .onErrorReturn(false);
    }

    public Mono<List<StockSearchResult>> searchStocks(String query, String apiKey) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", query)
                        .queryParam("token", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseSearchResults)
                .onErrorReturn(new ArrayList<>());
    }

    private BigDecimal parsePrice(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode priceNode = root.get("c");
            
            if (priceNode == null || priceNode.isNull()) {
                throw new InvalidStockException("Stock not found or invalid");
            }
            
            BigDecimal price = new BigDecimal(priceNode.asText());
            
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidStockException("Invalid stock price: price must be greater than zero");
            }
            
            return price;
        } catch (InvalidStockException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidStockException("Failed to parse stock price: " + e.getMessage());
        }
    }

    private BigDecimal parsePriceForValidation(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode priceNode = root.get("c");
            
            if (priceNode == null || priceNode.isNull()) {
                return null;
            }
            
            return new BigDecimal(priceNode.asText());
        } catch (Exception e) {
            return null;
        }
    }

    private List<StockSearchResult> parseSearchResults(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode resultNode = root.get("result");
            
            if (resultNode == null || !resultNode.isArray()) {
                return new ArrayList<>();
            }
            
            List<StockSearchResult> results = new ArrayList<>();
            for (JsonNode item : resultNode) {
                String symbol = item.get("symbol").asText();
                String description = item.has("description") ? item.get("description").asText() : "";
                results.add(new StockSearchResult(symbol, description));
                
                if (results.size() >= 10) {
                    break;
                }
            }
            
            return results;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
