package com.arcane.Arcane.common.Storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "arcane.storage.s3")
public class S3StorageProperties {
    private String bucket = "";
    private String region = "ap-northeast-2";
    private String publicBaseUrl = "";
    private String guideImagePrefix = "guides";
    private long maxFileSizeBytes = 5 * 1024 * 1024;

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getGuideImagePrefix() {
        return guideImagePrefix;
    }

    public void setGuideImagePrefix(String guideImagePrefix) {
        this.guideImagePrefix = guideImagePrefix;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }
}
