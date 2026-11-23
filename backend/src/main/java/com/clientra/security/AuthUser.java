package com.clientra.security;

/** The authenticated caller; stored as the Spring Security principal for the current request. */
public record AuthUser(Integer userId, String email, String fullName, String role) {
    public boolean isRep() { return Roles.REP.equals(role); }
    public boolean isAdmin() { return Roles.ADMIN.equals(role); }
}
