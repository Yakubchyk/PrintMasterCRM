package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.AreaUnit;
import com.springboot.printmastercrm.entity.LengthUnit;
import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** «Админка»: цены, единицы измерения и коэффициент. */
@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingService settingService;

    @InitBinder("settings")
    public void restrictBinding(WebDataBinder binder) {
        binder.setDisallowedFields("id");
    }

    @GetMapping
    public String show(Model model) {
        model.addAttribute("settings", settingService.getSettings());
        model.addAttribute("lengthUnits", LengthUnit.values());
        model.addAttribute("areaUnits", AreaUnit.values());
        return "settings";
    }

    @PostMapping
    public String save(@ModelAttribute("settings") Setting settings, RedirectAttributes redirectAttributes) {
        settingService.saveSettings(settings);
        redirectAttributes.addFlashAttribute("saved", "Настройки сохранены");
        return "redirect:/settings";
    }
}
