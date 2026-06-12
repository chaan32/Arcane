package com.arcane.Arcane.riot.Ranker.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Queue;

@Getter
@Builder
public class RankerFinalResDto {
    Queue<RankerResDto> rankers;
    private Long totalPage;
    private Long currentPage;

    public static RankerFinalResDto from(Queue<RankerResDto> rankersByKey, Long totalPage, Long currentPage){
        return RankerFinalResDto.builder()
                .rankers(rankersByKey)
                .totalPage(totalPage)
                .currentPage(currentPage)
                .build();
    }
}
