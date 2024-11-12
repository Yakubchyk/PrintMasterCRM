package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.Account;
import com.springboot.printmastercrm.service.AccountService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequestMapping("/")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("account", new Account());
        return "login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute("account") @Valid Account account, BindingResult bindingResult, HttpSession session, Model model) {
        if (bindingResult.hasErrors()) {
            return "login";
        }
        Optional<Account> loggedInAccount = accountService.login(account);
        if (loggedInAccount.isPresent()) {
            session.setAttribute("account", loggedInAccount.get());
            return "redirect:/";
        }
        model.addAttribute("errorMessage", "Неверное имя пользователя или пароль!");
        return "login";
    }

    @GetMapping("/register")
    public String registration(Model model) {
        model.addAttribute("account", new Account());
        return "register";
    }

    @PostMapping("/register")
    public String registration(@ModelAttribute("account") @Valid Account account, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        accountService.createAccount(account);
        model.addAttribute("successMessage", "Регистрация прошла успешно! Заходите.");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/profile")
    public String getProfile(HttpSession session, Model model) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }
        model.addAttribute("account", account);
        return "profile";
    }

    @GetMapping("/profile/update")
    public String getProfileUpdate(HttpSession session, Model model) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }
        model.addAttribute("account", account);
        return "profileUpdate";
    }

    @PostMapping("/profile/update")
    public String saveProfileUpdate(@ModelAttribute("account") Account updatedAccount, HttpSession session, Model model) {
        Account sessionAccount = (Account) session.getAttribute("account");
        if (sessionAccount == null) {
            return "redirect:/login";
        }

        Optional<Account> accountOpt = accountService.getAccountById(sessionAccount.getId());
        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            account.setName(updatedAccount.getName());
            account.setUsername(updatedAccount.getUsername());
            accountService.updateAccount(account.getId(), updatedAccount);

            session.setAttribute("account", account);
            model.addAttribute("message", "Изменения успешно сохранены!");
            return "profileUpdate";
        } else {
            model.addAttribute("errorMessage", "Аккаунт не найден.");
            return "profileUpdate";
        }
    }
}