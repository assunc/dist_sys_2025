package com.example.springsoap.Controller;
import com.example.springsoap.UserService;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class BrokerController {
    private final UserService userService;

    public BrokerController(UserService userService) {
        this.userService = userService;
    }
    @GetMapping("/")
    public String home(@AuthenticationPrincipal OidcUser user, Model model) {
        boolean isLoggedIn = true;
        if (user == null)
        {
            isLoggedIn = false;
        }
        if (user != null)
        {
            userService.findOrCreateFromOidcUser(user);

            String idToken = user.getIdToken().getTokenValue();
            System.out.println("\n\nID TOKEN:\n" + idToken + "\n");
            model.addAttribute("name", user.getFullName());        }

        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "Home");
        model.addAttribute("contentTemplate", "index");
        return "layout";
    }





    @GetMapping("/flights")
    public String flights(@AuthenticationPrincipal OidcUser user, Model model) {
        boolean isLoggedIn = true;
        if (user == null)
        {
            isLoggedIn = false;
        }
        model.addAttribute("title", "Flights");
        model.addAttribute("flights", List.of("BRU → NYC", "BRU → TOKYO"));
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("contentTemplate", "flights");
        return "layout";
    }

    @GetMapping("/hotels")
    public String hotels(@AuthenticationPrincipal OidcUser user, Model model) {
        boolean isLoggedIn = true;
        if (user == null)
        {
            isLoggedIn = false;
        }
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "Hotels");
        model.addAttribute("contentTemplate", "hotels");
        return "layout";
    }

    @GetMapping("/combo")
    public String combo(@AuthenticationPrincipal OidcUser user, Model model) {
        boolean isLoggedIn = true;
        if (user == null)
        {
            isLoggedIn = false;
        }
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("title", "Hotels + Flights");
        model.addAttribute("contentTemplate", "combo");
        return "layout";
    }
   @GetMapping("/logged-out")
public String loggedOut(@AuthenticationPrincipal OidcUser user , Model model) {
       boolean isLoggedIn = true;
       if (user == null)
       {
           isLoggedIn = false;
       }
       model.addAttribute("isLoggedIn", isLoggedIn);
    model.addAttribute("title", "Logged Out");
    model.addAttribute("contentTemplate", "loggedout");
    return "layout";
}

}
