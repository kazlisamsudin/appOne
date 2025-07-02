package com.app.ai.controller;

import com.app.ai.model.Order;
import com.app.ai.model.User;
import com.app.ai.service.OrderService;
import com.app.ai.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public String listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderDate") String sortField,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            Model model) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Order> orderPage;
        if (status != null && !status.trim().isEmpty()) {
            try {
                Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
                orderPage = orderService.findOrdersByStatus(orderStatus, pageable);
                model.addAttribute("selectedStatus", status);
            } catch (IllegalArgumentException e) {
                orderPage = orderService.findPaginatedOrders(pageable);
            }
        } else {
            orderPage = orderService.findPaginatedOrders(pageable);
        }

        model.addAttribute("orderPage", orderPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        model.addAttribute("orderStatuses", Order.OrderStatus.values());

        return "order/list";
    }

    @GetMapping("/my-orders")
    public String myOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication,
            Model model) {

        User currentUser = userService.findUserByUsername(authentication.getName()).orElse(null);
        if (currentUser == null) {
            return "redirect:/login";
        }

        Sort sort = Sort.by("orderDate").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Order> orderPage = orderService.findOrdersByUser(currentUser, pageable);

        model.addAttribute("orderPage", orderPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);

        return "order/my-orders";
    }

    @GetMapping("/view/{id}")
    public String viewOrder(@PathVariable Long id, Model model, Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        Optional<Order> orderOpt = orderService.findOrderById(id);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Order not found.");
            return "redirect:/orders/list";
        }

        Order order = orderOpt.get();
        User currentUser = userService.findUserByUsername(authentication.getName()).orElse(null);

        // Check if user can view this order (admin or order owner)
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = currentUser != null && currentUser.equals(order.getUser());

        if (!isAdmin && !isOwner) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied.");
            return "redirect:/orders/my-orders";
        }

        model.addAttribute("order", order);
        model.addAttribute("isAdmin", isAdmin);
        return "order/view";
    }

    @PostMapping("/update-status/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateOrderStatus(@PathVariable Long id,
                                   @RequestParam String status,
                                   RedirectAttributes redirectAttributes) {
        try {
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            orderService.updateOrderStatus(id, newStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Order status updated successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid order status.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating order status: " + e.getMessage());
        }

        return "redirect:/orders/view/" + id;
    }

    @GetMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.deleteOrder(id);
            redirectAttributes.addFlashAttribute("successMessage", "Order deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting order: " + e.getMessage());
        }
        return "redirect:/orders/list";
    }
}
