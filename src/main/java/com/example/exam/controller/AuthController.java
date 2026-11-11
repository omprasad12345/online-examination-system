package com.example.exam.controller;

import com.example.exam.model.User;
import com.example.exam.service.FileStorageService;
import com.example.exam.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.GrantedAuthority;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/")
    public String home() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !(authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser"))) {


            boolean isAdmin = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_ADMIN"::equals);

            if (isAdmin) {

                return "redirect:/admin/dashboard";
            } else {

                return "redirect:/student/dashboard";
            }
        }

        return "index";
    }


    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerStudent(
            @ModelAttribute("user") User user,
            BindingResult bindingResult,
            @RequestParam("profilePicFile") MultipartFile profilePicFile, // Gets the file
            Model model) {


        if (userService.findByUsername(user.getUsername()) != null) {
            bindingResult.rejectValue("username", "error.user", "An account already exists with that email.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "register";
        }

        if (!profilePicFile.isEmpty()) {
            try {

                String filePath = fileStorageService.saveFile(profilePicFile);
                user.setProfilePicUrl(filePath);
            } catch (Exception e) {

                model.addAttribute("user", user);
                model.addAttribute("fileError", "Could not save profile picture. Please try again.");
                return "register";
            }
        }

        userService.saveStudent(user);
        return "redirect:/register?success";
    }
}
