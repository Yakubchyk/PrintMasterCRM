package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.Account;
import com.springboot.printmastercrm.entity.Order;
import com.springboot.printmastercrm.entity.OrderStatus;
import com.springboot.printmastercrm.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrdersController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public String viewOrders(@RequestParam(name = "status", required = false) String status,
                             @RequestParam(name = "customerId", required = false) Long customerId,
                             Authentication authentication, Model model) {
        OrderStatus filter = (status == null || status.equals("ALL")) ? null : parseStatus(status);
        List<Order> orders = orderService.getVisibleOrders(authentication.getName(), isAdmin(authentication), filter);
        if (customerId != null) {
            orders = orders.stream().filter(o -> o.getCustomer().getId().equals(customerId)).toList();
        }
        model.addAttribute("orders", orders);
        model.addAttribute("customerId", customerId);
        return "orders";
    }

    @PostMapping("/status/{id}")
    public String updateStatus(@PathVariable Long id, @RequestParam("status") String status,
                               Authentication authentication) {
        orderService.setStatus(id, parseStatus(status), authentication.getName(), isAdmin(authentication));
        return "redirect:/orders";
    }

    private OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status: " + status);
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> Account.Role.ROLE_ADMIN.getAuthority().equals(a.getAuthority()));
    }
}
