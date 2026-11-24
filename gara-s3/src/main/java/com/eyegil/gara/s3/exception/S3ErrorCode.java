package com.eyegil.gara.s3.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * The subset of S3 error codes that Gara returns.
 */
@Getter
@RequiredArgsConstructor
public enum S3ErrorCode {

    NO_SUCH_BUCKET("NoSuchBucket", HttpStatus.NOT_FOUND, "The specified bucket does not exist."),
    NO_SUCH_KEY("NoSuchKey", HttpStatus.NOT_FOUND, "The specified key does not exist."),
    INVALID_ARGUMENT("InvalidArgument", HttpStatus.BAD_REQUEST, "Invalid argument."),
    INVALID_REQUEST("InvalidRequest", HttpStatus.BAD_REQUEST, "Invalid request."),
    ACCESS_DENIED("AccessDenied", HttpStatus.FORBIDDEN, "Access Denied"),
    INVALID_ACCESS_KEY_ID("InvalidAccessKeyId", HttpStatus.FORBIDDEN, "The AWS access key Id you provided does not exist in our records."),
    SIGNATURE_DOES_NOT_MATCH("SignatureDoesNotMatch", HttpStatus.FORBIDDEN, "The request signature we calculated does not match the signature you provided."),
    REQUEST_TIME_TOO_SKEWED("RequestTimeTooSkewed", HttpStatus.FORBIDDEN, "The difference between the request time and the server's time is too large."),
    AUTHORIZATION_HEADER_MALFORMED("AuthorizationHeaderMalformed", HttpStatus.BAD_REQUEST, "The authorization header is malformed."),
    NO_SUCH_UPLOAD("NoSuchUpload", HttpStatus.NOT_FOUND, "The specified multipart upload does not exist."),
    INVALID_PART("InvalidPart", HttpStatus.BAD_REQUEST, "One or more of the specified parts could not be found or its entity tag did not match."),
    INVALID_PART_ORDER("InvalidPartOrder", HttpStatus.BAD_REQUEST, "The list of parts was not in ascending order."),
    MALFORMED_XML("MalformedXML", HttpStatus.BAD_REQUEST, "The XML you provided was not well-formed."),
    BAD_DIGEST("BadDigest", HttpStatus.BAD_REQUEST, "The Content-MD5 you specified did not match what we received."),
    CONTENT_SHA256_MISMATCH("XAmzContentSHA256Mismatch", HttpStatus.BAD_REQUEST, "The provided x-amz-content-sha256 header does not match what was computed."),
    KEY_TOO_LONG("KeyTooLongError", HttpStatus.BAD_REQUEST, "Your key is too long."),
    ENTITY_TOO_LARGE("EntityTooLarge", HttpStatus.BAD_REQUEST, "Your proposed upload exceeds the maximum allowed object size."),
    SLOW_DOWN("SlowDown", HttpStatus.SERVICE_UNAVAILABLE, "Please reduce your request rate."),
    INTERNAL_ERROR("InternalError", HttpStatus.INTERNAL_SERVER_ERROR, "We encountered an internal error. Please try again.");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;
}
