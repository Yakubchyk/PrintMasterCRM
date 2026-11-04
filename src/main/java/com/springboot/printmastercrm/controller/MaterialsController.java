package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.MaterialCategory;
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

    @GetMapping
    public String list(Model model) {
        Map<MaterialCategory, List<com.springboot.printmastercrm.entity.Material>> byCategory = new LinkedHashMap<>();
        for (MaterialCategory category : MaterialCategory.values()) {
            byCategory.put(category, materialService.findByCategory(category));
        }
        model.addAttribute("materialsByCategory", byCategory);
        model.addAttribute("categories", MaterialCategory.values());
        return "materials";
    }

    @PostMapping
    public String create(@RequestParam String name, @RequestParam MaterialCategory category,
                         @RequestParam(value = "sheetSize", required = false) String sheetSize,
                         @RequestParam(value = "stock", required = false) BigDecimal stock,
                         @RequestParam(value = "note", required = false) String note) {
        materialService.create(name, category, sheetSize, stock, note);
        return "redirect:/materials";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("material", materialService.get(id));
        model.addAttribute("movements", materialService.movements(id));
        return "material";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id, @RequestParam String name,
                         @RequestParam(value = "sheetSize", required = false) String sheetSize,
                         @RequestParam(value = "note", required = false) String note) {
        materialService.update(id, name, sheetSize, note);
        return "redirect:/materials/" + id;
    }

    @PostMapping("/{id}/receive")
    public String receive(@PathVariable Long id, @RequestParam BigDecimal quantity,
                          @RequestParam(value = "note", required = false) String note) {
        materialService.receive(id, quantity, note);
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
