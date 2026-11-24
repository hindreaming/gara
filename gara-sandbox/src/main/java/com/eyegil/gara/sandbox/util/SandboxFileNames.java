package com.eyegil.gara.sandbox.util;

import java.util.Locale;

/**
 * Turns a visitor's file name into a safe object key segment: letters, digits, '.', '-' and '_' only.
 */
public final class SandboxFileNames {

    private static final int MAX_LENGTH = 100;

    private SandboxFileNames() {
    }

    public static String sanitize(String name) {
        String base = name == null ? "" : name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        String safe = base.replaceAll("[^A-Za-z0-9._-]+", "-").replaceAll("^[.-]+", "");
        if (safe.length() > MAX_LENGTH) {
            safe = safe.substring(safe.length() - MAX_LENGTH);
        }
        return safe.isEmpty() ? "file" : safe;
    }

    /**
     * @return the lowercase extension without the dot, or "" when there is none
     */
    public static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
