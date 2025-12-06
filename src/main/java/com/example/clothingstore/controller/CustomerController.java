// CustomerController.java
package com.example.clothingstore.controller;

import com.example.clothingstore.model.Role;
import com.example.clothingstore.model.User;
import com.example.clothingstore.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/customers")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class CustomerController {

    private final UserService userService;

    public CustomerController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listCustomers(Model model,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(defaultValue = "id") String sortBy,
                                @RequestParam(defaultValue = "asc") String sortDir,
                                @RequestParam(required = false) String search,
                                @RequestParam(required = false) String status) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> customerPage;
        if (search != null && !search.isBlank()) {
            customerPage = userService.searchCustomers(search, pageable);
        } else if ("active".equals(status)) {
            customerPage = userService.getRecentlyActiveCustomers(pageable);
        } else if ("deleted".equals(status)) {
            customerPage = userService.getDeletedCustomers(pageable);
        } else {
            customerPage = userService.getAllCustomers(pageable);
        }

        List<User> inactiveCustomers = customerPage.getContent().stream()
                .filter(customer -> customer.getLastActivity() != null &&
                        customer.getLastActivity().isBefore(LocalDateTime.now().minusDays(30)))
                .collect(Collectors.toList());

        model.addAttribute("customerPage", customerPage);
        model.addAttribute("inactiveCustomers", inactiveCustomers);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        return "customers/list";
    }
    @PostMapping("/change-role/{id}")
    public String changeUserRole(@PathVariable Long id,
                                 @RequestParam String newRole,
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findById(id);
            Role role = Role.valueOf(newRole);
            user.getRoles().clear();
            user.getRoles().add(role);

            userService.saveUser(user);
            redirectAttributes.addFlashAttribute("success", "Роль пользователя изменена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка изменения роли: " + e.getMessage());
        }
        return "redirect:/customers";
    }
    @GetMapping("/archive")
    public String showArchive(Model model,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> archivedCustomers = userService.getArchivedCustomers(pageable);

        model.addAttribute("archivedCustomers", archivedCustomers);
        model.addAttribute("currentPage", page);
        return "customers/archive";
    }

    @GetMapping("/restore/{id}")
    public String restoreCustomer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.restoreCustomer(id);
            redirectAttributes.addFlashAttribute("success", "Клиент восстановлен из архива");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка восстановления клиента: " + e.getMessage());
        }
        return "redirect:/customers/archive";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("customer", new User());
        return "customers/form";
    }

    @PostMapping("/create")
    public String createCustomer(@RequestParam String firstName,
                                 @RequestParam String lastName,
                                 @RequestParam String username,
                                 @RequestParam String email,
                                 @RequestParam String password,
                                 @RequestParam String role,
                                 @RequestParam(required = false) String phone,
                                 @RequestParam(required = false) String address,
                                 @RequestParam(required = false) String city,
                                 @RequestParam(required = false) String postalCode,
                                 @RequestParam(defaultValue = "true") boolean enabled,
                                 RedirectAttributes redirectAttributes) {
        try {
            User customer = new User();
            customer.setFirstName(firstName);
            customer.setLastName(lastName);
            customer.setUsername(username);
            customer.setEmail(email);
            customer.setPassword(password);
            customer.setPhone(phone);
            customer.setAddress(address);
            customer.setCity(city);
            customer.setPostalCode(postalCode);
            customer.setEnabled(enabled);

            Role userRole = Role.valueOf(role);
            customer.getRoles().add(userRole);

            userService.registerCustomer(customer);

            redirectAttributes.addFlashAttribute("success", "Клиент успешно создан");
            return "redirect:/customers";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка создания клиента: " + e.getMessage());
            return "redirect:/customers/create";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        User customer = userService.findById(id);
        model.addAttribute("customer", customer);
        return "customers/form";
    }

    @PostMapping("/update/{id}")
    public String updateCustomer(@PathVariable Long id,
                                 @RequestParam String firstName,
                                 @RequestParam String lastName,
                                 @RequestParam String email,
                                 @RequestParam(required = false) String phone) {
        User customer = userService.findById(id);
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
        customer.setPhone(phone);

        userService.saveUser(customer);
        return "redirect:/customers?success";
    }

    @GetMapping("/soft-delete/{id}")
    public String softDeleteCustomer(@PathVariable Long id) {
        userService.softDeleteUser(id);
        return "redirect:/customers";
    }

    @GetMapping("/hard-delete/{id}")
    public String hardDeleteCustomer(@PathVariable Long id) {
        userService.hardDeleteUser(id);
        return "redirect:/customers";
    }

    @GetMapping("/enable/{id}")
    public String enableCustomer(@PathVariable Long id) {
        userService.enableUser(id);
        return "redirect:/customers";
    }

    @GetMapping("/disable/{id}")
    public String disableCustomer(@PathVariable Long id) {
        userService.disableUser(id);
        return "redirect:/customers";
    }
}