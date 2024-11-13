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
        System.out.println(account);



//        SecurityContext context = SecurityContextHolder.getContext();
//        Authentication authentication = context.getAuthentication();
//        System.out.println(authentication.getPrincipal());
//        System.out.println(authentication.getAuthorities());
//        System.out.println(authentication.getDetails());
//        System.out.println(authentication.getCredentials());
        return "home";
    }
}
