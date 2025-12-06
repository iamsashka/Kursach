package com.example.clothingstore.service;

import com.example.clothingstore.model.Brand;
import com.example.clothingstore.repository.BrandRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BrandService {

    private final BrandRepository brandRepository;

    public BrandService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }
    public List<Brand> getAllActiveBrands() {
        return brandRepository.findByDeletedFalse();
    }
    public Page<Brand> getAllBrands(Pageable pageable) {
        return brandRepository.findAll(pageable);
    }
    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }
    public Brand getBrandById(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Бренд с ID " + id + " не найден"));
    }
    public Brand createBrand(Brand brand) {
        if (brandRepository.existsByNameAndDeletedFalse(brand.getName())) {
            throw new RuntimeException("Бренд с названием '" + brand.getName() + "' уже существует");
        }
        brand.setDeleted(false);
        return brandRepository.save(brand);
    }
    public Brand updateBrand(Long id, Brand brandDetails) {
        Brand brand = getBrandById(id);
        if (!brand.getName().equals(brandDetails.getName()) &&
                brandRepository.existsByNameAndDeletedFalse(brandDetails.getName())) {
            throw new RuntimeException("Бренд с названием '" + brandDetails.getName() + "' уже существует");
        }

        brand.setName(brandDetails.getName());
        brand.setContactEmail(brandDetails.getContactEmail());
        return brandRepository.save(brand);
    }
    public void softDeleteBrand(Long id) {
        Brand brand = getBrandById(id);
        brand.setDeleted(true);
        brandRepository.save(brand);
    }
    public void hardDeleteBrand(Long id) {
        brandRepository.deleteById(id);
    }

    public List<Brand> searchBrands(String query) {
        return brandRepository.findByNameContainingIgnoreCaseAndDeletedFalse(query);
    }
    public Brand restoreBrand(Long id) {
        Brand brand = getBrandById(id);
        brand.setDeleted(false);
        return brandRepository.save(brand);
    }

    public Optional<Brand> findByName(String name) {
        return brandRepository.findByNameAndDeletedFalse(name);
    }

    public long getTotalBrandsCount() {
        return brandRepository.countTotalBrands();
    }

    public long getActiveBrandsCount() {
        return brandRepository.countActiveBrands();
    }

    public long getArchivedBrandsCount() {
        return brandRepository.countArchivedBrands();
    }

    public Page<Brand> getArchivedBrands(Pageable pageable) {
        return brandRepository.findByDeletedTrue(pageable);
    }

    public void exportToExcel(List<Brand> brands, HttpServletResponse response) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Бренды");

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Название", "Email", "Статус"};

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

            int rowNum = 1;
            for (Brand brand : brands) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(brand.getId());
                row.createCell(1).setCellValue(brand.getName());
                row.createCell(2).setCellValue(brand.getContactEmail() != null ? brand.getContactEmail() : "");
                row.createCell(3).setCellValue(brand.isDeleted() ? "Удален" : "Активен");
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }
    }
    public void exportToPdf(List<Brand> brands, HttpServletResponse response) throws IOException {
        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Paragraph title = new Paragraph("Отчет по брендам - " + LocalDate.now(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);

            String[] headers = {"ID", "Название", "Email", "Статус"};
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);

            for (String header : headers) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
                headerCell.setGrayFill(0.8f);
                headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                headerCell.setPadding(5);
                table.addCell(headerCell);
            }

            Font dataFont = new Font(Font.HELVETICA, 8, Font.NORMAL);

            for (Brand brand : brands) {
                addTableCell(table, String.valueOf(brand.getId()), dataFont);
                addTableCell(table, brand.getName(), dataFont);
                addTableCell(table, brand.getContactEmail() != null ? brand.getContactEmail() : "", dataFont);
                addTableCell(table, brand.isDeleted() ? "Удален" : "Активен", dataFont);
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

    public Page<Brand> getActiveBrands(Pageable pageable) {
        return brandRepository.findByDeletedFalse(pageable);
    }

    public Page<Brand> searchBrands(String search, Pageable pageable) {
        return brandRepository.searchBrands(search, pageable);
    }
}