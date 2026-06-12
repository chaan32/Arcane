package com.arcane.Arcane.riot.Data.Rune.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@ToString(exclude = {"runePath", "runes"}) // 순환 참조를 막기 위해 두 필드 모두 제외
public class RuneSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "rune_path_id")
    private RunePath runePath;

    @OneToMany(mappedBy = "slot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rune> runes = new ArrayList<>();
}