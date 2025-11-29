package com.example.clothingstore.service;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class SheetResult {
    private String sheetName;
    private int processedCount = 0;
    private int successCount = 0;
    private int errorCount = 0;
    private List<String> errors = new ArrayList<>();

    public SheetResult(String sheetName) {
        this.sheetName = sheetName;
    }

    public void incrementSuccessCount() {
        successCount++;
        processedCount++;
    }

    public void addError(String error) {
        errors.add(error);
        errorCount++;
        processedCount++;
    }

    public String getSummary() {
        return String.format("%s: Обработано %d, Успешно %d, Ошибок %d",
                sheetName, processedCount, successCount, errorCount);
    }
}