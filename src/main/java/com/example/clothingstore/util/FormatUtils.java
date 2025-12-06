package com.example.clothingstore.util;

import com.example.clothingstore.model.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FormatUtils {

    public static String formatDate(LocalDateTime dateTime, String dateFormat) {
        if (dateTime == null) return "";

        DateTimeFormatter formatter;
        switch (dateFormat != null ? dateFormat : "dd.MM.yyyy") {
            case "dd/MM/yyyy":
                formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                break;
            case "yyyy-MM-dd":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                break;
            case "MM/dd/yyyy":
                formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
                break;
            default:
                formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                break;
        }

        return dateTime.format(formatter);
    }

    public static String formatNumber(BigDecimal number, String numberFormat) {
        if (number == null) return "0";

        String formatted;
        switch (numberFormat != null ? numberFormat : "COMMA") {
            case "SPACE":
                formatted = String.format("%,.2f", number).replace(",", " ");
                break;
            case "DOT":
                formatted = String.format("%,.2f", number).replace(",", "X").replace(".", ",").replace("X", ".");
                break;
            default:
                formatted = String.format("%,.2f", number);
                break;
        }

        return formatted;
    }

    public static String formatDateWithUser(LocalDateTime dateTime, User user) {
        return formatDate(dateTime, user != null ? user.getDateFormat() : null);
    }

    public static String formatNumberWithUser(BigDecimal number, User user) {
        return formatNumber(number, user != null ? user.getNumberFormat() : null);
    }
}