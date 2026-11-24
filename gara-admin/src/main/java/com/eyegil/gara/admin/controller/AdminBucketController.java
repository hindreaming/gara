package com.eyegil.gara.admin.controller;

import com.eyegil.gara.admin.dto.CreateBucketRequest;
import com.eyegil.gara.admin.service.BucketAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Creates and deletes buckets. Listing them is part of the overview.
 */
@RestController
@RequestMapping("/admin/api/buckets")
@RequiredArgsConstructor
public class AdminBucketController {

    private final BucketAdminService bucketAdminService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> create(@RequestBody CreateBucketRequest request) {
        bucketAdminService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{bucket}")
    public ResponseEntity<Void> delete(@PathVariable String bucket) {
        bucketAdminService.delete(bucket);
        return ResponseEntity.noContent().build();
    }
}
