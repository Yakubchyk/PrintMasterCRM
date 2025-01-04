package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.*;
import com.springboot.printmastercrm.service.CustomerService;
import com.springboot.printmastercrm.service.PostPressService;
import com.springboot.printmastercrm.service.PrintingService;
import com.springboot.printmastercrm.service.SettingService;
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
    @Autowired
    PrintingService printingService;
    @Autowired
    SettingService settingService;

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

            List<Printing> printingList = printingService.findByCustomerId(id);
            model.addAttribute("printingList", printingList);
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

    @GetMapping("/postpress")
    public String postPress(@RequestParam(value = "customerId") Long customerId, Model model) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        Customer customer = customerService.findById(customerId);
        model.addAttribute("selectedCustomer", customer);
        model.addAttribute("postPress", new PostPress());
        return "postpress";
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


    @GetMapping("/order")
    public String order(@RequestParam("customerId") Long customerId, Model model) {

        List<PostPress> postPress = postPressService.findByCustomerId(customerId);
        if (postPress == null) {
            throw new RuntimeException("PostPress not found for Customer ID: " + customerId);
        }
        List<Printing> printing = printingService.findByCustomerId(customerId);

        Customer customer = customerService.findById(customerId);
        if (customer == null) {
            throw new RuntimeException("Customer not found for ID: " + customerId);
        }

        Order order = new Order();
        order.setCustomer(customer);

        model.addAttribute("order", order);
        model.addAttribute("customer", customer);
        model.addAttribute("postPress", postPress);
        model.addAttribute("printing", printing);

        return "order";
    }

    @GetMapping("/printing")
    public String printing(@RequestParam("customerId") Long customerId, Model model) {
        Customer customer = customerService.findById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("Customer not found for ID: " + customerId);
        }

        Setting settings = settingService.getSettings();

        model.addAttribute("selectedCustomer", customer);
        model.addAttribute("printing", new Printing());
        model.addAttribute("pricePrint", settings.getPricePrint()); // Добавляем pricePrint

        return "printing";
    }

    @PostMapping("/printing")
    public String savePrinting(@ModelAttribute Printing printing, @RequestParam Long customerId) {
        Customer customer = customerService.findById(customerId);
        Setting settings = settingService.getSettings();

        double totalPrice = settings.getPricePrint() * printing.getQuantity();
        printing.setTotalCost(totalPrice);

        printing.setCustomer(customer);
        printingService.save(printing);

        return "redirect:/profile?id=" + customerId;
    }


}