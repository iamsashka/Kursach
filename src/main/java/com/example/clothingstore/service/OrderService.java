package com.example.clothingstore.service;
import com.example.clothingstore.model.Order;
import com.example.clothingstore.model.OrderStatus;
import com.example.clothingstore.model.Product;
import com.example.clothingstore.repository.OrderRepository;
import com.example.clothingstore.repository.ProductRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Autowired(required = false)
    private MetricsService metricsService;

    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAllByDeletedFalse(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Order> getAllOrdersWithDetails(Pageable pageable) {
        Page<Order> orders = getAllOrders(pageable);
        initializeOrdersDetails(orders.getContent());
        return orders;
    }

    @Transactional(readOnly = true)
    public Page<Order> searchOrdersWithDetails(String search, Pageable pageable) {
        Page<Order> orders = searchOrders(search, pageable);
        initializeOrdersDetails(orders.getContent());
        return orders;
    }

    @Transactional(readOnly = true)
    public Page<Order> getOrdersByStatusWithDetails(OrderStatus status, Pageable pageable) {
        Page<Order> orders = getOrdersByStatus(status, pageable);
        initializeOrdersDetails(orders.getContent());
        return orders;
    }

    private void initializeOrdersDetails(List<Order> orders) {
        for (Order order : orders) {
            initializeOrderDetails(order);
        }
    }

    private void initializeOrderDetails(Order order) {
        if (order.getUser() != null) {
            Hibernate.initialize(order.getUser());
            order.getUser().getFirstName();
            order.getUser().getLastName();
            order.getUser().getEmail();
        }

        if (order.getProducts() != null) {
            Hibernate.initialize(order.getProducts());
            for (Product product : order.getProducts()) {
                if (product != null) {
                    product.getName();
                    product.getPrice();
                }
            }
        }
    }

    public Order saveOrder(Order order) {
        if (order.getOrderDate() == null) order.setOrderDate(LocalDateTime.now());
        if (order.getOrderNumber() == null || order.getOrderNumber().isBlank()) {
            order.setOrderNumber(generateOrderNumber());
        }

        Order savedOrder = orderRepository.save(order);

        return savedOrder;
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }
    public BigDecimal calculateTotalRevenue() {
        return orderRepository.sumTotalAmountOfAllOrders();
    }
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден: " + id));
    }

    public void softDeleteOrder(Long id) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден: " + id));

        order.setDeleted(true);
        orderRepository.save(order);
    }

    public long getUserOrdersCount(Long userId) {
        return orderRepository.countByUserIdAndDeletedFalse(userId);
    }

    public BigDecimal getTotalSpentByUser(Long userId) {
        BigDecimal total = orderRepository.getTotalSpentByUserId(userId);
        return total != null ? total : BigDecimal.ZERO;
    }

    public List<Order> getRecentOrdersByUser(Long userId) {
        return orderRepository.findRecentOrdersByUserId(userId);
    }

    public void hardDeleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    public Page<Order> searchOrdersByUserName(String userName, Pageable pageable) {
        if (userName == null || userName.isBlank()) return getAllOrders(pageable);
        return orderRepository.findByUserNameContaining(userName, pageable);
    }

    public Page<Order> getUserOrdersPage(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdAndDeletedFalseOrderByOrderDateDesc(userId, pageable);
    }

    public Page<Order> getUserOrdersPageWithAllProducts(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdAndDeletedFalse(userId, pageable);
    }

    private void calculateTotalAmount(Order order) {
        BigDecimal total = BigDecimal.ZERO;
        if (order.getProducts() != null) {
            for (Product p : order.getProducts()) {
                if (p.getPrice() != null) total = total.add(p.getPrice());
            }
        }
        order.setTotalAmount(total);
    }

    public Page<Order> searchOrders(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return getAllOrders(pageable);
        }
        return orderRepository.searchOrders(search, pageable);
    }

    public Page<Order> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatusAndDeletedFalse(status, pageable);
    }
    public Page<Order> getArchivedOrders(Pageable pageable) {
        return orderRepository.findByDeletedTrue(pageable);
    }
    public void restoreOrder(Long id) {
        Order order = getOrderById(id);
        order.setDeleted(false);
        orderRepository.save(order);
    }
    public long countOrdersByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }
    // Методы для экспорта без пагинации
    public List<Order> getAllOrdersWithDetails() {
        List<Order> orders = orderRepository.findAllWithDetails();
        initializeOrdersDetails(orders);
        return orders;
    }

    public List<Order> searchOrdersWithDetails(String search) {
        List<Order> orders = orderRepository.searchOrdersWithDetails(search);
        initializeOrdersDetails(orders);
        return orders;
    }

    public List<Order> getOrdersByStatusWithDetails(OrderStatus status) {
        List<Order> orders = orderRepository.findByStatusWithDetails(status);
        initializeOrdersDetails(orders);
        return orders;
    }

    // Методы экспорта
    public void exportToExcel(List<Order> orders, HttpServletResponse response) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Заказы");

            // Заголовки
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = {"№ заказа", "Клиент", "Email", "Товары", "Сумма", "Дата", "Статус", "Адрес"};

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Данные
            int rowNum = 1;
            for (Order order : orders) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(order.getOrderNumber());
                row.createCell(1).setCellValue(order.getUser().getFirstName() + " " + order.getUser().getLastName());
                row.createCell(2).setCellValue(order.getUser().getEmail());

                String products = order.getProducts().stream()
                        .map(Product::getName)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                row.createCell(3).setCellValue(products);

                row.createCell(4).setCellValue(order.getTotalAmount().doubleValue());
                row.createCell(5).setCellValue(order.getOrderDate().toString());
                row.createCell(6).setCellValue(order.getStatus().getDisplayName());
                row.createCell(7).setCellValue(order.getShippingAddress() != null ? order.getShippingAddress() : "");
            }

            // Авто-размер колонок
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }
    }

    public void exportToPdf(List<Order> orders, HttpServletResponse response) throws IOException {
        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            // Заголовок
            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Paragraph title = new Paragraph("Отчет по заказам - " + LocalDate.now(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Таблица
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);

            // Заголовки таблицы
            String[] headers = {"№ заказа", "Клиент", "Email", "Товары", "Сумма", "Дата", "Статус", "Адрес"};
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);

            for (String header : headers) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
                headerCell.setGrayFill(0.8f); // Серый фон вместо BaseColor.DARK_GRAY
                headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                headerCell.setPadding(5);
                table.addCell(headerCell);
            }

            // Данные
            Font dataFont = new Font(Font.HELVETICA, 8, Font.NORMAL);

            for (Order order : orders) {
                addTableCell(table, order.getOrderNumber(), dataFont);
                addTableCell(table, order.getUser().getFirstName() + " " + order.getUser().getLastName(), dataFont);
                addTableCell(table, order.getUser().getEmail(), dataFont);

                String products = order.getProducts().stream()
                        .map(Product::getName)
                        .limit(3)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                addTableCell(table, products, dataFont);

                addTableCell(table, order.getTotalAmount() + " ₽", dataFont);
                addTableCell(table, order.getOrderDate().toString(), dataFont);
                addTableCell(table, order.getStatus().getDisplayName(), dataFont);
                addTableCell(table, order.getShippingAddress() != null ? order.getShippingAddress() : "", dataFont);
            }

            document.add(table);
            document.close();

        } catch (DocumentException e) {
            throw new IOException("Ошибка создания PDF", e);
        }
    }

    private void addTableCell(PdfPTable table, String content, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }
}