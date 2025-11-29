package com.example.clothingstore.controller;

import com.example.clothingstore.model.Brand;
import com.example.clothingstore.service.BrandService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/brands")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    public String getAllBrands(Model model,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(required = false) String search,
                               @RequestParam(required = false) String status) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());

        Page<Brand> brandPage;
        if (search != null && !search.isBlank()) {
            brandPage = brandService.searchBrands(search, pageable);
        } else if (status != null && !status.isEmpty()) {
            if ("active".equals(status)) {
                brandPage = brandService.getActiveBrands(pageable);
            } else if ("archived".equals(status)) {
                brandPage = brandService.getArchivedBrands(pageable);
            } else {
                brandPage = brandService.getAllBrands(pageable);
            }
        } else {
            brandPage = brandService.getAllBrands(pageable);
        }

        // Добавлено передача brandPage вместо brands
        model.addAttribute("brandPage", brandPage);
        model.addAttribute("brand", new Brand());
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalBrandsCount", brandService.getTotalBrandsCount());
        model.addAttribute("activeBrandsCount", brandService.getActiveBrandsCount());
        model.addAttribute("archivedBrandsCount", brandService.getArchivedBrandsCount());

        return "brands/list";
    }
    @GetMapping("/archive")
    public String getArchivedBrands(Model model,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Brand> archivedBrands = brandService.getArchivedBrands(pageable);

        model.addAttribute("brands", archivedBrands.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", archivedBrands.getTotalPages());
        model.addAttribute("totalBrandsCount", brandService.getTotalBrandsCount());
        model.addAttribute("activeBrandsCount", brandService.getActiveBrandsCount());
        model.addAttribute("archivedBrandsCount", brandService.getArchivedBrandsCount());

        return "brands/archive";
    }

    @GetMapping("/restore/{id}")
    public String restoreBrand(@PathVariable Long id) {
        brandService.restoreBrand(id);
        return "redirect:/brands";
    }

    @GetMapping("/export/excel")
    public void exportToExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=brands_" + LocalDate.now() + ".xlsx";
        response.setHeader(headerKey, headerValue);

        List<Brand> brands = brandService.getAllBrands();
        brandService.exportToExcel(brands, response);
    }

    @GetMapping("/export/pdf")
    public void exportToPdf(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=brands_" + LocalDate.now() + ".pdf";
        response.setHeader(headerKey, headerValue);

        List<Brand> brands = brandService.getAllBrands();
        brandService.exportToPdf(brands, response);
    }

    @PostMapping("/create")
    public String createBrand(@Valid @ModelAttribute("brand") Brand brand,
                              BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("brands", brandService.getAllBrands());
            return "brands/list";
        }
        brandService.createBrand(brand);
        return "redirect:/brands";
    }

    @GetMapping("/edit/{id}")
    public String editBrand(@PathVariable Long id, Model model) {
        Brand brand = brandService.getBrandById(id);
        model.addAttribute("brand", brand);
        return "brands/form";
    }

    @PostMapping("/update/{id}")
    public String updateBrand(@PathVariable Long id,
                              @Valid @ModelAttribute("brand") Brand brand,
                              BindingResult result) {
        if (result.hasErrors()) {
            return "brands/form";
        }
        brandService.updateBrand(id, brand);
        return "redirect:/brands";
    }

    @GetMapping("/soft-delete/{id}")
    public String softDeleteBrand(@PathVariable Long id) {
        brandService.softDeleteBrand(id);
        return "redirect:/brands";
    }

    @GetMapping("/hard-delete/{id}")
    public String hardDeleteBrand(@PathVariable Long id) {
        brandService.hardDeleteBrand(id);
        return "redirect:/brands";
    }
}