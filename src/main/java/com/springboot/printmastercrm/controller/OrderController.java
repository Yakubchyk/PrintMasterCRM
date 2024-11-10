package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.Order;
import com.springboot.printmastercrm.entity.User;
import com.springboot.printmastercrm.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public List<Order> getOrders(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return orderService.getOrdersByUser(user);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Order createOrder(@RequestBody Order order, Authentication auth) {
        User user = (User) auth.getPrincipal();
        return orderService.createOrder(order, user);
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    public void deleteOrder(@PathVariable Long id, Authentication auth) {
        Order order = (Order) auth.getPrincipal();
        orderService.deleteOrder(order);
    }
}