package com.example.springsoap.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class BrokerController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal OidcUser user,Model model) {
        if (user != null) {
            // You can log the user if needed
            System.out.println("Logged in as: " + user.getFullName());
        }

        model.addAttribute("title", "Home");
        model.addAttribute("contentTemplate", "index");
        return "layout";
    }

    @GetMapping("/flights")
    public String flights(Model model) {
        model.addAttribute("title", "Flights");
        model.addAttribute("flights", List.of("BRU → NYC", "BRU → TOKYO"));
        model.addAttribute("contentTemplate", "flights");
        return "layout";
    }

    @GetMapping("/hotels")
    public String hotels(Model model) {
        model.addAttribute("title", "Hotels");
        model.addAttribute("contentTemplate", "hotels");
        return "layout";
    }

    @GetMapping("/combo")
    public String combo(Model model) {
        model.addAttribute("title", "Hotels + Flights");
        model.addAttribute("contentTemplate", "combo");
        return "layout";
    }
   @GetMapping("/logged-out")
public String loggedOut(Model model) {
    model.addAttribute("title", "Logged Out");
    model.addAttribute("contentTemplate", "loggedout");
    return "layout";
}

}
