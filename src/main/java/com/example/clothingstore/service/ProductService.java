package com.example.clothingstore.service;
import com.example.clothingstore.util.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import com.example.clothingstore.model.Product;
import com.example.clothingstore.model.ProductTag;
import com.example.clothingstore.model.TargetAudience;
import com.example.clothingstore.repository.ProductRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final MetricsService metricsService;
    private final AuditService auditService;

    public ProductService(ProductRepository productRepository,MetricsService metricsService,AuditService auditService) {
        this.productRepository = productRepository;
        this.metricsService = metricsService;
        this.auditService = auditService;
        initializeProductsCount();
    }
    private void initializeProductsCount() {
        long count = productRepository.count();
        metricsService.setProductsCount((int) count);
    }
    
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAllByDeletedFalse(pageable);
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product productDetails) {
        Product product = getProductById(id);
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStockQuantity(productDetails.getStockQuantity());
        product.setSizes(productDetails.getSizes());
        product.setBrand(productDetails.getBrand());
        product.setCategory(productDetails.getCategory());
        product.setTargetAudience(productDetails.getTargetAudience());
        product.setCountryOfOrigin(productDetails.getCountryOfOrigin());
        product.setOriginalPrice(productDetails.getOriginalPrice());
        product.setTags(productDetails.getTags());
        product.setViews(productDetails.getViews());
        product.setRating(productDetails.getRating());
        product.setReviewCount(productDetails.getReviewCount());
        return productRepository.save(product);
    }

    public Product getProductById(Long id) {
        Optional<Product> opt = productRepository.findById(id);
        return opt.orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    public void softDeleteProduct(Long id) {
        Product p = getProductById(id);
        p.setDeleted(true);
        productRepository.save(p);
    }

    public void hardDeleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    public Page<Product> searchProducts(String keyword, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCaseAndDeletedFalse(keyword, pageable);
    }
    public Page<Product> filterProducts(String name, Long categoryId, Long brandId,
                                        BigDecimal minPrice, BigDecimal maxPrice,
                                        String color, String size, String country,
                                        TargetAudience audience, ProductTag tag,
                                        Pageable pageable) {
        return productRepository.findByFilters(name, categoryId, brandId, minPrice, maxPrice,
                color, size, country, audience, tag, pageable);
    }
    public Page<Product> filterProductsAlternative(String name, Long categoryId, Long brandId,
                                                   BigDecimal minPrice, BigDecimal maxPrice,
                                                   String color, String size, String country,
                                                   TargetAudience audience, ProductTag tag,
                                                   Pageable pageable) {
        return productRepository.findByFiltersAlternative(name, categoryId, brandId, minPrice, maxPrice,
                color, size, country, audience, tag, pageable);
    }
    public Page<Product> filterProducts(String name, Long categoryId, Long brandId,
                                        Double minPrice, Double maxPrice, Pageable pageable) {
        BigDecimal minPriceBigDecimal = minPrice != null ? BigDecimal.valueOf(minPrice) : null;
        BigDecimal maxPriceBigDecimal = maxPrice != null ? BigDecimal.valueOf(maxPrice) : null;

        return productRepository.findByFilters(name, categoryId, brandId,
                minPriceBigDecimal, maxPriceBigDecimal, null, null, null, null, null, pageable);
    }

    public List<Product> getAllActiveProducts() {
        return productRepository.findByDeletedFalse();
    }

    public List<Product> findByIds(List<Long> ids) {
        return productRepository.findByIdInAndDeletedFalse(ids);
    }

    public Page<Product> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryIdAndDeletedFalse(categoryId, pageable);
    }

    public Page<Product> getProductsByBrand(Long brandId, Pageable pageable) {
        return productRepository.findByBrandIdAndDeletedFalse(brandId, pageable);
    }

    // Методы для главной страницы
    public List<Product> getNewArrivals(int limit) {
        return productRepository.findByTagsContainingAndDeletedFalseOrderByCreatedAtDesc(
                ProductTag.NEW_ARRIVAL, PageRequest.of(0, limit)).getContent();
    }

    public List<Product> getBestSellers(int limit) {
        return productRepository.findByTagsContainingAndDeletedFalse(
                ProductTag.BESTSELLER, PageRequest.of(0, limit)).getContent();
    }

    public List<Product> getSaleProducts(int limit) {
        return productRepository.findByTagsContainingAndDeletedFalse(
                ProductTag.SALE, PageRequest.of(0, limit)).getContent();
    }

    public Page<Product> getProductsByAudience(TargetAudience audience, Pageable pageable) {
        return productRepository.findByTargetAudienceAndDeletedFalse(audience, pageable);
    }

    public Page<Product> getProductsByTag(ProductTag tag, Pageable pageable) {
        return productRepository.findByTagsContainingAndDeletedFalse(tag, pageable);
    }

    public List<String> getAvailableColors() {
        return productRepository.findDistinctColors();
    }

    public List<String> getAvailableSizes() {
        return productRepository.findDistinctSizes();
    }

    public List<String> getAvailableCountries() {
        return productRepository.findDistinctCountries();
    }

    public Page<Product> getDiscountedProducts(Pageable pageable) {
        return productRepository.findDiscountedProducts(pageable);
    }

    public Page<Product> getProductsByDiscountRange(BigDecimal minDiscount, BigDecimal maxDiscount, Pageable pageable) {
        return productRepository.findByDiscountRange(minDiscount, maxDiscount, pageable);
    }

    public Page<Product> getProductsByBrandAndCategory(Long brandId, Long categoryId, Pageable pageable) {
        return productRepository.findByBrandIdAndCategoryIdAndDeletedFalse(brandId, categoryId, pageable);
    }

    public Page<Product> getProductsInStock(Integer minStock, Pageable pageable) {
        return productRepository.findByStockQuantityGreaterThanAndDeletedFalse(minStock, pageable);
    }

    public Page<Product> getPopularProducts(Pageable pageable) {
        return productRepository.findPopularProducts(pageable);
    }

    public Page<Product> getProductsByMultipleTags(List<ProductTag> tags, Pageable pageable) {
        return productRepository.findByMultipleTags(tags, pageable);
    }

    public Page<Product> getProductsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return productRepository.findByCreatedAtBetween(startDate, endDate, pageable);
    }

    public Page<Product> getTopDiscountedProducts(Pageable pageable) {
        return productRepository.findTopDiscountedProducts(pageable);
    }

    public Page<Product> getProductsByRating(Double minRating, Pageable pageable) {
        return productRepository.findByRatingGreaterThanEqual(minRating, pageable);
    }

    public Page<Product> getProductsByColor(String colorName, Pageable pageable) {
        return productRepository.findByColorName(colorName, pageable);
    }

    public Page<Product> getProductsByColors(List<String> colorNames, Pageable pageable) {
        return productRepository.findByColorNames(colorNames, pageable);
    }

    public Long getProductsWithTagsCount() {
        return productRepository.countProductsWithTags();
    }

    public Long getDiscountedProductsCount() {
        return productRepository.countDiscountedProducts();
    }

    public Page<Product> getMostViewedProducts(Pageable pageable) {
        return productRepository.findMostViewedProducts(pageable);
    }

    public List<String> getProductNameSuggestions(String query, int limit) {
        return productRepository.findProductNamesContaining(query, PageRequest.of(0, limit));
    }

    public List<Product> getProductsWithTag(ProductTag tag, int limit) {
        return productRepository.findByTagsContainingAndDeletedFalse(tag, PageRequest.of(0, limit))
                .getContent();
    }
    public void save(Product product) {
        productRepository.save(product);
    }
    public List<Product> getProductsByTagOrderByCreatedAt(ProductTag tag, int limit) {
        return productRepository.findByTagOrderByCreatedAtDesc(tag, PageRequest.of(0, limit));
    }
    public List<Product> getRelatedProducts(Product product) {
        return productRepository.findTop4ByCategoryAndIdNot(product.getCategory(), product.getId());
    }
    public List<Product> getRecommendedProducts(Long productId, int limit) {
        try {
            Product currentProduct = getProductById(productId);

            currentProduct.incrementViews();
            productRepository.save(currentProduct);

            if (currentProduct.getCategory() != null && currentProduct.getBrand() != null) {
                return productRepository.findByBrandIdAndCategoryIdAndDeletedFalse(
                        currentProduct.getBrand().getId(),
                        currentProduct.getCategory().getId(),
                        PageRequest.of(0, limit)
                ).getContent();
            }

            return getPopularProducts(PageRequest.of(0, limit)).getContent();

        } catch (Exception e) {
            return getPopularProducts(PageRequest.of(0, limit)).getContent();
        }
    }
    public boolean isProductAvailable(Long productId, int quantity) {
        try {
            Product product = getProductById(productId);
            return !product.isDeleted() && product.getStockQuantity() >= quantity;
        } catch (Exception e) {
            return false;
        }
    }
    public void updateStockQuantity(Long productId, int quantity) {
        Product product = getProductById(productId);
        if (product.getStockQuantity() >= quantity) {
            product.setStockQuantity(product.getStockQuantity() - quantity);
            productRepository.save(product);
        } else {
            throw new IllegalArgumentException("Недостаточно товара на складе");
        }
    }

    public void increaseStockQuantity(Long productId, int quantity) {
        Product product = getProductById(productId);
        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
    }

    public List<Product> getTopDiscountedProductsList(int limit) {
        return getDiscountedProducts(PageRequest.of(0, limit * 2)).getContent()
                .stream()
                .sorted((p1, p2) -> {
                    BigDecimal discount1 = p1.getDiscountAmount();
                    BigDecimal discount2 = p2.getDiscountAmount();
                    return discount2.compareTo(discount1);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Page<Product> searchProductsAdvanced(String keyword, Long categoryId, Long brandId,
                                                BigDecimal minPrice, BigDecimal maxPrice,
                                                Pageable pageable) {
        return productRepository.findByFilters(keyword, categoryId, brandId, minPrice, maxPrice,
                null, null, null, null, null, pageable);
    }

    public void updateProductRating(Long productId, Double newRating) {
        Product product = getProductById(productId);
        product.updateRating(newRating);
        productRepository.save(product);
    }

    public List<Product> getRecentArrivals(int limit) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return productRepository.findByCreatedAtBetween(
                thirtyDaysAgo, LocalDateTime.now(), PageRequest.of(0, limit)
        ).getContent();
    }

    public List<Product> getLowStockProducts(int threshold) {
        return getAllActiveProducts().stream()
                .filter(p -> p.getStockQuantity() <= threshold && p.getStockQuantity() > 0)
                .collect(Collectors.toList());
    }

    public Page<Product> getTrendingProducts(Pageable pageable) {
        return getPopularProducts(pageable);
    }

    public void bulkUpdateProducts(List<Product> products) {
        productRepository.saveAll(products);
    }

    public boolean isProductNameUnique(String name, Long excludeProductId) {
        List<Product> products = productRepository.findByNameContainingIgnoreCaseAndDeletedFalse(name, PageRequest.of(0, 1)).getContent();
        if (products.isEmpty()) {
            return true;
        }
        return excludeProductId != null && products.get(0).getId().equals(excludeProductId);
    }

    public Product getProductByIdIgnoreDeleted(Long id) {
        return productRepository.findByIdIgnoreDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Товар с ID " + id + " не найден"));
    }
    public Page<Product> getProductsByTagAndAudience(ProductTag tag, TargetAudience audience, Pageable pageable) {
        return filterProducts(null, null, null, null, null, null, null, null, audience, tag, pageable);
    }
    public Page<Product> getArchivedProducts(Pageable pageable) {
        return productRepository.findByDeletedTrue(pageable);
    }

    public void restoreProduct(Long id) {
        Product product = getProductById(id);
        product.setDeleted(false);
        productRepository.save(product);
    }

    public long getTotalProductsCount() {
        return productRepository.count();
    }

    public long getActiveProductsCount() {
        return productRepository.countByDeletedFalse();
    }

    public long getArchivedProductsCount() {
        return productRepository.countByDeletedTrue();
    }

    public List<Product> getProductsForExport(String search, Long categoryId, Long brandId, Double minPrice, Double maxPrice) {
        BigDecimal minPriceBigDecimal = minPrice != null ? BigDecimal.valueOf(minPrice) : null;
        BigDecimal maxPriceBigDecimal = maxPrice != null ? BigDecimal.valueOf(maxPrice) : null;

        return productRepository.findByFilters(search, categoryId, brandId,
                minPriceBigDecimal, maxPriceBigDecimal,
                null, null, null, null, null, Pageable.unpaged()).getContent();
    }

    public List<Product> searchProductsForExport(String search) {
        return productRepository.findByNameContainingIgnoreCaseAndDeletedFalse(search);
    }

    public List<Product> getFilteredProductsForExport(Long categoryId, Long brandId, Double minPrice, Double maxPrice) {
        BigDecimal minPriceBigDecimal = minPrice != null ? BigDecimal.valueOf(minPrice) : null;
        BigDecimal maxPriceBigDecimal = maxPrice != null ? BigDecimal.valueOf(maxPrice) : null;

        return productRepository.findBySimpleFilters(null, categoryId, brandId,
                minPriceBigDecimal, maxPriceBigDecimal);
    }

    public List<Product> getAllProductsForExport() {
        return productRepository.findByDeletedFalse();
    }

    public void exportToExcel(List<Product> products, HttpServletResponse response) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Товары");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Название", "Цена", "Количество", "Категория", "Бренд", "Статус"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(product.getId());
                row.createCell(1).setCellValue(product.getName());
                row.createCell(2).setCellValue(product.getPrice().doubleValue());
                row.createCell(3).setCellValue(product.getStockQuantity());
                row.createCell(4).setCellValue(product.getCategory() != null ? product.getCategory().getName() : "");
                row.createCell(5).setCellValue(product.getBrand() != null ? product.getBrand().getName() : "");
                row.createCell(6).setCellValue(product.isDeleted() ? "Архив" : "Активен");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }
    }

    public void exportToPdf(List<Product> products, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");

        StringBuilder pdfContent = new StringBuilder();
        pdfContent.append("Отчет по товарам\n\n");
        pdfContent.append("Сгенерировано: ").append(java.time.LocalDateTime.now()).append("\n\n");
        pdfContent.append(String.format("%-5s %-30s %-10s %-8s %-15s %-15s %-10s\n",
                "ID", "Название", "Цена", "Кол-во", "Категория", "Бренд", "Статус"));
        pdfContent.append("=".repeat(100)).append("\n");

        for (Product product : products) {
            pdfContent.append(String.format("%-5d %-30s %-10.2f %-8d %-15s %-15s %-10s\n",
                    product.getId(),
                    product.getName().length() > 28 ? product.getName().substring(0, 28) + "..." : product.getName(),
                    product.getPrice().doubleValue(),
                    product.getStockQuantity(),
                    product.getCategory() != null ?
                            (product.getCategory().getName().length() > 12 ?
                                    product.getCategory().getName().substring(0, 12) + "..." :
                                    product.getCategory().getName()) : "-",
                    product.getBrand() != null ?
                            (product.getBrand().getName().length() > 12 ?
                                    product.getBrand().getName().substring(0, 12) + "..." :
                                    product.getBrand().getName()) : "-",
                    product.isDeleted() ? "Архив" : "Активен"));
        }

        pdfContent.append("\n\nВсего товаров: ").append(products.size());

        response.getWriter().write(pdfContent.toString());
    }

    public List<Product> getAllArchivedProducts() {
        return productRepository.findByDeletedTrue();
    }


    public Product createProduct(Product product, HttpServletRequest request) {
        Product savedProduct = productRepository.save(product);

        auditService.logAction("CREATE", "PRODUCT", savedProduct.getId(),
                null, JsonUtils.toJson(savedProduct), request);

        return savedProduct;
    }

    public Product updateProduct(Long id, Product productDetails, HttpServletRequest request) {
        Product product = getProductById(id);

        String oldValues = JsonUtils.toJson(product);

        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStockQuantity(productDetails.getStockQuantity());
        product.setSizes(productDetails.getSizes());
        product.setBrand(productDetails.getBrand());
        product.setCategory(productDetails.getCategory());
        product.setTargetAudience(productDetails.getTargetAudience());
        product.setCountryOfOrigin(productDetails.getCountryOfOrigin());
        product.setOriginalPrice(productDetails.getOriginalPrice());
        product.setTags(productDetails.getTags());

        Product savedProduct = productRepository.save(product);

        String newValues = JsonUtils.toJson(savedProduct);
        auditService.logAction("UPDATE", "PRODUCT", id, oldValues, newValues, request);

        return savedProduct;
    }

    public void softDeleteProduct(Long id, HttpServletRequest request) {
        Product product = getProductById(id);
        String oldValues = JsonUtils.toJson(product);

        product.setDeleted(true);
        productRepository.save(product);

        auditService.logAction("SOFT_DELETE", "PRODUCT", id, oldValues,
                "{\"deleted\": true}", request);
    }

    public void restoreProduct(Long id, HttpServletRequest request) {
        Product product = getProductById(id);
        String oldValues = JsonUtils.toJson(product);

        product.setDeleted(false);
        productRepository.save(product);

        auditService.logAction("RESTORE", "PRODUCT", id, oldValues,
                "{\"deleted\": false}", request);
    }

    public void hardDeleteProduct(Long id, HttpServletRequest request) {
        Product product = getProductById(id);
        String oldValues = JsonUtils.toJson(product);

        productRepository.deleteById(id);

        auditService.logAction("HARD_DELETE", "PRODUCT", id, oldValues, null, request);
    }
}