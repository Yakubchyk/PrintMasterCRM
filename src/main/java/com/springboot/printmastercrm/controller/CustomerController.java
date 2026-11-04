package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.*;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.ExistsException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.service.OrderService;
import com.springboot.printmastercrm.service.CustomerService;
import com.springboot.printmastercrm.service.PostPressService;
import com.springboot.printmastercrm.service.PrintingService;
import com.springboot.printmastercrm.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/profile")
public class CustomerController {

    @Autowired
    CustomerService customerService;
    @Autowired
    PostPressService postPressService;
    @Autowired
    PrintingService printingService;
    @Autowired
    SettingService settingService;
    @Autowired
    OrderService orderService;

    /**
     * Поля, которые клиент не вправе задавать через форму: идентификаторы, связи с другими
     * сущностями и рассчитанные на сервере суммы. Иначе подменой параметров запроса можно
     * перезаписать чужую запись (save() с заполненным id делает UPDATE).
     */
    @InitBinder({"postPress", "printing", "customer"})
    public void restrictNewObjectBinding(WebDataBinder binder) {
        binder.setDisallowedFields("id", "account*", "customer*", "manager*", "totalPrice", "totalCost");
    }

    /** При редактировании карточки id нужен, но менеджера и аккаунт менять через форму нельзя. */
    @InitBinder("selectedCustomer")
    public void restrictCustomerUpdateBinding(WebDataBinder binder) {
        binder.setDisallowedFields("account*", "manager*");
    }

    @GetMapping
    public String profile(Model model, Authentication authentication, @RequestParam(value = "id", required = false) Long id) {
        String managerUsername = authentication.getName();
        List<Customer> customers = customerService.getCustomersByManager(managerUsername);
        model.addAttribute("customers", customers);
        model.addAttribute("customer", new Customer());

        if (id != null) {
            Customer selectedCustomer = customerService.getOwnedCustomer(id, managerUsername);
            model.addAttribute("selectedCustomer", selectedCustomer);

            List<PostPress> postPressList = postPressService.findByCustomerId(id);
            model.addAttribute("postPressList", postPressList);

            List<Printing> printingList = printingService.findByCustomerId(id);
            model.addAttribute("printingList", printingList);
        }

        return "profile";
    }

    @GetMapping("/customer")
    public String newCustomer(Model model) {
        model.addAttribute("customer", new Customer());
        return "customer";
    }


    @PostMapping("/postpress")
    public String saveCalculation(@ModelAttribute PostPress postPress, @RequestParam Long customerId,
                                  Authentication authentication, RedirectAttributes redirectAttributes) {
        Customer customer = customerService.getOwnedCustomer(customerId, authentication.getName());
        if (postPress.getQuantity() <= 0 || postPress.getWidthSM() <= 0 || postPress.getLengthSM() <= 0) {
            redirectAttributes.addFlashAttribute("error", "Ширина, длина и тираж должны быть больше нуля");
            return "redirect:/profile/postpress?customerId=" + customerId;
        }
        postPress.setCustomer(customer);
        postPress.setManager(customer.getAccount());

        postPressService.save(postPress);

        return "redirect:/profile?id=" + customer.getId();

    }

    @GetMapping("/postpress")
    public String postPress(@RequestParam(value = "customerId") Long customerId, Model model,
                            Authentication authentication) {
        Customer customer = customerService.getOwnedCustomer(customerId, authentication.getName());
        model.addAttribute("selectedCustomer", customer);
        model.addAttribute("postPress", new PostPress());
        return "postpress";
    }


    @PostMapping("/postpress/delete")
    public String deletePostPress(@RequestParam("postPressId") Long postPressId, Authentication authentication) {
        PostPress postPress = postPressService.findById(postPressId);
        if (postPress.getCustomer() == null) {
            throw new NotFoundException("PostPress not found");
        }
        // проверяем владельца по клиенту, к которому реально привязан расчёт, а не по параметру запроса
        Customer customer = customerService.getOwnedCustomer(postPress.getCustomer().getId(), authentication.getName());
        postPressService.deletePostPressById(postPressId);
        return "redirect:/profile?id=" + customer.getId();
    }


    @PostMapping("/register")
    public String register(@ModelAttribute("customer") Customer customer, Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        String managerUsername = authentication.getName();
        try {
            customerService.createCustomerForManager(customer, managerUsername);
        } catch (ExistsException | BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/profile/customer";
        }
        return "redirect:/profile";
    }


    @PostMapping("/update")
    public String updateCustomer(@ModelAttribute("selectedCustomer") Customer updatedCustomer,
                                 Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            customerService.updateCustomer(updatedCustomer, authentication.getName());
        } catch (ExistsException | BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile?id=" + updatedCustomer.getId();
    }

    @PostMapping("/delete")
    public String deleteCustomer(@RequestParam("id") Long id, Authentication authentication) {
        Customer customer = customerService.getOwnedCustomer(id, authentication.getName());
        customerService.deleteCustomer(customer.getId());
        return "redirect:/profile";
    }


    @GetMapping("/order")
    public String order(@RequestParam("customerId") Long customerId, Model model, Authentication authentication) {

        Customer customer = customerService.getOwnedCustomer(customerId, authentication.getName());

        List<PostPress> postPress = postPressService.findByCustomerId(customerId);
        List<Printing> printing = printingService.findByCustomerId(customerId);

        Order order = new Order();
        order.setCustomer(customer);

        model.addAttribute("order", order);
        model.addAttribute("customer", customer);
        model.addAttribute("postPress", postPress);
        model.addAttribute("printing", printing);

        return "order";
    }

    @PostMapping("/order")
    public String createOrder(@RequestParam Long customerId,
                              @RequestParam String product,
                              @RequestParam(required = false) List<Long> postPressIds,
                              @RequestParam(required = false) List<Long> printingIds,
                              Authentication authentication, RedirectAttributes redirectAttributes) {
        Customer customer = customerService.getOwnedCustomer(customerId, authentication.getName());
        try {
            // менеджер заказа = менеджер, за которым закреплён (и которому принадлежит) клиент
            orderService.createOrder(customer, customer.getAccount(), product, postPressIds, printingIds);
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/profile/order?customerId=" + customerId;
        }
        return "redirect:/orders?customerId=" + customerId;
    }

    @GetMapping("/printing")
    public String printing(@RequestParam("customerId") Long customerId, Model model, Authentication authentication) {
        Customer customer = customerService.getOwnedCustomer(customerId, authentication.getName());

        Setting settings = settingService.getSettings();

        model.addAttribute("selectedCustomer", customer);
        model.addAttribute("printing", new Printing());
        model.addAttribute("pricePrint", settings.getPricePrint()); // Добавляем pricePrint

        return "printing";
    }

    @PostMapping("/printing")
    public String savePrinting(@ModelAttribute Printing printing, @RequestParam Long customerId,
                               Authentication authentication, RedirectAttributes redirectAttributes) {
        Customer customer = customerService.getOwnedCustomer(customerId, authentication.getName());
        if (printing.getQuantity() == null || printing.getQuantity() <= 0) {
            redirectAttributes.addFlashAttribute("error", "Укажите тираж больше нуля");
            return "redirect:/profile/printing?customerId=" + customerId;
        }

        printing.setCustomer(customer);
        printingService.save(printing); // стоимость считает сам сервис

        return "redirect:/profile?id=" + customerId;
    }


    @GetMapping("/new_order")
    public String showNewOrderForm() {
        Order order = new Order();
        order.setCustomer(new Customer());

        return "new_order";
    }

}
