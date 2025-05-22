package com.example.springsoap;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<String> permissions = jwt.getClaimAsStringList("permissions");
        return permissions == null ? List.of() :
                permissions.stream()
                        .map(p -> "ROLE_" + p.toUpperCase().replace(":", "_"))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
    }
}

