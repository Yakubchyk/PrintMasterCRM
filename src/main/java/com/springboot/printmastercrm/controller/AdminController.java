package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.*;
import com.springboot.printmastercrm.service.*;
import org.apache.catalina.Manager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    @Autowired
    private AccountService accountService;

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
        List<Account> accountList;
        accountList = accountService.findAll(customerId);


        model.addAttribute("postPressList", postPressList);
        model.addAttribute("printingList", printingList);
        model.addAttribute("accountList", accountList);
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

//    @PostMapping("/settings/assign-manager")
//    public String assignManager(
//            @RequestParam("postPressId") Long postPressId,
//            @RequestParam("managerId") Long managerId) {
//
//        // Получение PostPress
//        PostPress postPress = postPressService.findById(postPressId);
//        if (postPress == null) {
//            throw new IllegalArgumentException("PostPress not found with ID: " + postPressId);
//        }
//
//        // Получение Manager (Account)
//        List<Account> manager = accountService.findById(managerId);
//        if (manager == null) {
//            throw new IllegalArgumentException("Manager not found or is not a valid manager.");
//        }
//
//        // Назначение менеджера
//        postPress.setId(managerId);
//        postPressService.save(postPress);
//
//        return "redirect:/admin/settings";
//    }

    @PostMapping("/settings/action")
    public String handleAction(
            @RequestParam("postPressId") Long postPressId,
            @RequestParam String action,
            @RequestParam(required = false) Long managerId) {

        PostPress postPress = postPressService.findById(postPressId);
        if (postPress == null) {
            throw new IllegalArgumentException("PostPress not found with ID: " + postPressId);
        }

        switch (action) {
            case "delete":
                // Удаление PostPress
                postPressService.deletePostPressById(postPressId);
                break;

            case "transfer":
                // Передача PostPress другому менеджеру
                if (managerId == null) {
                    throw new IllegalArgumentException("Manager ID is required for transfer action.");
                }
                Account manager = accountService.findById(managerId)
                        .orElseThrow(() -> new IllegalArgumentException("Manager not found with ID: " + managerId));

                postPress.setManager(manager); // Назначаем менеджера
                postPressService.save(postPress);
                break;

            default:
                throw new IllegalArgumentException("Unknown action: " + action);
        }

        return "redirect:/admin/settings";
    }

    @PostMapping("/settings/deleteManager")
    public String deleteManager(@RequestParam("id") Long managerId) {
        accountService.deleteManagerById(managerId);
        return "redirect:/admin/settings";
    }

    @PostMapping("/printing/delete")
    public String deletePrinting(@RequestParam("printingId") Long printingId) {
        printingService.deleteById(printingId);
        return "redirect:/admin/settings";
    }

    @PostMapping("/settings/deleteCustomer")
    public String deleteCustomer(@RequestParam("id") Long id) {
        customerService.deleteCustomer(id);
        return "redirect:/admin/settings";
    }
}
