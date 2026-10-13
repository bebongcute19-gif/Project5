package com.omnichannel.identity.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.omnichannel.identity.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String username;
    private final String email;
    @JsonIgnore
    private final String password;
    private final String fullName;
    private final String status;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> {
                String roleName = role.getName();
                if (roleName.startsWith("ROLE_")) {
                    authorities.add(new SimpleGrantedAuthority(roleName));
                    authorities.add(new SimpleGrantedAuthority(roleName.substring(5)));
                } else {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
                    authorities.add(new SimpleGrantedAuthority(roleName));
                }
            });
        }

        boolean isEnabled = Boolean.TRUE.equals(user.getEnabled()) && "ACTIVE".equalsIgnoreCase(user.getStatus());

        return UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername() != null ? user.getUsername() : user.getEmail())
                .email(user.getEmail())
                .password(user.getPasswordHash())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .enabled(isEnabled)
                .authorities(authorities)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
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
        return !"LOCKED".equalsIgnoreCase(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
