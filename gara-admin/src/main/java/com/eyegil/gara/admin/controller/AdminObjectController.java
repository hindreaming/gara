package com.eyegil.gara.admin.controller;

import com.eyegil.gara.admin.dto.BrowseResponse;
import com.eyegil.gara.admin.service.ObjectBrowserService;
import com.eyegil.gara.s3.dto.S3ObjectMetadata;
import com.eyegil.gara.s3.dto.S3StoredObject;
import com.eyegil.gara.s3.service.S3ObjectService;
import com.eyegil.gara.s3.util.ObjectKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Object browser behind the admin UI: browse folders, upload, download and delete. Writes go through the same
 * object service as the S3 API, so the same locks, limits and repository rules apply.
 */
@RestController
@RequestMapping("/admin/api/buckets/{bucket}/objects")
@RequiredArgsConstructor
public class AdminObjectController {

    private final ObjectBrowserService objectBrowserService;
    private final S3ObjectService s3ObjectService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public BrowseResponse browse(@PathVariable String bucket,
                                 @RequestParam(defaultValue = "") String prefix) {
        return objectBrowserService.browse(bucket, prefix);
    }

    @GetMapping("/content")
    public ResponseEntity<byte[]> download(@PathVariable String bucket, @RequestParam String key) {
        S3StoredObject object = s3ObjectService.get(bucket, ObjectKeys.fromPath(key));
        String fileName = key.substring(key.lastIndexOf('/') + 1);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(object.metadata().contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
                .body(object.content());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public S3ObjectMetadata upload(@PathVariable String bucket,
                                   @RequestParam(defaultValue = "") String prefix,
                                   @RequestParam MultipartFile file) throws IOException {
        String key = ObjectKeys.fromPath(prefix + file.getOriginalFilename());
        return s3ObjectService.put(bucket, key, file.getBytes(), file.getContentType());
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable String bucket, @RequestParam String key) {
        s3ObjectService.delete(bucket, ObjectKeys.fromPath(key));
        return ResponseEntity.noContent().build();
    }
}
