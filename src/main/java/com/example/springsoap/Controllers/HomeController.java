package com.example.springsoap.Controllers;


import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // Public welcome page
    @GetMapping("/home")
    public String home() {
        return "home";
    }

    // Post-login redirect handler
    @GetMapping("/")
    public String handleLogin(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            if (auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGE_ORDERS"))) {
                return "redirect:/manager/dashboard";
            } else {
                return "redirect:/user/dashboard";
            }
        }
        return "redirect:/home";
    }
}


