package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.entity.FaqEntry;
import com.bob.ecommerceangularapp.entity.SiteBanner;
import com.bob.ecommerceangularapp.service.ContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public reads for the CMS-managed content (roadmap #17): the site banner and the FAQ list. */
@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/banner")
    public ResponseEntity<SiteBanner> banner() {
        return contentService.getActiveBanner()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/faq")
    public List<FaqEntry> faq() {
        return contentService.listActiveFaq();
    }
}
