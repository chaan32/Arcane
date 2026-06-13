package com.arcane.Arcane.web.Storage.service;

import com.arcane.Arcane.common.Logging.ApiLogSupport;
import com.arcane.Arcane.common.Storage.S3StorageProperties;
import com.arcane.Arcane.web.Storage.dto.GuideImageUploadResponse;
import com.arcane.Arcane.web.User.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuideImageStorageService {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    private final S3Client s3Client;
    private final S3StorageProperties properties;

    public GuideImageUploadResponse uploadGuideImage(MultipartFile file, User uploader) {
        validate(file);

        String contentType = normalizeContentType(file.getContentType());
        String objectKey = createObjectKey(file, contentType, uploader);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(requireBucket())
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength(file.getSize())
                    .cacheControl("public, max-age=31536000, immutable")
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String url = buildPublicUrl(objectKey);
            log.info(ApiLogSupport.api(
                    "S3 이미지 업로드",
                    "GuideImageStorageService.uploadGuideImage",
                    "SUCCESS",
                    "userId=" + uploader.getId() + " | objectKey=" + objectKey + " | size=" + file.getSize()
            ));

            return new GuideImageUploadResponse(url, objectKey, file.getSize(), contentType);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 파일을 읽지 못했습니다.", e);
        } catch (S3Exception e) {
            log.error(ApiLogSupport.api(
                    "S3 이미지 업로드",
                    "GuideImageStorageService.uploadGuideImage",
                    "FAILED",
                    "userId=" + uploader.getId() + " | objectKey=" + objectKey + " | reason=" + e.awsErrorDetails().errorMessage()
            ), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "S3 이미지 업로드에 실패했습니다.", e);
        } catch (SdkClientException e) {
            log.error(ApiLogSupport.api(
                    "S3 이미지 업로드",
                    "GuideImageStorageService.uploadGuideImage",
                    "FAILED",
                    "userId=" + uploader.getId() + " | objectKey=" + objectKey + " | reason=" + e.getMessage()
            ), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "S3 연결 또는 인증 설정을 확인해주세요.", e);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "업로드할 이미지 파일이 없습니다.");
        }

        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "이미지 파일은 " + toMegabytes(properties.getMaxFileSizeBytes()) + "MB 이하만 업로드할 수 있습니다."
            );
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "png, jpg, webp, gif 이미지만 업로드할 수 있습니다.");
        }
    }

    private String createObjectKey(MultipartFile file, String contentType, User uploader) {
        String extension = resolveExtension(file, contentType);
        String prefix = cleanPathSegment(properties.getGuideImagePrefix(), "guides");
        LocalDate today = LocalDate.now();

        return "%s/%d/%04d/%02d/%02d/%s.%s".formatted(
                prefix,
                uploader.getId(),
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                UUID.randomUUID(),
                extension
        );
    }

    private String resolveExtension(MultipartFile file, String contentType) {
        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);

        if (extension == null || extension.isBlank()) {
            return EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
        }

        extension = extension.toLowerCase(Locale.ROOT);
        if (extension.equals("jpeg")) {
            return "jpg";
        }

        if (Set.of("jpg", "png", "webp", "gif").contains(extension)) {
            return extension;
        }

        return EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
    }

    private String buildPublicUrl(String objectKey) {
        String publicBaseUrl = properties.getPublicBaseUrl();
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl.replaceAll("/+$", "") + "/" + objectKey;
        }

        return "https://%s.s3.%s.amazonaws.com/%s".formatted(
                requireBucket(),
                properties.getRegion(),
                objectKey
        );
    }

    private String requireBucket() {
        String bucket = properties.getBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 버킷 설정이 없습니다.");
        }
        return bucket;
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
    }

    private String cleanPathSegment(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private long toMegabytes(long bytes) {
        return Math.max(1, bytes / 1024 / 1024);
    }
}
