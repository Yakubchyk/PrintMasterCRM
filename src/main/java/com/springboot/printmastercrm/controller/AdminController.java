package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private SettingService settingService;

    @GetMapping("/settings")
    public String getSettings(Model model) {
        Setting settings = settingService.getSettings();
        model.addAttribute("settings", settings);
        return "settings";
    }

    @PostMapping("/settings")
    public String updateSettings(@ModelAttribute("settings") Setting updatedSettings) {
        settingService.saveSettings(updatedSettings);
        return "redirect:/admin/settings";
    }
}
