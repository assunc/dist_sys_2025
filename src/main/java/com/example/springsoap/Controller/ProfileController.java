package com.example.springsoap.Controller;

import com.example.springsoap.Auth0ManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Controller
public class ProfileController {

    @Autowired
    private Auth0ManagementService auth0Service;

//    @PostMapping("/profile/update")
//    public String updateProfile(@AuthenticationPrincipal OidcUser user,
//                                @RequestParam String email,
//                                @RequestParam String nickname) {
//        auth0Service.updateUser(user.getSubject(), nickname);
//        return "redirect:/profile?updated=true";
//    }

    @PostMapping("/profile/reset-password")
    public String resetPassword(@RequestParam String email) {
        auth0Service.triggerPasswordReset(email);
        return "redirect:/profile?resetSent=true";
    }

}
