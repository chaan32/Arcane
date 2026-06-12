package com.arcane.Arcane.web.Statistics.service;

import com.arcane.Arcane.common.Exception.Normal.CannotFoundChampion;
import com.arcane.Arcane.riot.Data.Champion.Champion;
import com.arcane.Arcane.riot.Data.Champion.repository.ChampionRepository;
import com.arcane.Arcane.web.Statistics.domain.Champion.ChampionStatsByPosition;
import com.arcane.Arcane.web.Statistics.domain.MatchUp.MatchUp;
import com.arcane.Arcane.web.Statistics.domain.Position;
import com.arcane.Arcane.web.Statistics.dto.ChampionDetailDto;
import com.arcane.Arcane.web.Statistics.dto.ChampionNameResDto;
import com.arcane.Arcane.web.Statistics.dto.CounterChampionResDto;
import com.arcane.Arcane.web.Statistics.dto.TierResponseDto;
import com.arcane.Arcane.web.Statistics.dto.detail.ChampionDetailBuild;
import com.arcane.Arcane.web.Statistics.dto.detail.ChampionDetailChampInfo;
import com.arcane.Arcane.web.Statistics.dto.detail.ChampionDetailList;
import com.arcane.Arcane.web.Statistics.repository.ChampionStatisticsRepository;
import com.arcane.Arcane.web.Statistics.repository.MatchUpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 챔피언 통계 데이터를 관리하고 생성하는 서비스 클래스.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StatisticsService {

    private final ChampionStatisticsRepository championStatisticsRepository;
    private final ChampionRepository championRepository;
    private final MatchUpRepository matchUpRepository;

    @Transactional(readOnly = true) // 데이터를 읽기만 하므로 readOnly = true 옵션으로 성능을 최적화합니다.
    public List<ChampionStatsByPosition> getTiers(String positionString) {
        Position position = Position.fromString(positionString);
        return championStatisticsRepository.findByPosition(position);
    }

    @Transactional
    public void createAllPositionStatistics() {
        log.info("모든 포지션 통계 데이터 생성을 시작합니다.");

        processPositionData(getTopChampionNames(), Position.TOP);
        processPositionData(getJungleChampionNames(), Position.JUG);
        processPositionData(getMidChampionNames(), Position.MID);
        processPositionData(getAdcChampionNames(), Position.ADC);
        processPositionData(getSupportChampionNames(), Position.SUP);

        log.info("모든 포지션 통계 데이터 생성이 완료되었습니다.");
    }
    @Transactional(readOnly = true)
    public List<TierResponseDto> getTierResponseList(String positionString) {
        Position position = Position.fromString(positionString);
        List<ChampionStatsByPosition> statsList = championStatisticsRepository.findByPosition(position);



        return statsList.stream().map(stats -> {
            // 1. 카운터 챔피언 3명 조회
            List<Champion> counters = matchUpRepository.findTop3CountersByStats(
                    stats, PageRequest.of(0, 3)
            );

            // 2. 카운터 정보를 DTO 객체로 변환
            List<CounterChampionResDto> counterDtos = counters.stream()
                    .map(c -> CounterChampionResDto.builder()
                            .championNameEn(c.getNameEn())
                            .championImgUrl(c.getNameEn() + ".png")
                            .build())
                    .collect(Collectors.toList());

            // 3. 최종 TierResponseDto 반환
            return new TierResponseDto(stats, counterDtos);
        }).collect(Collectors.toList());
    }


    private void processPositionData(List<String> championNames, Position position) {

        log.info("{} 포지션 데이터 처리 중...", position);

        for (String championName : championNames) {
            Champion mainChampion = findByChampionName(championName);

            // 1. DB에서 메인 챔피언의 통계 정보를 찾고, '없으면 새로 생성'합니다.
            ChampionStatsByPosition mainStats = championStatisticsRepository
                    .findByChampionAndPosition(mainChampion, position)
                    .orElseGet(() -> new ChampionStatsByPosition(mainChampion, position));

            for (String opponentName : championNames) {
                log.info("{} vs {} 처리 중 ...", championName, opponentName);
                if (opponentName.equals(championName)) {
                    continue; // 자기 자신과의 매치업은 건너뜁니다.
                }
                Champion opponentChampion = findByChampionName(opponentName);

                // 2. 상대방 챔피언의 통계 정보도 똑같이 '없으면 새로 생성'합니다.
                ChampionStatsByPosition opponentStats = championStatisticsRepository
                        .findByChampionAndPosition(opponentChampion, position)
                        .orElseGet(() -> new ChampionStatsByPosition(opponentChampion, position));

                // 3. 상대 승률과 게임 수를 계산하고, 각자의 통계 객체에 매치업 정보를 추가합니다.
                // 이 로직은 양측의 매치업이 이미 존재하는지 확인 후 업데이트하는 로직으로 개선될 수 있습니다.
                float myWinRate = (float) ThreadLocalRandom.current().nextDouble(40.5, 59.5);


                mainStats.addMatchUp(opponentChampion, myWinRate);
                opponentStats.addMatchUp(mainChampion, 100 - myWinRate);

                // 4. 부모만 저장하면 Cascade 옵션에 의해 자식(MatchUp)도 함께 저장/업데이트됩니다.

                championStatisticsRepository.save(mainStats);
                championStatisticsRepository.save(opponentStats);
            }
        }
    }

    public List<ChampionDetailDto> getChampionDetail(String championName) {
        Champion champion = findByChampionNameEn(championName);
        List<ChampionStatsByPosition> stats = championStatisticsRepository.findByChampion(champion);

        List<ChampionDetailDto> dtos = new LinkedList<>();
        int totalGamePlayed = 0;
        float percent = (float) 100.0;
        if (stats.size() != 1) {
            for (ChampionStatsByPosition stat : stats) {
                totalGamePlayed += stat.getTotalGamesPlayed();
            }
        }

        for (ChampionStatsByPosition stat : stats) {

            ChampionDetailChampInfo info = new ChampionDetailChampInfo(stat);

            if (stats.size() != 0 && totalGamePlayed!=0) {
                percent = (float) info.getGameCount() / totalGamePlayed * 100;
            }
            info.setPercent(percent);

            ChampionDetailBuild build = new ChampionDetailBuild(stat);
            List<MatchUp> matchUps = matchUpRepository.findByOwnerStats(stat);
            ChampionDetailList relative = new ChampionDetailList(matchUps);
            ChampionDetailList synergy = new ChampionDetailList(matchUps);

            dtos.add(new ChampionDetailDto(info, build, relative, synergy));
        }
        return dtos;
    }

    public Queue<ChampionNameResDto> getAllName() {
        List<Champion> all = championRepository.findAll();
        Queue<ChampionNameResDto> dtos = new LinkedList<>();
        for (Champion champion : all) {
            dtos.add(ChampionNameResDto.of(champion));
        }
        return dtos;
    }

    private Champion findByChampionNameEn(String championNameEn){
        return championRepository.findByNameEn(championNameEn)
                .orElseThrow(() -> new CannotFoundChampion(championNameEn + "라는 이름의 챔피언은 존재하지 않습니다"));
    }


    private Champion findByChampionName(String championName) {
        return championRepository.findByNameKo(championName)
                .orElseThrow(() -> new CannotFoundChampion(championName + "라는 이름의 챔피언은 존재하지 않습니다"));
    }

    // 챔피언 이름 리스트를 반환하는 메서드들 (가독성 및 유지보수를 위해 분리)
    private List<String> getTopChampionNames() {
        return List.of("세트", "말파이트", "모데카이저", "리븐", "케일", "쉔", "아트록스", "이렐리아", "문도 박사", "잭스", "퀸", "우르곳", "피오라", "레넥톤", "워윅", "초가스", "케넨", "오른", "카밀", "다리우스", "갱플랭크", "그라가스", "나서스", "티모", "가렌", "블라디미르", "올라프", "사이온", "렝가", "요네", "그웬", "요릭", "야스오", "신지드", "하이머딩거", "제이스", "클레드", "트런들", "볼리베어", "자크", "나르", "크산테", "럼블", "라이즈", "일라오이", "사일러스", "베인", "카시오페아", "칼리스타", "탐 켄치", "판테온", "오로라", "갈리오", "아크샨", "뽀삐");
    }

    private List<String> getJungleChampionNames() {
        return List.of("그라가스", "그레이브즈", "니달리", "녹턴", "누누와 윌럼프", "다이애나", "람머스", "렉사이", "렝가", "리 신", "릴리아", "마스터 이", "바이", "벨베스", "볼리베어", "브라이어", "브랜드", "비에고", "샤코", "세주아니", "쉬바나", "신 짜오", "아무무", "아이번", "에코", "엘리스", "오공", "올라프", "우디르", "워윅", "자르반 4세", "자크", "카서스", "카직스", "케인", "킨드레드", "탈론", "트런들", "피들스틱", "헤카림", "잭스", "모데카이저", "그웬", "스카너", "세트");
    }

    private List<String> getMidChampionNames() {
        return List.of("갈리오", "라이즈", "럭스", "르블랑", "리산드라", "말자하", "벡스", "벨코즈", "브랜드", "블라디미르", "사일러스", "세라핀", "스웨인", "신드라", "아리", "아우렐리온 솔", "아지르", "아칼리", "아크샨", "애니", "애니비아", "야스오", "에코", "오리아나", "요네", "조이", "제라스", "제드", "직스", "카르마", "카사딘", "카타리나", "코르키", "키아나", "탈론", "탈리야", "트위스티드 페이트", "판테온", "피즈", "흐웨이", "니코", "다이애나", "이렐리아", "빅토르", "제이스", "베이가", "오로라", "카시오페아", "말파이트", "세트", "트리스타나", "자이라", "럼블", "클레드", "케일");
    }

    private List<String> getAdcChampionNames() {
        return List.of("드레이븐", "루시안", "미스 포츈", "바루스", "베인", "사미라", "세나", "스몰더", "시비르", "애쉬", "아펠리오스", "이즈리얼", "자야", "제리", "진", "징크스", "카이사", "칼리스타", "코그모", "트위치", "트리스타나", "닐라", "야스오", "유나라");
    }

    private List<String> getSupportChampionNames() {
        return List.of("노틸러스", "나미", "레오나", "레나타 글라스크", "렐", "룰루", "럭스", "라칸", "마오카이", "모르가나", "밀리오", "바드", "브라움", "브랜드", "블리츠크랭크", "세나", "세라핀", "소나", "소라카", "스웨인", "샤코", "쓰레쉬", "알리스타", "애쉬", "자이라", "잔나", "질리언", "카르마", "타릭", "탐 켄치", "파이크", "피들스틱", "하이머딩거", "벨코즈", "제라스", "오로라", "유미", "니코", "뽀삐");
    }
}