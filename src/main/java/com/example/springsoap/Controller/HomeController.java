//package com.example.springsoap.Controller;


//import org.springframework.security.oauth2.core.oidc.user.OidcUser;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//
//@Controller
//public class HomeController {
//
//    @GetMapping("/")
//    public String home(Model model, @AuthenticationPrincipal OidcUser principal) {
//        if (principal != null) {
//            model.addAttribute("profile", principal.getClaims());
//        }
//        return "index";
//    }
//}
//import org.springframework.security.oauth2.core.oidc.user.OidcUser;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//
//@Controller
//public class HomeController {
//
//    @GetMapping("/")
//    public String home(@AuthenticationPrincipal OidcUser user, Model model) {
//        if (user != null) {
//            String idToken = user.getIdToken().getTokenValue();
//            System.out.println("\n\nID TOKEN:\n" + idToken + "\n");
//
//            model.addAttribute("name", user.getFullName());
//            model.addAttribute("email", user.getEmail());
//            model.addAttribute("picture", user.getPicture());
//            model.addAttribute("roles", user.getClaims().get("https://distributed.com/roles"));
//        }
//        return "index";
//    }
//}