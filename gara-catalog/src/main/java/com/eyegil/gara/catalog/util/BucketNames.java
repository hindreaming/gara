package com.eyegil.gara.catalog.util;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Naming rules for buckets and where they live on GitHub.
 */
public final class BucketNames {

    /**
     * S3 bucket names (the subset that matters for path-style access): 3 to 63 characters, lowercase letters,
     * digits, dots and hyphens, starting and ending with a letter or digit.
     */
    private static final Pattern BUCKET = Pattern.compile("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]");
    private static final Pattern REPOSITORY = Pattern.compile("[A-Za-z0-9._-]{1,100}");
    private static final Pattern DIRECTORY_SEGMENT = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Set<String> FORBIDDEN_SEGMENTS = Set.of(".", "..", ".git");

    private BucketNames() {
    }

    public static boolean isValidBucket(String name) {
        return name != null && BUCKET.matcher(name).matches() && !name.contains("..");
    }

    public static boolean isValidRepository(String name) {
        return name != null && REPOSITORY.matcher(name).matches() && !name.equals(".") && !name.equals("..");
    }

    /**
     * A folder inside a repository: "" for the root, or segments separated by "/".
     */
    public static boolean isValidDirectory(String directory) {
        if (directory.isEmpty()) {
            return true;
        }
        for (String segment : directory.split("/", -1)) {
            if (!DIRECTORY_SEGMENT.matcher(segment).matches() || FORBIDDEN_SEGMENTS.contains(segment)) {
                return false;
            }
        }
        return true;
    }

    public static String trimSlashes(String value) {
        return value == null ? "" : value.trim().replaceAll("^/+|/+$", "");
    }
}
