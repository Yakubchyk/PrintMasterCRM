package com.springboot.printmastercrm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class ProfileController {

    @GetMapping("/profile")
    public String profile(Model model) {
        return "profile";
    }
}
