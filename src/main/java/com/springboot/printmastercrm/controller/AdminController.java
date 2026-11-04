package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.Account;
import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.service.AccountService;
import com.springboot.printmastercrm.service.CustomerService;
import com.springboot.printmastercrm.service.SettingService;
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
    private AccountService accountService;

    @GetMapping("/settings")
    public String getSettings(Model model) {
        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute("settings", settingService.getSettings());

        List<Account> accountList = accountService.findAll();
        model.addAttribute("accountList", accountList);
        // клиентов передают только менеджерам, а не администраторам
        model.addAttribute("managerList", accountList.stream()
                .filter(account -> account.getRoles().contains(Account.Role.ROLE_MANAGER))
                .toList());
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
}
