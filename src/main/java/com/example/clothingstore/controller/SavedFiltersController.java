package com.example.clothingstore.controller;

import com.example.clothingstore.model.User;
import com.example.clothingstore.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/catalog")
public class SavedFiltersController {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    public SavedFiltersController(UserService userService) {
        this.userService = userService;
        this.objectMapper = new ObjectMapper();
    }

    // Автоматическое сохранение фильтров при каждом использовании каталога
    // Этот метод нужно добавить в ваш CatalogController
    public void saveCurrentFilters(Authentication authentication,
                                   Map<String, Object> currentFilters) {
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                Optional<User> user = userService.findByEmail(authentication.getName());
                if (user.isPresent()) {
                    String savedFilters = objectMapper.writeValueAsString(currentFilters);
                    user.get().setSavedFilters(savedFilters);
                    userService.save(user.get());
                }
            } catch (Exception e) {
                System.out.println("Ошибка сохранения фильтров: " + e.getMessage());
            }
        }
    }

    // Применение сохраненных фильтров
    @GetMapping("/apply-saved-filters")
    public String applySavedFilters(Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Для применения фильтров необходимо авторизоваться");
            return "redirect:/catalog";
        }

        try {
            Optional<User> user = userService.findByEmail(authentication.getName());
            if (user.isPresent() && user.get().getSavedFilters() != null) {
                Map<String, Object> filters = objectMapper.readValue(
                        user.get().getSavedFilters(), Map.class
                );

                // Формируем URL с параметрами фильтров
                StringBuilder url = new StringBuilder("redirect:/catalog?");

                if (filters.containsKey("search")) {
                    url.append("search=").append(filters.get("search")).append("&");
                }
                if (filters.containsKey("categoryId")) {
                    url.append("categoryId=").append(filters.get("categoryId")).append("&");
                }
                if (filters.containsKey("brandId")) {
                    url.append("brandId=").append(filters.get("brandId")).append("&");
                }
                if (filters.containsKey("minPrice")) {
                    url.append("minPrice=").append(filters.get("minPrice")).append("&");
                }
                if (filters.containsKey("maxPrice")) {
                    url.append("maxPrice=").append(filters.get("maxPrice")).append("&");
                }

                redirectAttributes.addFlashAttribute("success", "Сохраненные фильтры применены");
                return url.toString();
            } else {
                redirectAttributes.addFlashAttribute("error", "Сохраненные фильтры не найдены");
                return "redirect:/catalog";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка применения фильтров");
            return "redirect:/catalog";
        }
    }

    // Очистка сохраненных фильтров
    @GetMapping("/clear-saved-filters")
    public String clearSavedFilters(Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Для очистки фильтров необходимо авторизоваться");
            return "redirect:/settings";
        }

        try {
            Optional<User> user = userService.findByEmail(authentication.getName());
            if (user.isPresent()) {
                user.get().setSavedFilters(null);
                userService.save(user.get());
                redirectAttributes.addFlashAttribute("success", "Фильтры успешно очищены");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка очистки фильтров");
        }

        return "redirect:/settings";
    }
}