package com.example.clothingstore.config;

import com.example.clothingstore.service.ThemeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ThemeAdvice {

    private final ThemeService themeService;

    public ThemeAdvice(ThemeService themeService) {
        this.themeService = themeService;
    }

    @ModelAttribute
    public void addThemeToModel(HttpServletRequest request, org.springframework.ui.Model model) {
        String currentTheme = themeService.getCurrentTheme(request);
        model.addAttribute("currentTheme", currentTheme);
    }
}