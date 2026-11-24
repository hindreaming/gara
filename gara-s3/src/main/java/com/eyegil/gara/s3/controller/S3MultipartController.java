package com.eyegil.gara.s3.controller;

import com.eyegil.gara.s3.dto.CompleteMultipartUpload;
import com.eyegil.gara.s3.dto.CompleteMultipartUploadResult;
import com.eyegil.gara.s3.dto.InitiateMultipartUploadResult;
import com.eyegil.gara.s3.dto.S3Payload;
import com.eyegil.gara.s3.service.S3MultipartService;
import com.eyegil.gara.s3.util.ObjectKeys;
import com.eyegil.gara.s3.util.S3Xml;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.eyegil.gara.s3.controller.S3ObjectController.OBJECT_PATH;

/**
 * S3 multipart upload endpoints: CreateMultipartUpload, UploadPart, CompleteMultipartUpload, AbortMultipartUpload.
 */
@RestController
@RequiredArgsConstructor
public class S3MultipartController {

    private final S3MultipartService s3MultipartService;
    private final S3Xml s3Xml;

    @PostMapping(value = OBJECT_PATH, params = "uploads", produces = MediaType.APPLICATION_XML_VALUE)
    public InitiateMultipartUploadResult createMultipartUpload(@PathVariable String bucket,
                                                               @PathVariable String key,
                                                               @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType) {
        String objectKey = ObjectKeys.fromPath(key);
        return new InitiateMultipartUploadResult(bucket, objectKey, s3MultipartService.initiate(bucket, objectKey, contentType));
    }

    @PutMapping(value = OBJECT_PATH, params = {"partNumber", "uploadId"}, consumes = MediaType.ALL_VALUE)
    public ResponseEntity<Void> uploadPart(@PathVariable String bucket,
                                           @PathVariable String key,
                                           @RequestParam int partNumber,
                                           @RequestParam String uploadId,
                                           S3Payload payload) {
        String etag = s3MultipartService.uploadPart(bucket, ObjectKeys.fromPath(key), uploadId, partNumber, payload.bytes());
        return ResponseEntity.ok().eTag(etag).build();
    }

    @PostMapping(value = OBJECT_PATH, params = "uploadId", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public CompleteMultipartUploadResult completeMultipartUpload(@PathVariable String bucket,
                                                                 @PathVariable String key,
                                                                 @RequestParam String uploadId,
                                                                 S3Payload payload,
                                                                 HttpServletRequest servletRequest) {
        String objectKey = ObjectKeys.fromPath(key);
        CompleteMultipartUpload request = s3Xml.read(payload.bytes(), CompleteMultipartUpload.class);
        String etag = s3MultipartService.complete(bucket, objectKey, uploadId, request.getParts());
        return new CompleteMultipartUploadResult(
                servletRequest.getRequestURL().toString(), bucket, objectKey, "\"" + etag + "\"");
    }

    @DeleteMapping(value = OBJECT_PATH, params = "uploadId")
    public ResponseEntity<Void> abortMultipartUpload(@PathVariable String bucket,
                                                     @PathVariable String key,
                                                     @RequestParam String uploadId) {
        s3MultipartService.abort(bucket, ObjectKeys.fromPath(key), uploadId);
        return ResponseEntity.noContent().build();
    }
}
