package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.dto.RegistrationForm;
import com.springboot.printmastercrm.exception.ExistsException;
import com.springboot.printmastercrm.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                           BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        try {
            accountService.registerManager(form);
        } catch (ExistsException e) {
            bindingResult.reject("account.exists", e.getMessage());
            return "register";
        }
        return "redirect:/account/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

}
