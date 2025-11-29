package com.example.clothingstore.controller;

import com.example.clothingstore.model.AuditLog;
import com.example.clothingstore.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public String getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String username,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLog> auditLogs;

        // Комбинированная фильтрация
        if ((entityType != null && !entityType.isEmpty()) ||
                (action != null && !action.isEmpty()) ||
                (username != null && !username.isEmpty())) {

            auditLogs = auditService.getLogsWithFilters(entityType, action, username, pageable);
        } else {
            auditLogs = auditService.getAllLogs(pageable);
        }

        model.addAttribute("logs", auditLogs);
        model.addAttribute("entityTypes", auditService.getAvailableEntityTypes());
        model.addAttribute("actions", auditService.getAvailableActions());
        model.addAttribute("currentPage", page);
        model.addAttribute("entityType", entityType);
        model.addAttribute("action", action);
        model.addAttribute("username", username);

        return "audit/list";
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public String getEntityHistory(@PathVariable String entityType,
                                   @PathVariable Long entityId,
                                   Model model) {

        List<AuditLog> entityHistory = auditService.getEntityHistory(entityType, entityId);
        model.addAttribute("history", entityHistory);
        model.addAttribute("entityType", entityType);
        model.addAttribute("entityId", entityId);

        return "audit/entity-history";
    }
}