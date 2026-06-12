package com.arcane.Arcane.riot.Data.Rune.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString; // ToString을 명시적으로 임포트

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@ToString(exclude = "slots") // 순환 참조를 막기 위해 slots 필드를 제외
public class RunePath {

    @Id
    private Long id;

    @Column(name = "rune_key")
    private String runeKey;
    private String name;
    private String icon;
    private String version;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "runePath", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RuneSlot> slots = new ArrayList<>();
}
