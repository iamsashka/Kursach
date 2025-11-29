package com.example.clothingstore.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
public class ThemeService {

    private static final String THEME_ATTRIBUTE = "currentTheme";
    private static final String DEFAULT_THEME = "light";

    // Допустимые темы - легко расширяемо
    private static final String[] ALLOWED_THEMES = {"light", "dark"};

    public String getCurrentTheme(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return DEFAULT_THEME;

        Object theme = session.getAttribute(THEME_ATTRIBUTE);
        return theme != null ? theme.toString() : DEFAULT_THEME;
    }

    public void toggleTheme(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String currentTheme = getCurrentTheme(request);
        String newTheme = DEFAULT_THEME.equals(currentTheme) ? "dark" : DEFAULT_THEME;
        session.setAttribute(THEME_ATTRIBUTE, newTheme);
    }

    public void setTheme(HttpServletRequest request, String theme) {
        if (!StringUtils.hasText(theme)) return;

        // Проверяем что тема допустимая
        boolean isValidTheme = false;
        for (String allowedTheme : ALLOWED_THEMES) {
            if (allowedTheme.equals(theme)) {
                isValidTheme = true;
                break;
            }
        }

        if (isValidTheme) {
            HttpSession session = request.getSession(true);
            session.setAttribute(THEME_ATTRIBUTE, theme);
        }
    }
    public Map<String, String> getHotkeys() {
        return Map.of(
                "Ctrl+E", "Экспорт данных",
                "Ctrl+I", "Импорт данных",
                "Ctrl+F", "Поиск/фильтр",
                "Ctrl+P", "Печать/PDF",
                "Ctrl+T", "Переключить тему",
                "Ctrl+R", "Обновить данные",
                "Ctrl+S", "Сохранить отчет",
                "Ctrl+D", "Панель управления"
        );
    }

    // Дополнительный метод для проверки
    public boolean isDarkTheme(HttpServletRequest request) {
        return "dark".equals(getCurrentTheme(request));
    }
}