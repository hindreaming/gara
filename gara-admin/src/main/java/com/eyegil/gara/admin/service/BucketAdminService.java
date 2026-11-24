package com.eyegil.gara.admin.service;

import com.eyegil.gara.admin.dto.CreateBucketRequest;
import com.eyegil.gara.catalog.entity.Bucket;
import com.eyegil.gara.catalog.exception.BucketRuleException;
import com.eyegil.gara.catalog.exception.BucketRuleException.Reason;
import com.eyegil.gara.catalog.service.BucketService;
import com.eyegil.gara.s3.web.ReservedPaths;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Bucket management for the admin UI. Adds the one rule the catalog cannot know: a bucket name must not collide
 * with a path the application reserves (such as {@code /admin}).
 */
@Service
@RequiredArgsConstructor
public class BucketAdminService {

    private final BucketService bucketService;
    private final ReservedPaths reservedPaths;

    public Bucket create(CreateBucketRequest request) {
        if (request.name() != null && reservedPaths.isReserved("/" + request.name())) {
            throw new BucketRuleException(Reason.INVALID, request.name() + " is reserved and cannot be a bucket name");
        }
        return bucketService.create(request.name(), request.repository(), request.directory(), request.createRepository(),
                request.publicRepos());
    }

    public void delete(String name) {
        bucketService.delete(name);
    }
}
