package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.entity.PostPress;
import com.springboot.printmastercrm.service.CustomerService;
import com.springboot.printmastercrm.service.PostPressService;
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

    @Autowired
    PostPressService postPressService;

    @GetMapping
    public String profile(Model model, Authentication authentication, @RequestParam(value = "id", required = false) Long id) {
        String managerUsername = authentication.getName();
        List<Customer> customers = customerService.getCustomersByManager(managerUsername);
        model.addAttribute("customers", customers);
        model.addAttribute("customer", new Customer());

        if (id != null) {
            Customer selectedCustomer = customerService.findById(id);
            model.addAttribute("selectedCustomer", selectedCustomer);

            List<PostPress> postPressList = postPressService.findByCustomerId(id);
            model.addAttribute("postPressList", postPressList);

        }

        return "profile";
    }

    @PostMapping("/postpress")
    public String saveCalculation(@ModelAttribute PostPress postPress, @RequestParam Long customerId) {
        Customer customer = customerService.findById(customerId);
        postPress.setCustomer(customer);

        postPressService.save(postPress);

        return "redirect:/profile?id=" + customer.getId();
    }

    @PostMapping("/postpress/delete")
    public String deletePostPress(@RequestParam("postPressId") Long postPressId, @RequestParam Long customerId) {
        postPressService.deletePostPressById(postPressId);
        return "redirect:/profile?id=" + customerId;
    }


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

    @PostMapping("/delete")
    public String deleteCustomer(@RequestParam("id") Long id) {
        customerService.deleteCustomer(id);
        return "redirect:/profile";
    }

}