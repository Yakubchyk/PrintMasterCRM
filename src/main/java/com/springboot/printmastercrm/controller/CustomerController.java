package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.service.CustomerService;
import com.springboot.printmastercrm.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** Раздел «Клиенты»: только сведения о клиентах и краткий список их заказов. */
@Controller
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final OrderService orderService;

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String q, Model model) {
        model.addAttribute("customers", customerService.search(q));
        model.addAttribute("q", q == null ? "" : q);
        return "customers";
    }

    @PostMapping
    public String create(@RequestParam String name, @RequestParam String phone, @RequestParam String email,
                         @RequestParam(value = "postalCode", required = false) String postalCode,
                         @RequestParam(value = "city", required = false) String city,
                         @RequestParam(value = "street", required = false) String street,
                         @RequestParam(value = "houseNumber", required = false) String houseNumber) {
        Customer customer = customerService.create(name, phone, email);
        customerService.saveAddress(customer.getId(), postalCode, city, street, houseNumber);
        return "redirect:/customers/" + customer.getId();
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("customer", customerService.get(id));
        model.addAttribute("orders", orderService.find(null, id));
        return "customer";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id, @RequestParam String name, @RequestParam String phone,
                         @RequestParam String email,
                         @RequestParam(value = "postalCode", required = false) String postalCode,
                         @RequestParam(value = "city", required = false) String city,
                         @RequestParam(value = "street", required = false) String street,
                         @RequestParam(value = "houseNumber", required = false) String houseNumber) {
        customerService.update(id, name, phone, email);
        customerService.saveAddress(id, postalCode, city, street, houseNumber);
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        customerService.delete(id);
        return "redirect:/customers";
    }
}
