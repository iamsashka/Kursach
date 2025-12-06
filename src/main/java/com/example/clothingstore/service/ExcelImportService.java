package com.example.clothingstore.service;

import com.example.clothingstore.model.*;
import com.example.clothingstore.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelImportService {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public ImportResult importFromExcel(MultipartFile file) {
        ImportResult result = new ImportResult();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName().toLowerCase();

                switch (sheetName) {
                    case "categories":
                        result.addSheetResult("categories", importCategories(sheet));
                        break;
                    case "brands":
                        result.addSheetResult("brands", importBrands(sheet));
                        break;
                    case "products":
                        result.addSheetResult("products", importProducts(sheet));
                        break;
                    case "users":
                        result.addSheetResult("users", importUsers(sheet));
                        break;
                    default:
                        log.warn("Неизвестный лист: {}", sheetName);
                }
            }

            result.setSuccess(true);
            result.setMessage("Импорт завершен успешно");

        } catch (Exception e) {
            log.error("Ошибка импорта данных из Excel", e);
            result.setSuccess(false);
            result.setMessage("Ошибка импорта: " + e.getMessage());
        }

        return result;
    }
    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet categoriesSheet = workbook.createSheet("Categories");
            Row categoryHeader = categoriesSheet.createRow(0);
            categoryHeader.createCell(0).setCellValue("Название");
            categoryHeader.createCell(1).setCellValue("Описание");

            Sheet brandsSheet = workbook.createSheet("Brands");
            Row brandHeader = brandsSheet.createRow(0);
            brandHeader.createCell(0).setCellValue("Название");
            brandHeader.createCell(1).setCellValue("Email");

            Sheet productsSheet = workbook.createSheet("Products");
            Row productHeader = productsSheet.createRow(0);
            productHeader.createCell(0).setCellValue("Название");
            productHeader.createCell(1).setCellValue("Описание");
            productHeader.createCell(2).setCellValue("Цена");
            productHeader.createCell(3).setCellValue("Количество");
            productHeader.createCell(4).setCellValue("Категория");
            productHeader.createCell(5).setCellValue("Бренд");
            productHeader.createCell(6).setCellValue("Размеры (через запятую)");
            productHeader.createCell(7).setCellValue("Аудитория (MEN, WOMEN, UNISEX, etc)");
            productHeader.createCell(8).setCellValue("Страна");
            productHeader.createCell(9).setCellValue("Оригинальная цена");
            productHeader.createCell(10).setCellValue("Теги (через запятую: NEW_ARRIVAL, SALE, etc)");

            Sheet usersSheet = workbook.createSheet("Users");
            Row userHeader = usersSheet.createRow(0);
            userHeader.createCell(0).setCellValue("Email");
            userHeader.createCell(1).setCellValue("Username");
            userHeader.createCell(2).setCellValue("Password");
            userHeader.createCell(3).setCellValue("Имя");
            userHeader.createCell(4).setCellValue("Фамилия");
            userHeader.createCell(5).setCellValue("Телефон");
            userHeader.createCell(6).setCellValue("Роли (через запятую: ROLE_CUSTOMER, ROLE_ADMIN)");

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                for (int j = 0; j < sheet.getRow(0).getLastCellNum(); j++) {
                    sheet.autoSizeColumn(j);
                }
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Ошибка создания шаблона", e);
            return new byte[0];
        }
    }
    private SheetResult importCategories(Sheet sheet) {
        SheetResult result = new SheetResult("Категории");

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;

            try {
                String name = getCellStringValue(row.getCell(0));
                String description = getCellStringValue(row.getCell(1));

                if (name == null || name.trim().isEmpty()) {
                    result.addError("Строка " + (i + 1) + ": Отсутствует название категории");
                    continue;
                }

                if (categoryRepository.findByNameAndDeletedFalse(name).isPresent()) {
                    result.addError("Строка " + (i + 1) + ": Категория '" + name + "' уже существует");
                    continue;
                }

                Category category = new Category();
                category.setName(name.trim());
                category.setDescription(description != null ? description.trim() : null);
                category.setDeleted(false);

                categoryRepository.save(category);
                result.incrementSuccessCount();

            } catch (Exception e) {
                result.addError("Строка " + (i + 1) + ": " + e.getMessage());
            }
        }

        return result;
    }

    private SheetResult importBrands(Sheet sheet) {
        SheetResult result = new SheetResult("Бренды");

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;

            try {
                String name = getCellStringValue(row.getCell(0));
                String email = getCellStringValue(row.getCell(1));

                if (name == null || name.trim().isEmpty()) {
                    result.addError("Строка " + (i + 1) + ": Отсутствует название бренда");
                    continue;
                }

                if (brandRepository.existsByNameAndDeletedFalse(name)) {
                    result.addError("Строка " + (i + 1) + ": Бренд '" + name + "' уже существует");
                    continue;
                }

                Brand brand = new Brand();
                brand.setName(name.trim());
                brand.setContactEmail(email != null ? email.trim() : null);
                brand.setDeleted(false);

                brandRepository.save(brand);
                result.incrementSuccessCount();

            } catch (Exception e) {
                result.addError("Строка " + (i + 1) + ": " + e.getMessage());
            }
        }

        return result;
    }

    private SheetResult importProducts(Sheet sheet) {
        SheetResult result = new SheetResult("Товары");

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;

            try {
                Product product = parseProductFromRow(row, i + 1, result);
                if (product != null) {
                    productRepository.save(product);
                    result.incrementSuccessCount();
                }

            } catch (Exception e) {
                result.addError("Строка " + (i + 1) + ": " + e.getMessage());
            }
        }

        return result;
    }

    private Product parseProductFromRow(Row row, int rowNum, SheetResult result) {
        String name = getCellStringValue(row.getCell(0));
        String description = getCellStringValue(row.getCell(1));
        BigDecimal price = getCellBigDecimalValue(row.getCell(2));
        Integer stockQuantity = getCellIntegerValue(row.getCell(3));
        String categoryName = getCellStringValue(row.getCell(4));
        String brandName = getCellStringValue(row.getCell(5));
        String sizes = getCellStringValue(row.getCell(6));
        String targetAudienceStr = getCellStringValue(row.getCell(7));
        String countryOfOrigin = getCellStringValue(row.getCell(8));
        BigDecimal originalPrice = getCellBigDecimalValue(row.getCell(9));
        String tagsStr = getCellStringValue(row.getCell(10));

        if (name == null || name.trim().isEmpty()) {
            result.addError("Строка " + rowNum + ": Отсутствует название товара");
            return null;
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("Строка " + rowNum + ": Некорректная цена");
            return null;
        }

        if (stockQuantity == null || stockQuantity < 0) {
            result.addError("Строка " + rowNum + ": Некорректное количество на складе");
            return null;
        }

        Category category = categoryRepository.findByNameAndDeletedFalse(categoryName)
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName(categoryName.trim());
                    newCategory.setDeleted(false);
                    return categoryRepository.save(newCategory);
                });

        Brand brand = brandRepository.findByNameAndDeletedFalse(brandName)
                .orElseGet(() -> {
                    Brand newBrand = new Brand();
                    newBrand.setName(brandName.trim());
                    newBrand.setDeleted(false);
                    return brandRepository.save(newBrand);
                });

        Product product = new Product();
        product.setName(name.trim());
        product.setDescription(description != null ? description.trim() : null);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        product.setCategory(category);
        product.setBrand(brand);
        product.setSizes(sizes);
        product.setCountryOfOrigin(countryOfOrigin);
        product.setOriginalPrice(originalPrice);
        product.setDeleted(false);

        if (targetAudienceStr != null) {
            try {
                TargetAudience audience = TargetAudience.valueOf(targetAudienceStr.toUpperCase());
                product.setTargetAudience(audience);
            } catch (IllegalArgumentException e) {
                result.addError("Строка " + rowNum + ": Некорректная целевая аудитория: " + targetAudienceStr);
            }
        }

        if (tagsStr != null) {
            Set<ProductTag> tags = new HashSet<>();
            String[] tagArray = tagsStr.split(",");
            for (String tagStr : tagArray) {
                try {
                    ProductTag tag = ProductTag.valueOf(tagStr.trim().toUpperCase());
                    tags.add(tag);
                } catch (IllegalArgumentException e) {
                    result.addError("Строка " + rowNum + ": Некорректный тег: " + tagStr);
                }
            }
            product.setTags(tags);
        }

        return product;
    }

    private SheetResult importUsers(Sheet sheet) {
        SheetResult result = new SheetResult("Пользователи");

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;

            try {
                String email = getCellStringValue(row.getCell(0));
                String username = getCellStringValue(row.getCell(1));
                String password = getCellStringValue(row.getCell(2));
                String firstName = getCellStringValue(row.getCell(3));
                String lastName = getCellStringValue(row.getCell(4));
                String phone = getCellStringValue(row.getCell(5));
                String rolesStr = getCellStringValue(row.getCell(6));

                if (email == null || email.trim().isEmpty()) {
                    result.addError("Строка " + (i + 1) + ": Отсутствует email");
                    continue;
                }

                if (username == null || username.trim().isEmpty()) {
                    result.addError("Строка " + (i + 1) + ": Отсутствует имя пользователя");
                    continue;
                }

                if (userRepository.existsByEmailAndDeletedFalse(email)) {
                    result.addError("Строка " + (i + 1) + ": Пользователь с email '" + email + "' уже существует");
                    continue;
                }

                User user = new User();
                user.setEmail(email.trim());
                user.setUsername(username.trim());
                user.setPassword(password != null ? password.trim() : "defaultPassword123");
                user.setFirstName(firstName != null ? firstName.trim() : null);
                user.setLastName(lastName != null ? lastName.trim() : null);
                user.setPhone(phone);
                user.setEnabled(true);
                user.setDeleted(false);
                user.setCreatedAt(LocalDateTime.now());

                List<Role> roles = new ArrayList<>();
                if (rolesStr != null) {
                    String[] roleArray = rolesStr.split(",");
                    for (String roleStr : roleArray) {
                        try {
                            Role role = Role.valueOf(roleStr.trim().toUpperCase());
                            roles.add(role);
                        } catch (IllegalArgumentException e) {
                            result.addError("Строка " + (i + 1) + ": Некорректная роль: " + roleStr);
                        }
                    }
                }

                if (roles.isEmpty()) {
                    roles.add(Role.ROLE_CUSTOMER);
                }

                user.setRoles(roles);

                userRepository.save(user);
                result.incrementSuccessCount();

            } catch (Exception e) {
                result.addError("Строка " + (i + 1) + ": " + e.getMessage());
            }
        }

        return result;
    }
    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    private Integer getCellIntegerValue(Cell cell) {
        try {
            if (cell == null) return null;

            switch (cell.getCellType()) {
                case NUMERIC:
                    return (int) cell.getNumericCellValue();
                case STRING:
                    return Integer.parseInt(cell.getStringCellValue().trim());
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal getCellBigDecimalValue(Cell cell) {
        try {
            if (cell == null) return null;

            switch (cell.getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    return value.isEmpty() ? null : new BigDecimal(value);
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isEmptyRow(Row row) {
        if (row == null) return true;

        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellStringValue(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}