package com.eyegil.gara.coordination.dto;

import java.time.Instant;

/**
 * A lease currently held by some instance.
 */
public record ActiveLease(String name, String ownerId, Instant expiresAt) {
}
