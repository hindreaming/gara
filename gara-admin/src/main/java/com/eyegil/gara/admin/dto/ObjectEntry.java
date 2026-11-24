package com.eyegil.gara.admin.dto;

/**
 * @param name         the key without the folder prefix, for display
 * @param lastModified ISO-8601 instant
 */
public record ObjectEntry(String key, String name, long size, String lastModified) {
}
