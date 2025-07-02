package com.app.ai.controller;

import com.app.ai.model.StockHistory;
import com.app.ai.service.StockHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/stock")
@PreAuthorize("hasRole('ADMIN')")
public class StockManagementController {

    @Autowired
    private StockHistoryService stockHistoryService;

    @GetMapping("/history")
    public String stockHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);
        Page<StockHistory> stockHistoryPage = stockHistoryService.getAllStockHistory(pageable);

        model.addAttribute("stockHistoryPage", stockHistoryPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);

        return "admin/stock-history";
    }
}
