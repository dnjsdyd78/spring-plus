package org.example.expert.domain.common.dto;

import lombok.Getter;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

@Getter
public class AuthUser {

    private final Long id;
    private final String email;
    private final String nickName;
    private final UserRole userRole;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthUser(Long id, String email, String nickName, UserRole role) {
        this.id = id;
        this.email = email;
        this.nickName = nickName;
        this.userRole = role;
        this.authorities = List.of(new SimpleGrantedAuthority(role.name()));
    }
}
