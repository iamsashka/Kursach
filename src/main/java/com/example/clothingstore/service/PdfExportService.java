package com.example.clothingstore.service;

import com.example.clothingstore.dto.AnalyticsDTO;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PdfExportService {

    public byte[] exportToPdf(AnalyticsDTO analytics, LocalDate startDate, LocalDate endDate) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            addTitle(document, startDate, endDate);
            addMetricsTable(document, analytics);
            addCharts(document, analytics);
            addTopProductsTable(document, analytics);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Ошибка генерации PDF", e);
        }
    }

    private void addTitle(Document document, LocalDate startDate, LocalDate endDate) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
        Paragraph title = new Paragraph("АНАЛИТИЧЕСКИЙ ОТЧЕТ TARENO", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        Font periodFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.GRAY);
        String period = String.format("Период: %s - %s",
                startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

        Paragraph periodPara = new Paragraph(period, periodFont);
        periodPara.setAlignment(Element.ALIGN_CENTER);
        periodPara.setSpacingAfter(20);
        document.add(periodPara);
    }

    private void addMetricsTable(Document document, AnalyticsDTO analytics) throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(20);

        PdfPCell headerCell = new PdfPCell(new Phrase("КЛЮЧЕВЫЕ ПОКАЗАТЕЛИ", headerFont));
        headerCell.setBackgroundColor(Color.DARK_GRAY);
        headerCell.setColspan(2);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(headerCell);

        addTableRow(table, "Пользователи", analytics.getTotalUsers().toString());
        addTableRow(table, "Заказы", analytics.getTotalOrders().toString());
        addTableRow(table, "Выручка", String.format("%.2f ₽", analytics.getTotalRevenue()));
        addTableRow(table, "Средний чек", String.format("%.2f ₽", analytics.getAverageOrderValue()));
        addTableRow(table, "Конверсия", String.format("%.1f%%", analytics.getConversionRate()));

        document.add(table);
    }

    private void addCharts(Document document, AnalyticsDTO analytics) throws Exception {
        // График 1: Динамика заказов
        Paragraph chart1Title = new Paragraph("ДИНАМИКА ЗАКАЗОВ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY));
        chart1Title.setSpacingBefore(20);
        document.add(chart1Title);

        DefaultCategoryDataset lineDataset = new DefaultCategoryDataset();
        analytics.getDailyOrders().forEach((date, count) ->
                lineDataset.addValue(count, "Заказы", date));

        JFreeChart lineChart = ChartFactory.createLineChart(
                "", "Дата", "Количество заказов",
                lineDataset, PlotOrientation.VERTICAL, true, true, false);

        addChartToDocument(document, lineChart, 500, 300);

        // График 2: Распределение по категориям
        Paragraph chart2Title = new Paragraph("ВЫРУЧКА ПО КАТЕГОРИЯМ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY));
        chart2Title.setSpacingBefore(20);
        document.add(chart2Title);

        DefaultPieDataset pieDataset = new DefaultPieDataset();
        analytics.getRevenueByCategory().forEach((category, revenue) ->
                pieDataset.setValue(category, revenue.doubleValue()));

        JFreeChart pieChart = ChartFactory.createPieChart(
                "", pieDataset, true, true, false);

        addChartToDocument(document, pieChart, 500, 300);
    }

    private void addChartToDocument(Document document, JFreeChart chart, int width, int height) throws Exception {
        try (ByteArrayOutputStream chartBaos = new ByteArrayOutputStream()) {
            ChartUtils.writeChartAsPNG(chartBaos, chart, width, height);
            Image chartImage = Image.getInstance(chartBaos.toByteArray());
            chartImage.scaleToFit(width, height);
            chartImage.setAlignment(Image.ALIGN_CENTER);
            document.add(chartImage);
            document.add(Chunk.NEWLINE);
        }
    }

    private void addTopProductsTable(Document document, AnalyticsDTO analytics) throws DocumentException {
        if (analytics.getTopSellingProducts() == null || analytics.getTopSellingProducts().isEmpty()) {
            return;
        }

        Paragraph tableTitle = new Paragraph("ТОП ПРОДАВАЕМЫХ ТОВАРОВ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY));
        tableTitle.setSpacingBefore(20);
        document.add(tableTitle);

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        PdfPCell headerCell = new PdfPCell(new Phrase("Товар", headerFont));
        headerCell.setBackgroundColor(Color.DARK_GRAY);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(headerCell);

        headerCell = new PdfPCell(new Phrase("Продажи", headerFont));
        headerCell.setBackgroundColor(Color.DARK_GRAY);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(headerCell);

        analytics.getTopSellingProducts().forEach((product, count) -> {
            addTableRow(table, product, count.toString());
        });

        document.add(table);
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        table.addCell(new PdfPCell(new Phrase(label)));
        PdfPCell valueCell = new PdfPCell(new Phrase(value));
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(valueCell);
    }
}