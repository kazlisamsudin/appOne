package com.app.ai.controller;

import com.app.ai.model.*;
import com.app.ai.repository.*;
import com.app.ai.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderService orderService; // Inject OrderService

    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
                           @RequestParam(defaultValue = "1") int quantity,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please log in to add items to cart");
            return "redirect:/login";
        }

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Product not found");
            return "redirect:/products/list";
        }

        if (product.getQuantity() < quantity) {
            redirectAttributes.addFlashAttribute("errorMessage", "Not enough stock available");
            return "redirect:/products/list";
        }

        // Get or create cart for user
        Cart cart = cartRepository.findByUser(user).orElseGet(() -> {
            Cart newCart = new Cart(user);
            return cartRepository.save(newCart);
        });

        // Check if product already in cart
        Optional<CartItem> existingItem = cartItemRepository.findByCartAndProduct(cart, product);

        if (existingItem.isPresent()) {
            // Update quantity
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            // Add new item
            CartItem newItem = new CartItem(cart, product, quantity);
            cartItemRepository.save(newItem);
        }

        cart.setUpdatedDate(LocalDateTime.now());
        cartRepository.save(cart);

        redirectAttributes.addFlashAttribute("successMessage", "Product added to cart!");
        return "redirect:/products/list";
    }

    @GetMapping("/view")
    public String viewCart(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return "redirect:/login";
        }

        Cart cart = cartRepository.findByUser(user).orElse(null);

        // Initialize the cart items to avoid lazy loading issues in the view
        if (cart != null && cart.getItems() != null) {
            // Force initialization of cart items to prevent lazy loading issues
            cart.getItems().size(); // This triggers the lazy loading

            // Initialize product data for each cart item
            for (CartItem item : cart.getItems()) {
                if (item.getProduct() != null) {
                    // Access product properties to initialize them
                    item.getProduct().getName();
                    item.getProduct().getPrice();
                }
            }
        }

        model.addAttribute("cart", cart);
        return "cart/view";
    }

    @PostMapping("/remove/{itemId}")
    public String removeFromCart(@PathVariable Long itemId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return "redirect:/login";
        }

        CartItem item = cartItemRepository.findById(itemId).orElse(null);
        if (item != null && item.getCart().getUser().equals(user)) {
            cartItemRepository.delete(item);
            redirectAttributes.addFlashAttribute("successMessage", "Item removed from cart");
        }

        return "redirect:/cart/view";
    }

    @PostMapping("/checkout")
    public String checkout(Authentication authentication, RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return "redirect:/login";
        }

        Cart cart = cartRepository.findByUser(user).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Your cart is empty");
            return "redirect:/cart/view";
        }

        try {
            // Create order from cart
            Order order = new Order();
            order.setUser(user);
            order.setOrderDate(LocalDateTime.now());
            order.setStatus(Order.OrderStatus.PENDING);
            order.setTotalAmount(cart.getTotalAmount());

            // Convert cart items to order items
            List<OrderItem> orderItems = new ArrayList<>();
            for (CartItem cartItem : cart.getItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(cartItem.getProduct());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setPrice(cartItem.getPrice());
                orderItem.setTotalPrice(cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
                orderItems.add(orderItem);
            }
            order.setOrderItems(orderItems);

            // Use OrderService.createOrder which handles stock reduction and history tracking
            Order savedOrder = orderService.createOrder(order);

            // Save order items to database
            for (OrderItem orderItem : orderItems) {
                orderItem.setOrder(savedOrder);
                orderItemRepository.save(orderItem);
            }

            // Clear cart
            cartItemRepository.deleteAll(cart.getItems());
            cartRepository.delete(cart);

            redirectAttributes.addFlashAttribute("successMessage",
                "Order placed successfully! Order #" + savedOrder.getId());
            return "redirect:/orders/my-orders";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Failed to place order: " + e.getMessage());
            return "redirect:/cart/view";
        }
    }
}
