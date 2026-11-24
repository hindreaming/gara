package com.eyegil.gara.admin.dto;

/**
 * @param repository       the bucket's first repository
 * @param directory        folder inside each repository for the objects; empty for the root
 * @param createRepository create {@code repository} on GitHub instead of using an existing one
 * @param publicRepos      repositories Gara creates for the bucket are public instead of private
 */
public record CreateBucketRequest(String name, String repository, String directory, boolean createRepository,
                                  boolean publicRepos) {
}
