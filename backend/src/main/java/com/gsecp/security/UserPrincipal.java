package com.gsecp.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 从 JWT 中还原的登录主体；角色编码统一加 ROLE_ 前缀以配合 hasRole 表达式。
 */
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final List<String> roles;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(Long userId, String username, Collection<String> roles) {
        this.userId = userId;
        this.username = username;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
        this.authorities = this.roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    public Long getUserId() {
        return userId;
    }

    public List<String> getRoles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
