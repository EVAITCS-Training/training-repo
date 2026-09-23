package com.evaitcs.securebank.security;

import com.evaitcs.securebank.domain.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * PRINCIPAL — "who is authenticated", adapted from OUR AppUser entity to
 * Spring Security's UserDetails interface.
 *
 * WHY AN ADAPTER? Spring Security speaks UserDetails; our database speaks
 * AppUser. Wrapping (not extending!) keeps the persistence entity free of
 * security-framework imports — the domain layer stays independent of HOW
 * we authenticate (JWT today, OAuth2 login tomorrow).
 *
 * The extra getCustomerId() is the bridge used all over the app: JWT claims
 * are built from it (JwtService), and controllers use it for ownership
 * checks ("show MY accounts").
 */
public class SecureBankUser implements UserDetails {

    private final AppUser user;

    public SecureBankUser(AppUser user) {
        this.user = user;
    }

    /** The customer profile this login maps to — null for staff (teller/admin). */
    public Long getCustomerId() {
        return user.getCustomer() == null ? null : user.getCustomer().getId();
    }

    public String getRoleName() {
        return user.getRole().name();
    }

    /**
     * Authorities = what this principal is ALLOWED to do. Spring Security
     * convention: roles are prefixed "ROLE_" so hasRole('TELLER') checks for
     * the authority ROLE_TELLER.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash(); // the BCrypt hash — never plaintext
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    // Account lifecycle flags — a real bank would wire these to columns
    // (locked after N failed logins, etc.). Defaults keep the demo simple.
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
