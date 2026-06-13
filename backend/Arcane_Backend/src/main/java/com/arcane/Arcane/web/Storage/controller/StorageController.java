package com.arcane.Arcane.web.Storage.controller;

import com.arcane.Arcane.common.Logging.ApiLogSupport;
import com.arcane.Arcane.web.Storage.dto.GuideImageUploadResponse;
import com.arcane.Arcane.web.Storage.service.GuideImageStorageService;
import com.arcane.Arcane.web.User.domain.User;
import com.arcane.Arcane.web.User.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/storage")
public class StorageController {
    private final GuideImageStorageService guideImageStorageService;
    private final UserService userService;

    @PostMapping(value = "/guide-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GuideImageUploadResponse> uploadGuideImage(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal String loginId
    ) {
        try {
            User uploader = userService.findByLoginId(loginId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));

            GuideImageUploadResponse response = guideImageStorageService.uploadGuideImage(file, uploader);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error(ApiLogSupport.api(
                    "공략 이미지 업로드",
                    "StorageController.uploadGuideImage",
                    "FAILED",
                    "loginId=" + loginId + " | reason=" + e.getMessage()
            ), e);
            throw e;
        }
    }
}
