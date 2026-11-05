package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.FoilType;
import com.springboot.printmastercrm.entity.LengthUnit;
import com.springboot.printmastercrm.entity.MaterialCategory;
import com.springboot.printmastercrm.service.SettingService;
import com.springboot.printmastercrm.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/materials")
@RequiredArgsConstructor
public class MaterialsController {

    private final MaterialService materialService;
    private final SettingService settingService;

    @GetMapping
    public String list(Model model) {
        Map<MaterialCategory, List<com.springboot.printmastercrm.entity.Material>> byCategory = new LinkedHashMap<>();
        for (MaterialCategory category : MaterialCategory.values()) {
            byCategory.put(category, materialService.findByCategory(category));
        }
        model.addAttribute("materialsByCategory", byCategory);
        model.addAttribute("categories", MaterialCategory.values());
        model.addAttribute("foilTypes", FoilType.values());
        model.addAttribute("lu", settingService.getSettings().getLengthUnit());
        return "materials";
    }

    @PostMapping
    public String create(@RequestParam(value = "name", required = false) String name,
                         @RequestParam MaterialCategory category,
                         @RequestParam(value = "foilType", required = false) FoilType foilType,
                         @RequestParam(value = "winding", required = false) BigDecimal winding,
                         @RequestParam(value = "rolls", required = false) BigDecimal rolls,
                         @RequestParam(value = "sheetSize", required = false) String sheetSize,
                         @RequestParam(value = "stock", required = false) BigDecimal stock,
                         @RequestParam(value = "width", required = false) BigDecimal width,
                         @RequestParam(value = "length", required = false) BigDecimal length,
                         @RequestParam(value = "sheets", required = false) BigDecimal sheets,
                         @RequestParam(value = "note", required = false) String note) {
        if (category == MaterialCategory.PAPER) {
            materialService.create(name, category, sheetSize, stock, note);
        } else if (category == MaterialCategory.FOIL) {
            LengthUnit unit = settingService.getSettings().getLengthUnit();
            materialService.createFoil(foilType, width == null ? null : unit.toCm(width), winding, rolls, note);
        } else {
            LengthUnit unit = settingService.getSettings().getLengthUnit();
            materialService.createPlate(name, category, width == null ? null : unit.toCm(width),
                    length == null ? null : unit.toCm(length), sheets, note);
        }
        return "redirect:/materials";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("material", materialService.get(id));
        model.addAttribute("movements", materialService.movements(id));
        model.addAttribute("lu", settingService.getSettings().getLengthUnit());
        return "material";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id, @RequestParam String name,
                         @RequestParam(value = "sheetSize", required = false) String sheetSize,
                         @RequestParam(value = "width", required = false) BigDecimal width,
                         @RequestParam(value = "length", required = false) BigDecimal length,
                         @RequestParam(value = "winding", required = false) BigDecimal winding,
                         @RequestParam(value = "note", required = false) String note) {
        materialService.update(id, name, sheetSize, note);
        LengthUnit unit = settingService.getSettings().getLengthUnit();
        if (width != null && winding != null) {
            // фольга: намотка в метрах
            materialService.updatePlateSize(id, unit.toCm(width), winding.movePointRight(2));
        } else if (width != null && length != null) {
            materialService.updatePlateSize(id, unit.toCm(width), unit.toCm(length));
        }
        return "redirect:/materials/" + id;
    }

    @PostMapping("/{id}/receive")
    public String receive(@PathVariable Long id, @RequestParam BigDecimal quantity,
                          @RequestParam(value = "note", required = false) String note) {
        materialService.receive(id, quantity, note);
        return "redirect:/materials/" + id;
    }

    @PostMapping("/{id}/receive-sheets")
    public String receiveSheets(@PathVariable Long id, @RequestParam BigDecimal sheets,
                                @RequestParam(value = "note", required = false) String note) {
        materialService.receiveSheets(id, sheets, note);
        return "redirect:/materials/" + id;
    }

    @PostMapping("/{id}/consume")
    public String consume(@PathVariable Long id, @RequestParam BigDecimal quantity,
                          @RequestParam(value = "note", required = false) String note) {
        materialService.consume(id, quantity, note);
        return "redirect:/materials/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        materialService.delete(id);
        return "redirect:/materials";
    }
}
