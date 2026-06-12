package com.arcane.Arcane.riot.Data.Champion;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChampionSkin {

    @Id
    private String skinId; // "101000" (라이엇 스킨 ID는 고유하므로 PK 사용 가능)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "champion_key")
    private Champion champion;

    private int num;         // 0, 1, 2...
    private String name;     // "룬 제라스"
    private boolean chromas; // 크로마 유무
}