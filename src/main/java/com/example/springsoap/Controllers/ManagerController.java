package com.example.springsoap.Controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ManagerController {

    @GetMapping("/manager/view")
    @PreAuthorize("hasAuthority('ROLE_READ_ORDERS')")
    public String viewAllOrders() {
        return "manager-dashboard";
    }

    @GetMapping("/manager/cancel")
    @PreAuthorize("hasAuthority('ROLE_CANCEL_ORDERS')")
    public String cancelAnyOrder() {
        return "manager-cancel";
    }

    @GetMapping("/manager/manage")
    @PreAuthorize("hasAuthority('ROLE_MANAGE_ORDERS')")
    public String managePlatform() {
        return "manager-manage";
    }
}
