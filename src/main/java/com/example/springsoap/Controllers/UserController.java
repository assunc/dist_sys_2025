package com.example.springsoap.Controllers;


import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserController {

    @GetMapping("/user/view")
    @PreAuthorize("hasAuthority('ROLE_READ_ORDERS')")
    public String viewOrders() {
        return "user-dashboard";
    }

    @GetMapping("/user/cancel")
    @PreAuthorize("hasAuthority('ROLE_CANCEL_ORDERS')")
    public String cancelOrder() {
        return "user-cancel";
    }
}
