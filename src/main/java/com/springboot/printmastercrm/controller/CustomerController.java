package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.entity.LengthUnit;
import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final PostPressService postPressService;
    private final PrintingService printingService;
    private final ClicheService clicheService;
    private final OrderService orderService;
    private final SettingService settingService;

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String q, Model model) {
        model.addAttribute("customers", customerService.search(q));
        model.addAttribute("q", q == null ? "" : q);
        return "customers";
    }

    @PostMapping
    public String create(@RequestParam String name, @RequestParam String phone, @RequestParam String email) {
        Customer customer = customerService.create(name, phone, email);
        return "redirect:/customers/" + customer.getId();
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        Customer customer = customerService.get(id);
        Setting settings = settingService.getSettings();
        model.addAttribute("customer", customer);
        model.addAttribute("settings", settings);
        model.addAttribute("lu", settings.getLengthUnit());
        model.addAttribute("postPressList", postPressService.findByCustomerId(id));
        model.addAttribute("printingList", printingService.findByCustomerId(id));
        model.addAttribute("clicheList", clicheService.findByCustomerId(id));
        model.addAttribute("orders", orderService.find(null, id));
        model.addAttribute("papers", PrintingService.PAPERS);
        model.addAttribute("colours", PrintingService.COLOURS);
        return "customer";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id, @RequestParam String name, @RequestParam String phone,
                         @RequestParam String email) {
        customerService.update(id, name, phone, email);
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        customerService.delete(id);
        return "redirect:/customers";
    }

    // ---- расчёты: размеры вводятся в выбранных единицах и хранятся в сантиметрах ----

    @PostMapping("/{id}/postpress")
    public String addPostPress(@PathVariable Long id, @RequestParam BigDecimal width,
                               @RequestParam BigDecimal length, @RequestParam int quantity) {
        Customer customer = customerService.get(id);
        LengthUnit unit = settingService.getSettings().getLengthUnit();
        postPressService.create(customer, unit.toCm(width), unit.toCm(length), quantity);
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/postpress/{calcId}/delete")
    public String deletePostPress(@PathVariable Long id, @PathVariable Long calcId) {
        postPressService.delete(id, calcId);
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/printing")
    public String addPrinting(@PathVariable Long id, @RequestParam String papier,
                              @RequestParam String colour, @RequestParam int quantity) {
        printingService.create(customerService.get(id), papier, colour, quantity);
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/printing/{calcId}/delete")
    public String deletePrinting(@PathVariable Long id, @PathVariable Long calcId) {
        printingService.delete(id, calcId);
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/cliche")
    public String addCliche(@PathVariable Long id, @RequestParam BigDecimal width,
                            @RequestParam BigDecimal length, @RequestParam int quantity) {
        Customer customer = customerService.get(id);
        LengthUnit unit = settingService.getSettings().getLengthUnit();
        clicheService.create(customer, unit.toCm(width), unit.toCm(length), quantity);
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/cliche/{calcId}/delete")
    public String deleteCliche(@PathVariable Long id, @PathVariable Long calcId) {
        clicheService.delete(id, calcId);
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/orders")
    public String createOrder(@PathVariable Long id, @RequestParam String product,
                              @RequestParam(value = "postPressIds", required = false) List<Long> postPressIds,
                              @RequestParam(value = "printingIds", required = false) List<Long> printingIds,
                              @RequestParam(value = "clicheIds", required = false) List<Long> clicheIds) {
        orderService.createOrder(customerService.get(id), product, postPressIds, printingIds, clicheIds);
        return "redirect:/customers/" + id;
    }
}
