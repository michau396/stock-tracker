package com.stocktracker.controller;

import com.stocktracker.dto.StockSearchResult;
import com.stocktracker.entity.StockPosition;
import com.stocktracker.entity.User;
import com.stocktracker.exception.StockNotFoundException;
import com.stocktracker.service.PortfolioService;
import com.stocktracker.service.StockService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
public class StockController {

    private final StockService stockService;
    private final PortfolioService portfolioService;
    private final String defaultApiKey;

    public StockController(StockService stockService, 
                          PortfolioService portfolioService,
                          @Value("${finnhub.api.key}") String defaultApiKey) {
        this.stockService = stockService;
        this.portfolioService = portfolioService;
        this.defaultApiKey = defaultApiKey;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/portfolio")
    public String portfolio(@AuthenticationPrincipal OAuth2User oauth2User, Model model) {
        String email = oauth2User.getAttribute("email");
        User user = portfolioService.getUserByEmail(email);
        
        // Update stock prices if needed (once per day)
        portfolioService.updateStockPricesIfNeeded(user);
        
        List<StockPosition> positions = portfolioService.getUserPositions(user);
        
        // Calculate actualValue on-the-fly if null
        for (StockPosition position : positions) {
            if (position.getActualValue() == null && position.getActualPrice() != null && position.getQuantity() != null) {
                position.setActualValue(position.getActualPrice().multiply(position.getQuantity()));
            }
        }
        
        BigDecimal totalPortfolioValue = positions.stream()
                .map(StockPosition::getActualValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalProfitLoss = positions.stream()
                .map(StockPosition::getProfit)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        model.addAttribute("positions", positions);
        model.addAttribute("username", oauth2User.getAttribute("name"));
        model.addAttribute("totalPortfolioValue", totalPortfolioValue);
        model.addAttribute("totalProfitLoss", totalProfitLoss);
        return "portfolio";
    }

    @PostMapping("/portfolio/add")
    public String addPosition(@AuthenticationPrincipal OAuth2User oauth2User,
                              @RequestParam("symbol") String symbol,
                              @RequestParam("quantity") BigDecimal quantity,
                              @RequestParam("purchasePrice") BigDecimal purchasePrice,
                              @RequestParam(value = "purchaseDate", required = false) String purchaseDateStr,
                              RedirectAttributes redirectAttributes) {
        String email = oauth2User.getAttribute("email");
        User user = portfolioService.getUserByEmail(email);
        LocalDate purchaseDate = purchaseDateStr != null && !purchaseDateStr.isEmpty() 
            ? LocalDate.parse(purchaseDateStr) 
            : LocalDate.now();
        portfolioService.addPosition(user, symbol, quantity, purchasePrice, purchaseDate);
        redirectAttributes.addFlashAttribute("success", "Position added successfully");
        return "redirect:/portfolio";
    }

    @PostMapping("/portfolio/delete")
    public String deletePosition(@AuthenticationPrincipal OAuth2User oauth2User,
                                 @RequestParam("id") Long id,
                                 RedirectAttributes redirectAttributes) {
        String email = oauth2User.getAttribute("email");
        User user = portfolioService.getUserByEmail(email);
        portfolioService.deletePosition(user, id);
        redirectAttributes.addFlashAttribute("success", "Position deleted successfully");
        return "redirect:/portfolio";
    }

    @GetMapping("/api/search")
    @ResponseBody
    public Mono<List<StockSearchResult>> searchStocks(@RequestParam("q") String query) {
        return stockService.searchStocks(query, defaultApiKey);
    }

    @GetMapping("/api/portfolio-history")
    @ResponseBody
    public Map<LocalDate, BigDecimal> getPortfolioHistory(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RequestParam(value = "period", defaultValue = "week") String period) {
        String email = oauth2User.getAttribute("email");
        User user = portfolioService.getUserByEmail(email);
        return portfolioService.getPortfolioValueHistory(user, period);
    }
}
