package com.clientra.security;

import com.clientra.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Convenience accessor: "who is calling?" without passing the user through every method. */
@Component
public class CurrentUser {
    public AuthUser get() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof AuthUser)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please sign in.");
        }
        return (AuthUser) a.getPrincipal();
    }

    /** Null when nobody is signed in (e.g. the scheduler or a failed login). */
    public AuthUser getOrNull() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a != null && a.getPrincipal() instanceof AuthUser) ? (AuthUser) a.getPrincipal() : null;
    }

    public Integer id() { return get().userId(); }
    public boolean isRep() { return get().isRep(); }
}
