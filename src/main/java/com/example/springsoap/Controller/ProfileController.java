package com.example.springsoap.Controller;

import com.example.springsoap.Auth0ManagementService;
import com.example.springsoap.Entities.User;
import com.example.springsoap.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class ProfileController {

    private final UserRepository userRepository;

    @Autowired
    private Auth0ManagementService auth0Service;

    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/profile/reset-password")
    public String resetPassword(@RequestParam String email) {
        auth0Service.triggerPasswordReset(email);
        return "redirect:/profile?resetSent=true";
    }

    @GetMapping("/profile")
    public String profilePage(@AuthenticationPrincipal OidcUser user, Model model) {
        if (user == null) {
            return "redirect:/"; // Redirect to main page if not authenticated
        }
        model.addAttribute("isLoggedIn", true);

        model.addAttribute("nickname", user.getAttribute("nickname"));
        model.addAttribute("email", user.getEmail());
        model.addAttribute("picture", user.getPicture());
        User userDb = userRepository.findByEmail(user.getEmail()).orElseThrow(() -> new RuntimeException("User not found in DB"));
        String deliveryAddress = userDb.getDeliveryAddress();
        String paymentInfo = userDb.getPaymentInfo();
        if (!paymentInfo.isEmpty()) {
            model.addAttribute("hasPaymentInfo", true);
            String[] paymentInfoSplit = paymentInfo.split("_");
            model.addAttribute("cardNumber", paymentInfoSplit[0]);
            model.addAttribute("expirationMonth", paymentInfoSplit[1]);
            model.addAttribute("expirationYear", paymentInfoSplit[2]);
            model.addAttribute("cvc", paymentInfoSplit[3]);
        }
        if (!deliveryAddress.isEmpty()) {
            model.addAttribute("hasDeliveryAddress", true);
            String[] deliveryAddressSplit = deliveryAddress.split("_");
            model.addAttribute("deliveryStreet", deliveryAddressSplit[0]);
            model.addAttribute("deliveryCity", deliveryAddressSplit[1]);
            model.addAttribute("deliveryPostal", deliveryAddressSplit[2]);
            model.addAttribute("deliveryCountry", deliveryAddressSplit[3]);
        }

        model.addAttribute("title", "Profile");
        model.addAttribute("contentTemplate", "profile");
        return "layout";
    }

    @GetMapping("/profile/edit-details")
    public String editProfileDetailsPage(@AuthenticationPrincipal OidcUser user, Model model) {
        if (user == null) {
            return "redirect:/login"; // Redirect to login if not authenticated
        }
        model.addAttribute("isLoggedIn", true);
        model.addAttribute("title", "Edit Profile Details");
        model.addAttribute("contentTemplate", "profile-edit-details"); // This is the new HTML fragment

        // Fetch user data from DB to pre-populate the form
        Optional<User> userOptional = userRepository.findByEmail(user.getEmail());
        if (userOptional.isPresent()) {
            User userDb = userOptional.get();

            String paymentInfo = userDb.getPaymentInfo();
            String deliveryAddress = userDb.getDeliveryAddress();

            if (paymentInfo != null && !paymentInfo.isEmpty()) {
                String[] paymentInfoSplit = paymentInfo.split("_");
                model.addAttribute("cardNumber", paymentInfoSplit[0]);
                model.addAttribute("expirationMonth", paymentInfoSplit[1]);
                model.addAttribute("expirationYear", paymentInfoSplit[2]);
                model.addAttribute("cvc", paymentInfoSplit[3]);
            }

            if (deliveryAddress != null && !deliveryAddress.isEmpty()) {
                String[] deliveryAddressSplit = deliveryAddress.split("_");
                model.addAttribute("deliveryStreet", deliveryAddressSplit[0]);
                model.addAttribute("deliveryCity", deliveryAddressSplit[1]);
                model.addAttribute("deliveryPostal", deliveryAddressSplit[2]);
                model.addAttribute("deliveryCountry", deliveryAddressSplit[3]);
            }
        }

        // For expiration month/year dropdowns, ensure sequence generation as in payment.html
        model.addAttribute("currentYear", java.time.Year.now().getValue()); // Get current year for dropdowns

        return "layout";
    }

    @PostMapping("/profile/save-details")
    public String saveProfileDetails(
            @AuthenticationPrincipal OidcUser user,
            @RequestParam String cardNumber,
            @RequestParam String expirationMonth,
            @RequestParam String expirationYear,
            @RequestParam String cvc,
            @RequestParam String deliveryStreet,
            @RequestParam String deliveryCity,
            @RequestParam String deliveryPostal,
            @RequestParam String deliveryCountry) {

        if (user == null) {
            return "redirect:/login";
        }

        try {
            User userDb = userRepository.findByEmail(user.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found in DB"));

            String paymentInfo = cardNumber + "_" + expirationMonth + "_" + expirationYear + "_" + cvc;
            userDb.setPaymentInfo(paymentInfo);

            String deliveryAddress = deliveryStreet + "_" + deliveryCity + "_" + deliveryPostal + "_" + deliveryCountry;
            userDb.setDeliveryAddress(deliveryAddress);

            userRepository.save(userDb);

        } catch (Exception e) {
            System.err.println("Error saving profile details: " + e.getMessage());
        }

        return "redirect:/profile"; // Redirect back to profile page
    }

}
