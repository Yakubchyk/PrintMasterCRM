package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.*;
import com.springboot.printmastercrm.service.*;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
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

        List<Account> accountList = accountService.findAll(customerId);

        model.addAttribute("postPressList", postPressList);
        model.addAttribute("printingList", printingList);
        model.addAttribute("accountList", accountList);
        return "settings";
    }

    @PostMapping("/settings")
    public String updateSettings(@ModelAttribute("settings") Setting updatedSettings,
                                 RedirectAttributes redirectAttributes) {
        try {
            settingService.saveSettings(updatedSettings);
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    @PostMapping("/settings/delete")
    public String deletePostPress(@RequestParam("postPressId") Long postPressId) {
        postPressService.deletePostPressById(postPressId);
        return "redirect:/admin/settings";
    }

    @PostMapping("/settings/action")
    public String handleAction(
            @RequestParam("customerId") Long customerId,
            @RequestParam String action,
            @RequestParam(required = false) Long managerId,
            RedirectAttributes redirectAttributes) {

        switch (action) {
            case "delete":
                customerService.deleteCustomer(customerId);
                break;

            case "transfer":
                if (managerId == null) {
                    redirectAttributes.addFlashAttribute("error", "Выберите менеджера, которому передать клиента");
                    break;
                }
                customerService.transferCustomerToManager(customerId, managerId);
                break;

            default:
                redirectAttributes.addFlashAttribute("error", "Неизвестное действие: " + action);
        }

        return "redirect:/admin/settings";
    }

    @PostMapping("/settings/deleteManager")
    public String deleteManager(@RequestParam("id") Long managerId, Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            accountService.deleteManagerById(managerId, authentication.getName());
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
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
