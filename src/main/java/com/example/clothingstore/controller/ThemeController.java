package com.example.clothingstore.controller;

import com.example.clothingstore.service.ThemeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class ThemeController {

    private final ThemeService themeService;

    public ThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    @GetMapping("/toggle-theme")
    public String toggleTheme(HttpServletRequest request) {
        themeService.toggleTheme(request);
        return "redirect:" + Optional.ofNullable(request.getHeader("Referer"))
                .orElse("/");
    }
}