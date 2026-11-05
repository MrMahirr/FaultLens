package com.faultlens.api.security;

/**
 * Role names and the {@code @PreAuthorize} expressions used on controllers.
 *
 * <ul>
 *   <li>{@code VIEWER}: reads everything; manages only its own profile, password and
 *       notification settings.</li>
 *   <li>{@code EDITOR}: also edits sources and alarm rules, acknowledges/resolves alarms, records
 *       deployments, triggers analyses.</li>
 *   <li>{@code ADMIN}: also deletes (sources, alarms, logs, analysis history), changes system
 *       settings and manages users.</li>
 * </ul>
 */
public final class Roles {
    public static final String ADMIN = "ADMIN";
    public static final String EDITOR = "EDITOR";
    public static final String VIEWER = "VIEWER";

    public static final String CAN_EDIT = "hasAnyRole('ADMIN', 'EDITOR')";
    public static final String ADMIN_ONLY = "hasRole('ADMIN')";

    private Roles() {
    }

    public static boolean isValid(String role) {
        return ADMIN.equals(role) || EDITOR.equals(role) || VIEWER.equals(role);
    }
}
