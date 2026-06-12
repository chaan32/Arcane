package com.arcane.Arcane.riot.Data.Item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "riot_item_metadata",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_riot_item_metadata_item_version",
                        columnNames = {"item_id", "version"}
                )
        },
        indexes = {
                @Index(name = "idx_riot_item_metadata_item_id", columnList = "item_id"),
                @Index(name = "idx_riot_item_metadata_version_active", columnList = "version,is_active")
        }
)
public class ItemMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @Column(nullable = false, length = 30)
    private String version;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "image_full")
    private String imageFull;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
