package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.User;
import com.springboot.printmastercrm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user, Model model) {
        if (userService.isUserExist(user.getUsername())) {
            model.addAttribute("error", "Пользователь с таким именем уже существует");
            return "register";
        }
        userService.registerUser(user);
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(Authentication authentication) {
        if (authentication != null) {
            authentication.setAuthenticated(false);
        }
        return "redirect:/login";
    }
}