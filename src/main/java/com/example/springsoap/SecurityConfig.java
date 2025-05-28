package com.example.springsoap; ////package com.example.springsoap;
////
////import org.springframework.context.annotation.Bean;
////import org.springframework.context.annotation.Configuration;
////import org.springframework.security.config.annotation.web.builders.HttpSecurity;
////import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
////import org.springframework.security.web.SecurityFilterChain;
////import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
////import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
////import org.springframework.security.oauth2.core.oidc.user.OidcUser;
////import org.springframework.security.core.annotation.AuthenticationPrincipal;
////import org.springframework.web.util.UriComponentsBuilder;
////import jakarta.servlet.http.HttpServletRequest;
////import jakarta.servlet.http.HttpServletResponse;
////@Configuration
////@EnableWebSecurity
////public class SecurityConfig {
////
////    @Bean
////    public SecurityFilterChain securityFilterChain(HttpSecurity http,
////                                                   ClientRegistrationRepository clientRegistrationRepository) throws Exception {
////        http
////                .authorizeHttpRequests(auth -> auth
////                        .requestMatchers("/", "/index", "/error").permitAll()
////                        .anyRequest().authenticated()
////                )
////                .oauth2Login(oauth2 -> oauth2
////                        .defaultSuccessUrl("/", true)
////                )
////                .logout(logout -> logout
////                        .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
////                );
////
////        return http.build();
////    }
////
////    private LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
////        return (HttpServletRequest request, HttpServletResponse response,
////                org.springframework.security.core.Authentication authentication) -> {
////
////            String issuerUri = "https://dev-distributed.eu.auth0.com/v2/logout";
////            String clientId = "qNdLFytoI8f16Xe8JtFKhRohbhrqWlFn";
////
////            String logoutUrl = UriComponentsBuilder
////                    .fromHttpUrl(issuerUri)
////                    .queryParam("client_id", clientId)
////                    .queryParam("returnTo", "http://localhost:8080/")
////                    .encode()
////                    .toUriString();
////
////            response.sendRedirect(logoutUrl);
////        };
////    }
////}
//
//package com.example.springsoap;
//
//
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

//    @Value("${auth0.logout-url}")
//    private String logoutUrl;
//
//    @Value("${auth0.client-id}")
//    private String clientId;
//
//    @Value("${auth0.post-logout-redirect}")
//    private String returnTo;

    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/", "/login/oauth2", "/flights" ,"/hotels" ,"/combo","/logged-out").permitAll()
//                        .requestMatchers("/manager/**").hasRole("manager")
//                        .anyRequest().authenticated()
//                )
//                .oauth2Login(oauth2 -> oauth2
//                        .userInfoEndpoint(userInfo -> userInfo
//                                .oidcUserService(oidcUserService()))
//                )
//                .logout(logout -> {
//                    System.out.println(" Logout endpoint hit");
//                    logout
//                            .logoutUrl("/logout")
//                            .logoutSuccessHandler(oidcLogoutSuccessHandler())
//                            .invalidateHttpSession(true)
//                            .clearAuthentication(true)
//                            .deleteCookies("JSESSIONID");
//                });
//        return http.build();
//    }
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/", "/login/oauth2", "/flights", "/hotels", "/combo", "/logged-out", "/css/**", "/js/**").permitAll()
//                        .requestMatchers("/manager/**").hasRole("manager")
//                        .anyRequest().permitAll() // ⬅ This ensures no forced login
//                )
//                .oauth2Login(oauth2 -> oauth2
//                        .userInfoEndpoint(userInfo -> userInfo
//                                .oidcUserService(oidcUserService()))
//                )
//                .logout(logout -> {
//                    System.out.println(" Logout endpoint hit");
//                    logout
//                            .logoutUrl("/logout")
//                            .logoutSuccessHandler(oidcLogoutSuccessHandler())
//                            .invalidateHttpSession(true)
//                            .clearAuthentication(true)
//                            .deleteCookies("JSESSIONID");
//                });
//
//        return http.build();
//    }
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .securityContext(context -> context
                        .requireExplicitSave(false)  // Optional
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 🔥 This disables automatic session restoration
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login/oauth2", "/flights", "/hotels", "/combo", "/logged-out", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/manager/**").hasRole("manager")
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(oidcUserService()))
                )
                .logout(logout -> {
                    System.out.println(" Logout endpoint hit");
                    logout
                            .logoutUrl("/logout")
                            .logoutSuccessHandler(oidcLogoutSuccessHandler())
                            .invalidateHttpSession(true)
                            .clearAuthentication(true)
                            .deleteCookies("JSESSIONID");
                });

        return http.build();
    }

    private OidcUserService oidcUserService() {
        OidcUserService delegate = new OidcUserService();

        return new OidcUserService() {
            @Override
            public OidcUser loadUser(OidcUserRequest userRequest) {
                OidcUser oidcUser = delegate.loadUser(userRequest);

                Map<String, Object> claims = oidcUser.getClaims();
                List<String> roles = (List<String>) claims.getOrDefault("https://distributed.com/roles", List.of());

                Set<GrantedAuthority> authorities = new HashSet<>();
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }

                // Add default authorities (like 'SCOPE_openid' etc.)
                authorities.addAll(oidcUser.getAuthorities());

                return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
            }
        };
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        return (request, response, authentication) -> {
            System.out.println("Logout triggered");

            // ✅ Manually clear the SecurityContext
            SecurityContextHolder.clearContext();

            // ✅ Also invalidate the HttpSession (in case Spring didn't)
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            String logoutRedirect = UriComponentsBuilder
                    .fromHttpUrl("https://dev-distributed.eu.auth0.com/v2/logout")
                    .queryParam("client_id", "qNdLFytoI8f16Xe8JtFKhRohbhrqWlFn")
                    .queryParam("returnTo", "http://localhost:8080/logged-out?loggedOut=true")
                    .build()
                    .toUriString();

            System.out.println("➡ Redirecting to: " + logoutRedirect);

            response.sendRedirect(logoutRedirect);
        };
    }



}

//    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
//        return (HttpServletRequest request, HttpServletResponse response,
//                org.springframework.security.core.Authentication authentication) -> {
//            String url = UriComponentsBuilder
//                    .fromHttpUrl(logoutUrl)
//                    .queryParam("client_id", clientId)
//                    .queryParam("returnTo", returnTo)
//                    .toUriString();
//
//            response.sendRedirect(url);
//        };
//    }
//}

