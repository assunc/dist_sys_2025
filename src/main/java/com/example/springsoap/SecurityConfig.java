package com.example.springsoap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .antMatchers("/", "/public/**").permitAll()
                        .antMatchers("/user/**").hasAnyAuthority(
                                "ROLE_READ_ORDERS", "ROLE_CANCEL_ORDERS")
                        .antMatchers("/manager/**").hasAnyAuthority(
                                "ROLE_MANAGE_ORDERS", "ROLE_READ_ORDERS", "ROLE_CANCEL_ORDERS")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendRedirect("/home"))
                )

                .oauth2Login()
                .and()
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter()))
                );

        return http.build();
    }

    private JwtAuthenticationConverter jwtConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverter());
        return converter;
    }
}
