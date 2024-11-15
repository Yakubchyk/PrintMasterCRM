package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.entity.PostPress;
import com.springboot.printmastercrm.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/profile")
public class CustomerController {

    @Autowired
    CustomerService customerService;

    @GetMapping
    public String profile(Model model, Authentication authentication) {
        String managerUsername = authentication.getName();
        List<Customer> customers = customerService.getCustomersByManager(managerUsername);
        model.addAttribute("customers", customers);
        model.addAttribute("customer", new Customer());
        return "profile";
    }

    // Обработка отправки формы регистрации нового клиента
    @PostMapping("/register")
    public String register(@ModelAttribute("customer") Customer customer, Authentication authentication) {
        String managerUsername = authentication.getName();
        customerService.createCustomerForManager(customer, managerUsername);
        return "redirect:/profile";
    }

    @PostMapping("/update")
    public String updateCustomer(@ModelAttribute("selectedCustomer") Customer updatedCustomer) {
        customerService.updateCustomer(updatedCustomer.getId(), updatedCustomer);
        return "redirect:/profile?id=" + updatedCustomer.getId();
    }

    // рбработка и отображение форм расчета по тиснению--->
    @GetMapping("/postpress")
    public String postpressForm(@RequestParam("customerId") Long customerId, Model model) {
        Customer customer = customerService.findById(customerId);
        model.addAttribute("postpress", new PostPress());
        model.addAttribute("customer", customer);
        return "postpress";
    }

    @PostMapping("/postpress")
    public String calculatePostpress(@ModelAttribute("postpress") PostPress postpress,
                                     @RequestParam("customerId") Long customerId) {
        Customer customer = customerService.findById(customerId);
        postpress.setCustomer(customer);

        return "redirect:/profile?customerId=" + customerId;
    }
}
