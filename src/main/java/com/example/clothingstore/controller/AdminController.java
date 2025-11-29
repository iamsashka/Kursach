package com.example.clothingstore.controller;

import com.example.clothingstore.dto.AnalyticsDTO;
import com.example.clothingstore.model.OrderStatus;
import com.example.clothingstore.config.AnalyticsConfig;
import com.example.clothingstore.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AnalyticsService analyticsService;
    private final PdfExportService pdfExportService;
    private final ExcelImportService excelImportService;
    private final AnalyticsConfig analyticsConfig;

    @GetMapping("/statistics")
    public String statistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        try {
            AnalyticsDTO analytics = analyticsService.getDashboardAnalytics(startDate, endDate);

            // Гарантируем, что все поля не null
            if (analytics.getTotalUsers() == null) analytics.setTotalUsers(0L);
            if (analytics.getTotalOrders() == null) analytics.setTotalOrders(0L);
            if (analytics.getTotalRevenue() == null) analytics.setTotalRevenue(BigDecimal.ZERO);
            if (analytics.getAverageOrderValue() == null) analytics.setAverageOrderValue(BigDecimal.ZERO);
            if (analytics.getConversionRate() == null) analytics.setConversionRate(0.0);
            if (analytics.getDailyOrders() == null) analytics.setDailyOrders(new HashMap<>());
            if (analytics.getTopSellingProducts() == null) analytics.setTopSellingProducts(new HashMap<>());
            if (analytics.getRevenueByCategory() == null) analytics.setRevenueByCategory(new HashMap<>());

            // Подготаваем данные для графиков
            prepareChartData(model, analytics);

            model.addAttribute("analytics", analytics);

        } catch (Exception e) {
            // Создаем пустой объект если сервис падает
            AnalyticsDTO emptyAnalytics = new AnalyticsDTO();
            emptyAnalytics.setTotalUsers(0L);
            emptyAnalytics.setTotalOrders(0L);
            emptyAnalytics.setTotalRevenue(BigDecimal.ZERO);
            emptyAnalytics.setAverageOrderValue(BigDecimal.ZERO);
            emptyAnalytics.setConversionRate(0.0);
            emptyAnalytics.setDailyOrders(new HashMap<>());
            emptyAnalytics.setTopSellingProducts(new HashMap<>());
            emptyAnalytics.setRevenueByCategory(new HashMap<>());

            prepareChartData(model, emptyAnalytics);
            model.addAttribute("analytics", emptyAnalytics);
            log.error("Error loading analytics, using empty data", e);
        }

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "admin/statistics";
    }

    private void prepareChartData(Model model, AnalyticsDTO analytics) {
        // 1. Данные для графика выручки по категориям
        Map<String, BigDecimal> revenueByCategory = analytics.getRevenueByCategory();
        List<String> categoryLabels = new ArrayList<>();
        List<BigDecimal> categoryData = new ArrayList<>();

        if (revenueByCategory != null && !revenueByCategory.isEmpty()) {
            // Используем традиционный for-loop вместо лямбды
            for (Map.Entry<String, BigDecimal> entry : revenueByCategory.entrySet()) {
                categoryLabels.add(entry.getKey());
                categoryData.add(entry.getValue());
            }
        } else {
            // Демо-данные если нет реальных
            categoryLabels = Arrays.asList("Одежда", "Обувь", "Аксессуары", "Электроника", "Книги");
            categoryData = Arrays.asList(
                    new BigDecimal("150000"),
                    new BigDecimal("80000"),
                    new BigDecimal("45000"),
                    new BigDecimal("120000"),
                    new BigDecimal("30000")
            );
        }

        model.addAttribute("categoryLabels", categoryLabels);
        model.addAttribute("categoryData", categoryData);

        // Преобразуем BigDecimal в Double для JavaScript
        List<Double> categoryDataNumbers = categoryData.stream()
                .map(BigDecimal::doubleValue)
                .collect(Collectors.toList());
        model.addAttribute("categoryDataNumbers", categoryDataNumbers);

        // 2. Данные для графика динамики заказов
        Map<String, Long> dailyOrders = analytics.getDailyOrders();
        List<String> dailyLabels = new ArrayList<>();
        List<Long> dailyData = new ArrayList<>();

        if (dailyOrders != null && !dailyOrders.isEmpty()) {
            // Используем традиционный подход
            List<Map.Entry<String, Long>> sortedEntries = new ArrayList<>(dailyOrders.entrySet());
            sortedEntries.sort(Map.Entry.comparingByKey());

            for (Map.Entry<String, Long> entry : sortedEntries) {
                dailyLabels.add(entry.getKey());
                dailyData.add(entry.getValue());
            }
        } else {
            // Демо-данные если нет реальных
            dailyLabels = Arrays.asList("2025-11-01", "2025-11-02", "2025-11-03", "2025-11-04", "2025-11-05");
            dailyData = Arrays.asList(5L, 8L, 12L, 7L, 15L);
        }

        model.addAttribute("dailyLabels", dailyLabels);
        model.addAttribute("dailyData", dailyData);

        // 3. Данные для графика топ товаров
        Map<String, Long> topProducts = analytics.getTopSellingProducts();
        List<String> productLabels = new ArrayList<>();
        List<Long> productData = new ArrayList<>();

        if (topProducts != null && !topProducts.isEmpty()) {
            // Используем традиционный подход
            List<Map.Entry<String, Long>> sortedEntries = new ArrayList<>(topProducts.entrySet());
            sortedEntries.sort(Map.Entry.<String, Long>comparingByValue().reversed());

            for (int i = 0; i < Math.min(5, sortedEntries.size()); i++) {
                Map.Entry<String, Long> entry = sortedEntries.get(i);
                productLabels.add(entry.getKey());
                productData.add(entry.getValue());
            }
        } else {
            // Демо-данные если нет реальных
            productLabels = Arrays.asList("Футболка", "Джинсы", "Кроссовки", "Рюкзак", "Часы");
            productData = Arrays.asList(25L, 18L, 12L, 8L, 5L);
        }

        model.addAttribute("productLabels", productLabels);
        model.addAttribute("productData", productData);

        // 4. Данные для графика каналов (фиксированные)
        model.addAttribute("channelLabels", Arrays.asList("Сайт", "Мобильное приложение", "Соцсети"));
        model.addAttribute("channelData", Arrays.asList(60, 30, 10));

        log.info("Chart data prepared: {} categories, {} daily points, {} top products",
                categoryLabels.size(), dailyLabels.size(), productLabels.size());
    }

    @GetMapping("/analytics/settings")
    public String analyticsSettings(Model model) {
        model.addAttribute("analyticsConfig", analyticsConfig);
        model.addAttribute("allStatuses", OrderStatus.values());
        return "admin/analytics-settings";
    }

    @PostMapping("/analytics/settings")
    public String updateAnalyticsSettings(@RequestParam List<OrderStatus> revenueStatuses,
                                          @RequestParam List<OrderStatus> orderCountStatuses,
                                          RedirectAttributes redirectAttributes) {
        analyticsConfig.setRevenueStatuses(revenueStatuses);
        analyticsConfig.setOrderCountStatuses(orderCountStatuses);

        redirectAttributes.addFlashAttribute("success", "Настройки аналитики обновлены!");
        return "redirect:/admin/analytics/settings";
    }

    @PostMapping("/statistics/import")
    public String importData(@RequestParam("file") MultipartFile file,
                             RedirectAttributes redirectAttributes) {
        try {
            ImportResult result = excelImportService.importFromExcel(file);

            if (result.isSuccess()) {
                redirectAttributes.addFlashAttribute("success",
                        "Импорт завершен! " + result.getSummary());

                // Добавляем детальную информацию по листам
                StringBuilder detailMessage = new StringBuilder();
                Map<String, SheetResult> sheetResults = result.getSheetResults();
                for (Map.Entry<String, SheetResult> entry : sheetResults.entrySet()) {
                    detailMessage.append(entry.getValue().getSummary()).append("; ");
                }
                redirectAttributes.addFlashAttribute("importDetails", detailMessage.toString());

            } else {
                redirectAttributes.addFlashAttribute("error",
                        "Ошибка импорта: " + result.getMessage());

                // Показываем первые 5 ошибок
                List<String> allErrors = new ArrayList<>();
                Map<String, SheetResult> sheetResults = result.getSheetResults();
                for (SheetResult sheetResult : sheetResults.values()) {
                    allErrors.addAll(sheetResult.getErrors());
                }
                if (!allErrors.isEmpty()) {
                    List<String> firstErrors = allErrors.stream().limit(5).collect(Collectors.toList());
                    redirectAttributes.addFlashAttribute("importErrors", firstErrors);
                }
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Ошибка при обработке файла: " + e.getMessage());
        }
        return "redirect:/admin/statistics";
    }

    @GetMapping("/statistics/export/csv")
    @ResponseBody
    public ResponseEntity<String> exportCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        AnalyticsDTO analytics = analyticsService.getDashboardAnalytics(startDate, endDate);
        String csv = analyticsService.exportToCsv(analytics);

        String filename = String.format("analytics_%s_%s.csv", startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/statistics/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        AnalyticsDTO analytics = analyticsService.getDashboardAnalytics(startDate, endDate);
        byte[] pdf = pdfExportService.exportToPdf(analytics, startDate, endDate);

        String filename = String.format("analytics_%s_%s.pdf", startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/statistics/import/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] template = excelImportService.generateTemplate();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"import_template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(template);
    }
}