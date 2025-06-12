package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.Order;
import com.springboot.printmastercrm.entity.OrderStatus;
import com.springboot.printmastercrm.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrdersController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public String viewOrders(@RequestParam(name = "status", required = false) String status, Model model) {
        List<Order> orders;
        if (status == null || status.equals("ALL")) {
            orders = orderService.getAllOrders();
        } else {
            orders = orderService.getOrdersByStatus(OrderStatus.valueOf(status));
        }
        model.addAttribute("orders", orders);
        return "orders";
    }

    @PostMapping("/status/{id}")
    public String updateStatus(@PathVariable Long id, @RequestParam("status") String status) {
        OrderStatus newStatus = OrderStatus.valueOf(status);
        orderService.setStatus(id, newStatus);
        return "redirect:/orders";
    }
}
