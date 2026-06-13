package com.arcane.Arcane.web.Storage.dto;

public record GuideImageUploadResponse(
        String url,
        String objectKey,
        long size,
        String contentType
) {
}
