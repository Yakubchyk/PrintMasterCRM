package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.OrderStatus;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrdersController {

    private final OrderService orderService;

    @GetMapping
    public String list(@RequestParam(value = "status", required = false) String status,
                       @RequestParam(value = "customerId", required = false) Long customerId, Model model) {
        model.addAttribute("orders", orderService.find(parseStatus(status), customerId));
        model.addAttribute("status", status == null || status.isBlank() ? "ALL" : status);
        model.addAttribute("customerId", customerId);
        model.addAttribute("statuses", OrderStatus.values());
        return "orders";
    }

    @PostMapping("/{id}/status")
    public String setStatus(@PathVariable Long id, @RequestParam String status) {
        orderService.setStatus(id, parseStatus(status));
        return "redirect:/orders";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        orderService.delete(id);
        return "redirect:/orders";
    }

    private static OrderStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equals(status)) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Неизвестный статус заказа");
        }
    }
}
