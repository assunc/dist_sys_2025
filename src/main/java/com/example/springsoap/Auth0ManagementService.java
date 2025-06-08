package com.example.springsoap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class Auth0ManagementService {

    @Value("${auth0.domain}")
    private String domain;

    @Value("${auth0.management.client-id}")
    private String clientId;

    @Value("${auth0.management.client-secret}")
    private String clientSecret;

    @Value("${auth0.management.audience}")
    private String audience;

    private String getToken() {
        WebClient client = WebClient.create();
        Map<String, String> request = Map.of(
                "grant_type", "client_credentials",
                "client_id", clientId,
                "client_secret", clientSecret,
                "audience", audience
        );

        return client.post()
                .uri("https://" + domain + "/oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .blockOptional()
                .map(r -> (String) r.get("access_token"))
                .orElseThrow(() -> new RuntimeException("Failed to get management API token"));
    }



    public void triggerPasswordReset(String email) {
        String token = getToken();
        WebClient.create().post()
                .uri("https://" + domain + "/dbconnections/change_password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "client_id", clientId,
                        "email", email,
                        "connection", "Username-Password-Authentication"
                ))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
