package com.eyegil.gara.catalog.dto;

import com.eyegil.gara.catalog.entity.Volume;

/**
 * A file just committed: the volume that holds it and its git blob sha.
 */
public record StoredFile(Volume volume, String gitSha) {
}
