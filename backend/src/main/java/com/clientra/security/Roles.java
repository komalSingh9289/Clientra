package com.clientra.security;

/** Role names as stored in tbl_roles, plus helpers to turn them into Spring Security authorities. */
public final class Roles {
    private Roles() {}
    public static final String ADMIN = "Administrator";
    public static final String MANAGER = "Sales Manager";
    public static final String REP = "Sales Representative";

    /** "Sales Manager" -> "ROLE_SALES_MANAGER" (used by hasRole('SALES_MANAGER')). */
    public static String authority(String roleName) {
        return "ROLE_" + roleName.trim().toUpperCase().replace(' ', '_');
    }
}
