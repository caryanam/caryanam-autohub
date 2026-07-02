package com.autohub.configuration;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CustomUserDetails implements UserDetails {

    private Long id;
    private String name;
    private String mobile;
    private String email;
    private String password;
    private String role;
    private Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(
            Long id,
            String name,
            String email,
            String mobile,
            String password,
            String role,
            Collection<? extends GrantedAuthority> authorities) {

        this.id = id;
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.password = password;
        this.role = role;
        this.authorities = authorities;
    }


    public String getRole() {
        return role;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getUsername() {
        return mobile;
    }

    @Override
    public String getPassword() {
        return password;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}