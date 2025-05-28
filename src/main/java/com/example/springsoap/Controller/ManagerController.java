//package com.example.springsoap.Controller;
//
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.oauth2.core.oidc.user.OidcUser;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//
//import java.util.List;
//
//@Controller
//public class ManagerController {
//
//    @GetMapping("/manager/orders")
//    public String orders(Model model, @AuthenticationPrincipal OidcUser principal) {
//        model.addAttribute("user", principal.getFullName());
//        model.addAttribute("orders", List.of(
//                "Order #1 - succeeded",
//                "Order #2 - ongoing",
//                "Order #3 - failed"
//        ));
//        return "orders";
//    }
//}