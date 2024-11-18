package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.service.CustomerService;
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


    @GetMapping("/settings")
    public String getSettings(Model model) {

        List<Customer> customers = customerService.getAllCustomers();
        model.addAttribute("customers", customers);

        Setting settings = settingService.getSettings();
        model.addAttribute("settings", settings);

//        Setting settings = settingService.getSettings();
//        model.addAttribute("settings", settings);
        return "settings";
    }

    @PostMapping("/settings")
    public String updateSettings(@ModelAttribute("settings") Setting updatedSettings) {
        settingService.saveSettings(updatedSettings);
        return "redirect:/admin/settings";
    }



}
