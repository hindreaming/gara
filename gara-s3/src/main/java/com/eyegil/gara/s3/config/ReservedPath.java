package com.eyegil.gara.s3.config;

/**
 * A first path segment (such as {@code /admin}) that another module serves instead of S3. Requests under it skip S3
 * signature checks, so the module that declares it must protect it. Declare one as a bean.
 */
public record ReservedPath(String path) {
}
