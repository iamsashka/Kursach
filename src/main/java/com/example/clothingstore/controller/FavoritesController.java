package com.example.clothingstore.controller;

import com.example.clothingstore.model.Favorite;
import com.example.clothingstore.model.Product;
import com.example.clothingstore.model.User;
import com.example.clothingstore.service.FavoriteService;
import com.example.clothingstore.service.MetricsService;
import com.example.clothingstore.service.ProductService;
import com.example.clothingstore.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoritesController {

    private final FavoriteService favoriteService;
    private final UserService userService;
    private final ProductService productService;
    private final MetricsService metricsService;

    @PostMapping(value = "/add", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String addToFavoritesForm(@RequestParam Long productId,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {

                String referer = request.getHeader("Referer");
                redirectAttributes.addFlashAttribute("loginRequired", true);
                redirectAttributes.addFlashAttribute("message", "Для добавления в избранное необходимо авторизоваться");
                return "redirect:/login";
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            Product product = productService.getProductById(productId);
            favoriteService.addToFavorites(user, product);
            redirectAttributes.addFlashAttribute("success", "Товар добавлен в избранное");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/catalog");
    }

    @PostMapping("/{productId}")
    public ResponseEntity<?> addToFavorites(@PathVariable Long productId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {
                return ResponseEntity.status(401).body(createErrorResponse("Требуется авторизация"));
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            Product product = productService.getProductById(productId);

            Favorite favorite = favoriteService.addToFavorites(user, product);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Товар добавлен в избранное");
            response.put("favoriteId", favorite.getId());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(createErrorResponse("Ошибка сервера"));
        }
    }

    @PostMapping("/remove")
    public ResponseEntity<?> removeFromFavorites(@RequestParam Long productId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {
                return ResponseEntity.status(401).body(createErrorResponse("Требуется авторизация"));
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            favoriteService.removeFromFavorites(user, productId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Товар удален из избранного");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(createErrorResponse("Ошибка сервера"));
        }
    }
    @PostMapping("/toggle")
    public String toggleFavorite(@RequestParam Long productId,
                                 @RequestParam String redirectUrl,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {

                redirectAttributes.addFlashAttribute("loginRequired", true);
                redirectAttributes.addFlashAttribute("message", "Для добавления в избранное необходимо авторизоваться");
                return "redirect:/login";
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            Product product = productService.getProductById(productId);

            boolean isCurrentlyFavorite = favoriteService.isProductInFavorites(user, productId);

            if (isCurrentlyFavorite) {
                favoriteService.removeFromFavorites(user, productId);
                redirectAttributes.addFlashAttribute("success", "Товар удален из избранного");
            } else {
                long favoritesCount = favoriteService.getUserFavoritesCount(user);

                if (favoritesCount >= 100) {
                    Favorite oldestFavorite = favoriteService.getOldestFavorite(user);

                    if (oldestFavorite != null) {
                        redirectAttributes.addFlashAttribute("limitReached", true);
                        redirectAttributes.addFlashAttribute("oldestFavoriteId", oldestFavorite.getId());
                        redirectAttributes.addFlashAttribute("oldestProductName", oldestFavorite.getProduct().getName());
                        redirectAttributes.addFlashAttribute("newProductId", productId);
                        redirectAttributes.addFlashAttribute("newProductName", product.getName());
                    } else {
                        redirectAttributes.addFlashAttribute("error", "Достигнут лимит избранного (100 товаров)");
                    }

                    return "redirect:" + redirectUrl;
                } else {
                    favoriteService.addToFavorites(user, product);
                    redirectAttributes.addFlashAttribute("success", "Товар добавлен в избранное");
                }
            }

            boolean isNowFavorite = !isCurrentlyFavorite;
            return "redirect:" + redirectUrl + "?favorite=" + isNowFavorite;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
            return "redirect:" + redirectUrl;
        }
    }

    @PostMapping("/replace-oldest")
    public String replaceOldestFavorite(@RequestParam Long newProductId,
                                        @RequestParam Long oldestFavoriteId,
                                        RedirectAttributes redirectAttributes) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {
                return "redirect:/login";
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
            favoriteService.removeFavoriteById(oldestFavoriteId);
            Product newProduct = productService.getProductById(newProductId);
            favoriteService.addToFavorites(user, newProduct);

            redirectAttributes.addFlashAttribute("success", "Товар добавлен в избранное (самый старый товар удален)");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }

        return "redirect:/api/favorites/page";
    }
    @PostMapping("/check-multiple")
    public ResponseEntity<?> checkMultipleFavorites(@RequestBody List<Long> productIds) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {
                return ResponseEntity.ok(Collections.emptyMap());
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            Map<Long, Boolean> favoritesMap = new HashMap<>();
            for (Long productId : productIds) {
                boolean isFavorite = favoriteService.isProductInFavorites(user, productId);
                favoritesMap.put(productId, isFavorite);
            }

            return ResponseEntity.ok(favoritesMap);

        } catch (Exception e) {
            return ResponseEntity.ok(Collections.emptyMap());
        }
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<?> removeFromFavoritesApi(@PathVariable Long productId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {
                return ResponseEntity.status(401).body(createErrorResponse("Требуется авторизация"));
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            favoriteService.removeFromFavorites(user, productId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Товар удален из избранного");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(createErrorResponse("Ошибка сервера"));
        }
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<?> checkFavorite(@PathVariable Long productId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {
                return ResponseEntity.ok(createCheckResponse(false));
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            boolean isFavorite = favoriteService.isProductInFavorites(user, productId);

            return ResponseEntity.ok(createCheckResponse(isFavorite));

        } catch (Exception e) {
            return ResponseEntity.ok(createCheckResponse(false));
        }
    }
    @GetMapping("/page")
    public String favoritesPage(Model model) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {
                return "redirect:/login";
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            List<Favorite> favorites = favoriteService.getUserFavorites(user);
            model.addAttribute("favorites", favorites != null ? favorites : Collections.emptyList());
            model.addAttribute("title", "Мои избранные товары");
            model.addAttribute("currentPage", "/api/favorites/page");

            return "favorites";

        } catch (Exception e) {
            model.addAttribute("error", "Ошибка загрузки избранных товаров: " + e.getMessage());
            model.addAttribute("favorites", Collections.emptyList());
            return "favorites";
        }
    }
    @PostMapping("/add-redirect")
    public String addToFavoritesRedirect(@RequestParam Long productId,
                                         RedirectAttributes redirectAttributes) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {
                return "redirect:/login";
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            Product product = productService.getProductById(productId);
            favoriteService.addToFavorites(user, product);

            redirectAttributes.addFlashAttribute("success", "Товар добавлен в избранное");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }

        return "redirect:/catalog";
    }

    @PostMapping("/remove-redirect")
    public String removeFromFavoritesRedirect(@RequestParam Long productId,
                                              RedirectAttributes redirectAttributes) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {
                return "redirect:/login";
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            favoriteService.removeFromFavorites(user, productId);

            redirectAttributes.addFlashAttribute("success", "Товар удален из избранного");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }

        return "redirect:/api/favorites/page";
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    private Map<String, Object> createCheckResponse(boolean isFavorite) {
        Map<String, Object> response = new HashMap<>();
        response.put("isFavorite", isFavorite);
        return response;
    }
}