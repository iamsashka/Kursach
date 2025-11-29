package com.example.clothingstore.controller;

import com.example.clothingstore.model.Role;
import com.example.clothingstore.model.User;
import com.example.clothingstore.service.MetricsService;
import com.example.clothingstore.service.ThemeService;
import com.example.clothingstore.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final MetricsService metricsService;
    private final ThemeService themeService;

    public AuthController(UserService userService, AuthenticationManager authenticationManager, MetricsService metricsService, ThemeService themeService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.metricsService = metricsService;
        this.themeService = themeService;
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "expired", required = false) String expired,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "registered", required = false) String registered,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "Неверный email или пароль");
        }
        if (expired != null) {
            model.addAttribute("error", "Сессия истекла. Пожалуйста, войдите снова.");
        }
        if (logout != null) {
            model.addAttribute("success", "Вы успешно вышли из системы.");
        }
        if (registered != null) {
            model.addAttribute("success", "Регистрация успешна! Добро пожаловать!");
        }

        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
                               BindingResult bindingResult,
                               Model model,
                               HttpServletRequest request) {

        System.out.println("=== DEBUG: Register attempt ===");
        System.out.println("Email: " + user.getEmail());
        System.out.println("Username: " + user.getUsername());

        if (userService.existsByEmail(user.getEmail())) {
            bindingResult.rejectValue("email", "error.user", "Пользователь с таким email уже существует");
        }
        if (userService.existsByUsername(user.getUsername())) {
            bindingResult.rejectValue("username", "error.user", "Пользователь с таким именем уже существует");
        }

        if (bindingResult.hasErrors()) {
            System.out.println("=== DEBUG: Validation errors ===");
            bindingResult.getAllErrors().forEach(error ->
                    System.out.println("Error: " + error.getDefaultMessage())
            );
            return "auth/register";
        }

        try {
            String rawPassword = user.getPassword();
            user.setEnabled(true);

            User registeredUser = userService.registerUser(user, Role.ROLE_CUSTOMER);
            System.out.println("=== DEBUG: User registered successfully, ID: " + registeredUser.getId());
            metricsService.userLoggedIn();
            authenticateUserAndSetSession(registeredUser.getEmail(), rawPassword, request);
            return "redirect:/home?registered";

        } catch (RuntimeException e) {
            System.out.println("=== DEBUG: Registration error: " + e.getMessage());
            model.addAttribute("error", "Ошибка регистрации: " + e.getMessage());
            model.addAttribute("user", user);
            return "auth/register";
        }
    }
    private void authenticateUserAndSetSession(String email, String rawPassword, HttpServletRequest request) {
        try {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(email, rawPassword);
            authToken.setDetails(new WebAuthenticationDetails(request));

            Authentication authentication = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            System.out.println("=== DEBUG: Auto-login successful for: " + email);

        } catch (Exception e) {
            System.err.println("Автоматическая авторизация не удалась: " + e.getMessage());
        }
    }
}