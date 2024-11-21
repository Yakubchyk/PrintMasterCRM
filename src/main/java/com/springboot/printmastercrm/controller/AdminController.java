package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.entity.PostPress;
import com.springboot.printmastercrm.entity.Printing;
import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.service.CustomerService;
import com.springboot.printmastercrm.service.PostPressService;
import com.springboot.printmastercrm.service.PrintingService;
import com.springboot.printmastercrm.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private SettingService settingService;

    @Autowired
    private CustomerService customerService;
    @Autowired
    private PostPressService postPressService;
    @Autowired
    private PrintingService printingService;

    @GetMapping("/settings")
    public String getSettings(@RequestParam(value = "customerId", required = false) Long customerId, Model model) {
        List<Customer> customers = customerService.getAllCustomers();
        model.addAttribute("customers", customers);

        Setting settings = settingService.getSettings();
        model.addAttribute("settings", settings);

        List<PostPress> postPressList;
        if (customerId != null) {
            postPressList = postPressService.findByCustomerId(customerId);
        } else {
            postPressList = postPressService.findAll();
        }

        List<Printing> printingList;
        if (customerId != null) {
            printingList = printingService.findByCustomerId(customerId);
        } else {
            printingList = printingService.findAll();
        }
        model.addAttribute("postPressList", postPressList);
        model.addAttribute("printingList", printingList);
        return "settings";
    }


    @PostMapping("/settings")
    public String updateSettings(@ModelAttribute("settings") Setting updatedSettings) {
        settingService.saveSettings(updatedSettings);
        return "redirect:/admin/settings";
    }

    @PostMapping("/settings/delete")
    public String deletePostPress(@RequestParam("postPressId") Long postPressId) {
        postPressService.deletePostPressById(postPressId);
        return "redirect:/admin/settings";
    }

    @PostMapping("/settings/deleteCustomer")
    public String deleteCustomer(@RequestParam("id") Long id) {
        customerService.deleteCustomer(id);
        return "redirect:/admin/settings";
    }
}
