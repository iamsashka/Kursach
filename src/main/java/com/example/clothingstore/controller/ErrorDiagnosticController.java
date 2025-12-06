package com.example.clothingstore.controller;

import com.example.clothingstore.model.CartItem;
import com.example.clothingstore.model.Product;
import com.example.clothingstore.model.User;
import com.example.clothingstore.repository.CartItemRepository;
import com.example.clothingstore.repository.ProductRepository;
import com.example.clothingstore.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ErrorDiagnosticController {

    @Autowired
    private Environment environment;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/diagnostic")
    public String diagnosticPage(HttpServletRequest request, Model model,
                                 @RequestParam(required = false) String test) {
        try {
            Map<String, Object> diagnostics = new LinkedHashMap<>();

            // 1. Информация о системе и времени
            diagnostics.put("=== СИСТЕМНАЯ ИНФОРМАЦИЯ ===", getSystemInfo());

            // 2. Информация о сессии и аутентификации
            diagnostics.put("=== АУТЕНТИФИКАЦИЯ И СЕССИЯ ===", getAuthAndSessionInfo(request));

            // 3. Информация о базе данных
            diagnostics.put("=== БАЗА ДАННЫХ ===", getDatabaseInfo());

            // 4. Информация о корзине (если пользователь аутентифицирован)
            diagnostics.put("=== КОРЗИНА ПОЛЬЗОВАТЕЛЯ ===", getCartDiagnostics());

            // 5. Информация о запросе
            diagnostics.put("=== HTTP ЗАПРОС ===", getRequestInfo(request));

            // 6. Переменные окружения
            diagnostics.put("=== ПЕРЕМЕННЫЕ ОКРУЖЕНИЯ ===", getEnvironmentInfo());

            // 7. Тестовые операции
            if ("cart".equals(test)) {
                diagnostics.put("=== ТЕСТ КОРЗИНЫ ===", testCartOperations());
            }
            if ("db".equals(test)) {
                diagnostics.put("=== ТЕСТ БАЗЫ ДАННЫХ ===", testDatabaseOperations());
            }

            model.addAttribute("diagnostics", diagnostics);
            model.addAttribute("currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            return "diagnostic-detailed";

        } catch (Exception e) {
            model.addAttribute("error", "Ошибка в диагностическом контроллере: " + e.getMessage());
            model.addAttribute("stackTrace", getStackTrace(e));
            return "error";
        }
    }

    private Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("Время сервера", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        info.put("Временная зона", TimeZone.getDefault().getID());
        info.put("Java версия", System.getProperty("java.version"));
        info.put("JVM", System.getProperty("java.vm.name"));
        info.put("ОС", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        info.put("Архитектура", System.getProperty("os.arch"));
        info.put("Пользователь", System.getProperty("user.name"));
        info.put("Рабочая директория", System.getProperty("user.dir"));
        info.put("Доступная память", (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + " MB");
        info.put("Используемая память", ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024)) + " MB");
        return info;
    }

    private Map<String, Object> getAuthAndSessionInfo(HttpServletRequest request) {
        Map<String, Object> info = new LinkedHashMap<>();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            info.put("Аутентифицирован", auth.isAuthenticated());
            info.put("Имя пользователя", auth.getName());
            info.put("Principal класс", auth.getPrincipal().getClass().getSimpleName());
            info.put("Роли", auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(", ")));
            info.put("Детали", auth.getDetails() != null ? auth.getDetails().toString() : "null");
        } else {
            info.put("Аутентифицирован", "НЕТ");
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            info.put("ID сессии", session.getId());
            info.put("Время создания", new Date(session.getCreationTime()));
            info.put("Последний доступ", new Date(session.getLastAccessedTime()));
            info.put("Макс. время бездействия", session.getMaxInactiveInterval() + " сек");

            // Атрибуты сессии
            List<String> sessionAttributes = Collections.list(session.getAttributeNames());
            info.put("Атрибуты сессии", sessionAttributes.isEmpty() ? "Нет атрибутов" : String.join(", ", sessionAttributes));
        } else {
            info.put("Сессия", "Не создана");
        }

        return info;
    }

    private Map<String, Object> getDatabaseInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        try {
            Integer dbCheck = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            info.put("Подключение к БД", dbCheck != null ? "✅ Успешно" : "❌ Ошибка");
            info.put("Пользователи", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class));
            info.put("Товары", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product", Integer.class));
            info.put("Элементы корзины", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cart_item", Integer.class));
            info.put("Категории", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM categories", Integer.class));
            info.put("Бренды", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM brands", Integer.class));

        } catch (Exception e) {
            info.put("Ошибка БД", e.getMessage());
        }
        return info;
    }

    private Map<String, Object> getCartDiagnostics() {
        Map<String, Object> info = new LinkedHashMap<>();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            info.put("Статус", "Пользователь не аутентифицирован");
            return info;
        }

        try {
            Optional<User> userOpt = userRepository.findByEmail(auth.getName());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                info.put("Пользователь ID", user.getId());
                info.put("Email", user.getEmail());

                List<CartItem> cartItems = cartItemRepository.findByUser(user);
                info.put("Всего товаров в корзине", cartItems.size());

                List<Map<String, Object>> itemsDetails = new ArrayList<>();
                BigDecimal total = BigDecimal.ZERO;

                for (int i = 0; i < cartItems.size(); i++) {
                    CartItem item = cartItems.get(i);
                    Map<String, Object> itemInfo = new LinkedHashMap<>();
                    itemInfo.put("№", i + 1);
                    itemInfo.put("ID элемента", item.getId());
                    itemInfo.put("Количество", item.getQuantity());
                    itemInfo.put("Размер", item.getSize() != null ? item.getSize() : "Не указан");
                    itemInfo.put("Цвет", item.getColor() != null ? item.getColor() : "Не указан");

                    if (item.getProduct() != null) {
                        Product product = item.getProduct();
                        itemInfo.put("ID товара", product.getId());
                        itemInfo.put("Название", product.getName());
                        itemInfo.put("Цена", product.getPrice() != null ? product.getPrice() + " ₽" : "Не указана");
                        itemInfo.put("Наличие", product.getStockQuantity() + " шт.");
                        itemInfo.put("Бренд", product.getBrand() != null ? product.getBrand().getName() : "Не указан");

                        Optional<Product> dbProduct = productRepository.findById(product.getId());
                        itemInfo.put("В БД", dbProduct.isPresent() ? "✅ Найден" : "❌ Отсутствует");

                        if (product.getPrice() != null) {
                            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                            total = total.add(itemTotal);
                        }
                    } else {
                        itemInfo.put("Товар", "❌ NULL - ОШИБКА ДАННЫХ");
                    }
                    itemsDetails.add(itemInfo);
                }

                info.put("Общая стоимость", total + " ₽");
                info.put("Детали товаров", itemsDetails);

            } else {
                info.put("Статус", "❌ Пользователь не найден в БД");
            }

        } catch (Exception e) {
            info.put("Ошибка диагностики корзины", e.getMessage());
            info.put("Stack trace", getStackTrace(e));
        }

        return info;
    }

    private Map<String, Object> getRequestInfo(HttpServletRequest request) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("Метод", request.getMethod());
        info.put("URL", request.getRequestURL().toString());
        info.put("URI", request.getRequestURI());
        info.put("Query String", request.getQueryString() != null ? request.getQueryString() : "Нет");
        info.put("Протокол", request.getProtocol());
        info.put("Схема", request.getScheme());
        info.put("Сервер", request.getServerName() + ":" + request.getServerPort());
        info.put("Remote Address", request.getRemoteAddr());
        info.put("Remote Host", request.getRemoteHost());
        info.put("Remote Port", request.getRemotePort());
        info.put("Locale", request.getLocale().toString());

        List<String> headers = Collections.list(request.getHeaderNames()).stream()
                .map(name -> name + ": " + request.getHeader(name))
                .collect(Collectors.toList());
        info.put("Заголовки", headers);

        return info;
    }

    private Map<String, Object> getEnvironmentInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("Активные профили", Arrays.toString(environment.getActiveProfiles()));
        info.put("Профили по умолчанию", Arrays.toString(environment.getDefaultProfiles()));
        info.put("Spring версия", getSpringVersion());
        return info;
    }

    private Map<String, Object> testCartOperations() {
        Map<String, Object> info = new LinkedHashMap<>();
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                Optional<User> userOpt = userRepository.findByEmail(auth.getName());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();

                    List<CartItem> cartItems = cartItemRepository.findByUser(user);
                    info.put("Тест получения корзины", "✅ Успешно - " + cartItems.size() + " элементов");

                    int validItems = 0;
                    int invalidItems = 0;
                    for (CartItem item : cartItems) {
                        if (item.getProduct() != null && productRepository.existsById(item.getProduct().getId())) {
                            validItems++;
                        } else {
                            invalidItems++;
                        }
                    }
                    info.put("Валидные товары", validItems);
                    info.put("Невалидные товары", invalidItems);

                } else {
                    info.put("Тест пользователя", "❌ Пользователь не найден");
                }
            } else {
                info.put("Тест аутентификации", "❌ Пользователь не аутентифицирован");
            }
        } catch (Exception e) {
            info.put("Ошибка теста", e.getMessage());
        }
        return info;
    }

    private Map<String, Object> testDatabaseOperations() {
        Map<String, Object> info = new LinkedHashMap<>();
        try {
            info.put("Тест users", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class) + " записей");
            info.put("Тест product", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product", Integer.class) + " записей");
            info.put("Тест cart_item", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cart_item", Integer.class) + " записей");
            info.put("Тест подключения", "✅ Успешно");
        } catch (Exception e) {
            info.put("Ошибка теста БД", e.getMessage());
        }
        return info;
    }

    private String getSpringVersion() {
        try {
            return org.springframework.core.SpringVersion.getVersion();
        } catch (Exception e) {
            return "Не удалось определить";
        }
    }

    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("    at ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}