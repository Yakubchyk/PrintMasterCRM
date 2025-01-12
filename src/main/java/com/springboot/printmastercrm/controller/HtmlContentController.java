package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.service.HtmlContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/html")
public class HtmlContentController {

    @Autowired
    private HtmlContentService htmlContentService;

    @PostMapping("/save")
    public String saveHtmlContent(Model model) {


        return "profile";
    }

}