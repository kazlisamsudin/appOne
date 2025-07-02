package com.app.ai.controller;

import com.app.ai.model.User;
import com.app.ai.model.Role;
import com.app.ai.model.ProfilePhoto;
import com.app.ai.service.UserService;
import com.app.ai.repository.RoleRepository;
import com.app.ai.repository.ProfilePhotoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Controller
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private ProfilePhotoRepository profilePhotoRepository;

    @GetMapping("/profile")
    public String viewProfile(Authentication authentication, Model model) {
        String username = authentication.getName();
        Optional<User> userOptional = userService.findUserByUsername(username);
        if (userOptional.isPresent()) {
            model.addAttribute("user", userOptional.get());
            return "user/profile";
        }
        return "redirect:/";
    }

    @GetMapping("/profile/edit")
    public String editProfile(Authentication authentication, Model model) {
        String username = authentication.getName();
        Optional<User> userOptional = userService.findUserByUsername(username);
        if (userOptional.isPresent()) {
            model.addAttribute("user", userOptional.get());
            return "user/edit-profile";
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(Authentication authentication,
                                @ModelAttribute User user,
                                @RequestParam(required = false) String currentPassword,
                                @RequestParam(required = false) String newPassword,
                                RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        Optional<User> existingUserOptional = userService.findUserByUsername(username);

        if (existingUserOptional.isPresent()) {
            User existingUser = existingUserOptional.get();

            // Update basic information
            existingUser.setEmail(user.getEmail());

            // Handle password change if provided
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                if (currentPassword == null || currentPassword.trim().isEmpty()) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Current password is required to change password");
                    return "redirect:/profile/edit";
                }

                // Verify current password
                if (!userService.verifyCurrentPassword(existingUser, currentPassword)) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Current password is incorrect");
                    return "redirect:/profile/edit";
                }

                // Encode and set new password
                userService.updateUserPassword(existingUser, newPassword);
            }

            try {
                userService.updateUser(existingUser);
                redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Error updating profile: " + e.getMessage());
                return "redirect:/profile/edit";
            }
        }

        return "redirect:/profile";
    }

    @GetMapping("/users/list")
    @PreAuthorize("hasRole('ADMIN')")
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir,
            Model model) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<User> userPage = userService.findPaginatedUsers(pageable);
        model.addAttribute("userPage", userPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        return "user/list";
    }

    @GetMapping("/users/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String showAddForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("allRoles", roleRepository.findAll());
        return "user/form";
    }

    @GetMapping("/users/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.findUserById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found");
            return "redirect:/users/list";
        }
        model.addAttribute("user", user);
        model.addAttribute("allRoles", roleRepository.findAll());
        return "user/form";
    }

    @PostMapping("/users/save")
    @PreAuthorize("hasRole('ADMIN')")
    public String saveUser(@ModelAttribute User user, @RequestParam(required = false) List<Long> roles, RedirectAttributes redirectAttributes) {
        Set<Role> userRoles = Set.copyOf(roleRepository.findAllById(roles));
        user.setRoles(userRoles);
        if (user.getId() == null) {
            userService.registerUser(user);
            redirectAttributes.addFlashAttribute("successMessage", "User added successfully!");
        } else {
            // Editing: if password is blank, keep the old password
            User existing = userService.findUserById(user.getId()).orElse(null);
            if (existing != null && (user.getPassword() == null || user.getPassword().isBlank())) {
                user.setPassword(existing.getPassword());
            }
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully!");
        }
        return "redirect:/users/list";
    }

    @GetMapping("/users/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully!");
        return "redirect:/users/list";
    }

    @GetMapping("/users/view/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String viewUser(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return userService.findUserById(id)
            .map(user -> {
                model.addAttribute("user", user);
                return "user/view";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("errorMessage", "User not found");
                return "redirect:/users/list";
            });
    }

    @PostMapping("/profile/photo/upload")
    public String uploadProfilePhoto(Authentication authentication, @RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) throws IOException {
        String username = authentication.getName();
        Optional<User> userOpt = userService.findUserByUsername(username);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found");
            return "redirect:/profile/edit";
        }
        User user = userOpt.get();
        ProfilePhoto photo = profilePhotoRepository.findByUser(user).orElse(new ProfilePhoto());
        photo.setUser(user);
        photo.setFileName(file.getOriginalFilename());
        photo.setFileType(file.getContentType());
        photo.setData(file.getBytes());
        profilePhotoRepository.save(photo);
        user.setProfilePhoto(photo);
        userService.updateUser(user);
        redirectAttributes.addFlashAttribute("successMessage", "Profile photo uploaded successfully.");
        return "redirect:/profile/edit";
    }

    @GetMapping("/profile/photo/{id}")
    public ResponseEntity<byte[]> getProfilePhoto(@PathVariable Long id) {
        Optional<User> userOpt = userService.findUserById(id);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();
        User user = userOpt.get();
        ProfilePhoto photo = user.getProfilePhoto();
        if (photo == null || photo.getData() == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(photo.getFileType()))
            .body(photo.getData());
    }
}
