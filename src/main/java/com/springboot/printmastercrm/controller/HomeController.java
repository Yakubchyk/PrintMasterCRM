package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.Account;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {

    @GetMapping
    public String home(@AuthenticationPrincipal Account account) {
        return "home";
    }
}
