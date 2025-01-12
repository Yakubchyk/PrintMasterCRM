package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.HtmlContent;
import com.springboot.printmastercrm.repository.HtmlContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HtmlContentService {

    @Autowired
    private HtmlContentRepository htmlContentRepository;
    public HtmlContent getHtmlContentById(Long id) {
        return htmlContentRepository.findById(id).orElse(null);
    }
    public List<HtmlContent> getAllHtmlContents() {
        return htmlContentRepository.findAll();
    }

    public HtmlContent saveContent(String htmlContent) {
        HtmlContent content = new HtmlContent();
        content.setContent(htmlContent);
        return htmlContentRepository.save(content);
    }

    public HtmlContent getContentById(Long id) {
        return htmlContentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Content not found with ID: " + id));
    }

}
