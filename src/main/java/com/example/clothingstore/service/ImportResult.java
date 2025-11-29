package com.example.clothingstore.service;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ImportResult {
    private boolean success;
    private String message;
    private Map<String, SheetResult> sheetResults = new HashMap<>();
    private int totalProcessed = 0;
    private int totalSuccess = 0;
    private int totalErrors = 0;

    public void addSheetResult(String sheetName, SheetResult sheetResult) {
        sheetResults.put(sheetName, sheetResult);
        totalProcessed += sheetResult.getProcessedCount();
        totalSuccess += sheetResult.getSuccessCount();
        totalErrors += sheetResult.getErrorCount();
    }

    public String getSummary() {
        return String.format("Обработано: %d, Успешно: %d, Ошибок: %d",
                totalProcessed, totalSuccess, totalErrors);
    }
}