package com.app.ai.controller;

import com.app.ai.service.OrderService;
import com.app.ai.service.ProductService;
import com.app.ai.service.UserService;
import com.app.ai.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public String dashboard(Model model) {
        // Get current date for analytics
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startOfWeek = now.minusDays(7);

        // Basic Statistics
        long totalUsers = userService.getTotalUserCount();
        long totalProducts = productService.findAllProducts().size();
        long totalCategories = categoryService.findAllCategories().size();
        long totalOrdersThisMonth = orderService.getOrderCountFromDate(startOfMonth);
        long totalOrdersThisWeek = orderService.getOrderCountFromDate(startOfWeek);

        // Revenue Analytics
        BigDecimal monthlyRevenue = orderService.getTotalRevenueByDateRange(startOfMonth, now);
        BigDecimal weeklyRevenue = orderService.getTotalRevenueByDateRange(startOfWeek, now);

        // Order Status Distribution
        List<Object[]> orderStatusCounts = orderService.getOrderStatusCounts();
        if (orderStatusCounts == null) {
            orderStatusCounts = List.of();
        }

        // Popular Products
        List<Object[]> popularProducts = orderService.getMostPopularProducts();
        if (popularProducts == null) {
            popularProducts = List.of();
        }

        List<Object[]> topRevenueProducts = orderService.getTopRevenueProducts();
        if (topRevenueProducts == null) {
            topRevenueProducts = List.of();
        }

        // Monthly Order Statistics for Current Year
        List<Object[]> monthlyStats = orderService.getMonthlyOrderStats(now.getYear());
        if (monthlyStats == null) {
            monthlyStats = List.of();
        }

        // Categories with Product Count - Add null safety
        List<Object[]> categoryStats;
        try {
            categoryStats = categoryService.getCategoriesOrderedByProductCount().stream()
                    .limit(10)
                    .map(category -> new Object[]{
                        category.getName() != null ? category.getName() : "Unknown",
                        categoryService.getProductCountByCategory(category.getId())
                    })
                    .toList();
        } catch (Exception e) {
            categoryStats = List.of();
        }

        // Add all data to model
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalCategories", totalCategories);
        model.addAttribute("totalOrdersThisMonth", totalOrdersThisMonth);
        model.addAttribute("totalOrdersThisWeek", totalOrdersThisWeek);
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute("weeklyRevenue", weeklyRevenue);
        model.addAttribute("orderStatusCounts", orderStatusCounts);
        model.addAttribute("popularProducts", popularProducts);
        model.addAttribute("topRevenueProducts", topRevenueProducts);
        model.addAttribute("monthlyStats", monthlyStats);
        model.addAttribute("categoryStats", categoryStats);
        model.addAttribute("currentYear", now.getYear());
        model.addAttribute("lastUpdated", now.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));

        return "dashboard/analytics";
    }

    @GetMapping("/reports")
    public String reports(@RequestParam(required = false) String reportType,
                         @RequestParam(required = false) String period,
                         Model model) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;
        String periodLabel;

        // Determine date range based on period
        switch (period != null ? period : "month") {
            case "week":
                startDate = now.minusDays(7);
                periodLabel = "Last 7 Days";
                break;
            case "quarter":
                startDate = now.minusMonths(3);
                periodLabel = "Last 3 Months";
                break;
            case "year":
                startDate = now.minusYears(1);
                periodLabel = "Last Year";
                break;
            default:
                startDate = now.withDayOfMonth(1);
                periodLabel = "This Month";
                period = "month";
        }

        // Generate report data based on type
        BigDecimal totalRevenue = orderService.getTotalRevenueByDateRange(startDate, now);
        Long totalOrders = orderService.getOrderCountFromDate(startDate);
        List<Object[]> orderStatusCounts = orderService.getOrderStatusCounts();

        model.addAttribute("reportType", reportType != null ? reportType : "sales");
        model.addAttribute("period", period);
        model.addAttribute("periodLabel", periodLabel);
        model.addAttribute("startDate", startDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        model.addAttribute("endDate", now.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("orderStatusCounts", orderStatusCounts);

        return "dashboard/reports";
    }
}
