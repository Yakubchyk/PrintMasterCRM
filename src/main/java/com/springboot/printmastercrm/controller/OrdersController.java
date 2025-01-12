package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.entity.PostPress;
import com.springboot.printmastercrm.entity.Printing;
import com.springboot.printmastercrm.service.CustomerService;
import com.springboot.printmastercrm.service.PostPressService;
import com.springboot.printmastercrm.service.PrintingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/profile")
public class OrdersController {

    @Autowired
    private PostPressService postPressService;

    @Autowired
    private PrintingService printingService;

    @Autowired
    private CustomerService customerService;

    @GetMapping("/orders/{id}")
    public String getOrdersForCustomer(@PathVariable Long id, Model model) {

        Customer customer = customerService.findById(id);
        if (customer == null) {
            throw new IllegalArgumentException("Customer not found with ID: " + id);
        }

        // Получить расчеты тиснения и печати для клиента
        List<PostPress> postPressList = postPressService.findByCustomerId(id);
        List<Printing> printingList = printingService.findByCustomerId(id);

        // Добавить данные в модель
        model.addAttribute("selectedCustomer", customer);
        model.addAttribute("postPressList", postPressList);
        model.addAttribute("printingList", printingList);

        // Вернуть имя шаблона
        return "orders"; // Название вашего Thymeleaf-шаблона
    }
}