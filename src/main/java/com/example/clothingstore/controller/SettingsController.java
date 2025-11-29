package com.example.clothingstore.controller;

import com.example.clothingstore.model.User;
import com.example.clothingstore.service.ThemeService;
import com.example.clothingstore.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SettingsController {

    private final UserService userService;
    private final ThemeService themeService;
    private final PasswordEncoder passwordEncoder;

    public SettingsController(UserService userService, ThemeService themeService,PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.themeService = themeService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/settings")
    public String getSettings(Model model, Authentication authentication, HttpServletRequest request) {
        try {
            // Получаем текущего пользователя
            User currentUser = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            // Передаем пользователя в модель
            model.addAttribute("user", currentUser);

            // Передаем текущую тему для навигации
            String currentTheme = themeService.getCurrentTheme(request);
            model.addAttribute("currentTheme", currentTheme);

            return "settings";

        } catch (Exception e) {
            model.addAttribute("error", "Ошибка загрузки настроек: " + e.getMessage());

            // Создаем временного пользователя для отображения формы
            User tempUser = new User();
            tempUser.setTheme("light");
            tempUser.setDateFormat("dd.MM.yyyy");
            tempUser.setNumberFormat("COMMA");
            tempUser.setPageSize(10);
            tempUser.setSavedFilters("{}");
            model.addAttribute("user", tempUser);

            // Передаем тему по умолчанию
            model.addAttribute("currentTheme", "light");

            return "settings";
        }
    }
    @PostMapping("/settings/change-password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            User currentUser = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            // Валидация
            if (!passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
                redirectAttributes.addFlashAttribute("error", "Текущий пароль неверен");
                return "redirect:/settings";
            }

            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Новые пароли не совпадают");
                return "redirect:/settings";
            }

            if (newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "Новый пароль должен содержать минимум 6 символов");
                return "redirect:/settings";
            }

            // Обновляем пароль
            currentUser.setPassword(passwordEncoder.encode(newPassword));
            userService.save(currentUser);

            redirectAttributes.addFlashAttribute("success", "Пароль успешно изменен");
            return "redirect:/settings";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при смене пароля: " + e.getMessage());
            return "redirect:/settings";
        }
    }
    @GetMapping("/forgot-password")
    public String forgotPassword(Model model, HttpServletRequest request) {
        String currentTheme = themeService.getCurrentTheme(request);
        model.addAttribute("currentTheme", currentTheme);

        return "forgot-password";
    }
    @PostMapping("/settings/update")
    public String updateSettings(
            @RequestParam String theme,
            @RequestParam String dateFormat,
            @RequestParam String numberFormat,
            @RequestParam Integer pageSize,
            @RequestParam String savedFilters,
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            User currentUser = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            // Сохраняем настройки
            currentUser.setTheme(theme);
            currentUser.setDateFormat(dateFormat);
            currentUser.setNumberFormat(numberFormat);
            currentUser.setPageSize(pageSize);
            currentUser.setSavedFilters(savedFilters);

            userService.save(currentUser);

            // Обновляем тему в сессии
            themeService.setTheme(request, theme);

            redirectAttributes.addFlashAttribute("success", "Настройки успешно сохранены!");

            // ДОБАВЛЯЕМ: Перенаправляем на каталог для применения новых настроек
            return "redirect:/catalog?settingsApplied=true";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка сохранения настроек: " + e.getMessage());
            return "redirect:/settings";
        }
    }
    // Страница подтверждения удаления аккаунта
    @GetMapping("/confirm-delete-account")
    public String confirmDeleteAccount(Model model, Authentication authentication, HttpServletRequest request) {
        try {
            User currentUser = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            model.addAttribute("user", currentUser);

            // Передаем текущую тему для навигации
            String currentTheme = themeService.getCurrentTheme(request);
            model.addAttribute("currentTheme", currentTheme);

            return "confirm-delete-account";

        } catch (Exception e) {
            return "redirect:/settings?error=user_not_found";
        }
    }

    // Обработка удаления аккаунта
    @PostMapping("/delete-account")
    public String deleteAccount(
            @RequestParam String password,
            @RequestParam(required = false) Boolean confirm1,
            @RequestParam(required = false) Boolean confirm2,
            @RequestParam(required = false) Boolean confirm3,
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            User currentUser = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            // Проверка всех подтверждений
            if (confirm1 == null || confirm2 == null || confirm3 == null) {
                redirectAttributes.addFlashAttribute("error", "Необходимо подтвердить все условия удаления аккаунта");
                return "redirect:/confirm-delete-account";
            }

            // Проверка пароля
            if (!passwordEncoder.matches(password, currentUser.getPassword())) {
                redirectAttributes.addFlashAttribute("error", "Неверный пароль");
                return "redirect:/confirm-delete-account";
            }

            // Мягкое удаление аккаунта (установка флага deleted)
            currentUser.setDeleted(true);
            currentUser.setEnabled(false);
            userService.save(currentUser);

            // Выход из системы
            SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
            logoutHandler.logout(request, null, null);

            redirectAttributes.addFlashAttribute("success", "Ваш аккаунт был успешно удален");
            return "redirect:/login?account_deleted";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении аккаунта: " + e.getMessage());
            return "redirect:/confirm-delete-account";
        }
    }

    // Выход из аккаунта (Spring Security уже обрабатывает /logout по умолчанию)
    // Этот метод для кастомного выхода если нужно
    @PostMapping("/custom-logout")
    public String customLogout(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            // Выход из системы
            SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
            logoutHandler.logout(request, null, null);

            redirectAttributes.addFlashAttribute("success", "Вы успешно вышли из системы");
            return "redirect:/login?logout";

        } catch (Exception e) {
            return "redirect:/login?error=logout_failed";
        }
    }
}