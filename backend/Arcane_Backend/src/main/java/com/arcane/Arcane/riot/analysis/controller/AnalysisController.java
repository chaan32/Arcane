package com.arcane.Arcane.riot.analysis.controller;

import com.arcane.Arcane.common.Exception.Normal.CannotFoundChampion;
import com.arcane.Arcane.riot.Data.Champion.Champion;
import com.arcane.Arcane.riot.Data.Champion.repository.ChampionRepository;
import com.arcane.Arcane.riot.analysis.domain.ChampionAnalysisSnapshot;
import com.arcane.Arcane.riot.analysis.domain.ChampionMatchUpStat;
import com.arcane.Arcane.riot.analysis.domain.ChampionOptionStat;
import com.arcane.Arcane.riot.analysis.domain.ChampionOptionType;
import com.arcane.Arcane.riot.analysis.domain.ChampionPositionStat;
import com.arcane.Arcane.riot.analysis.domain.ChampionRuneStat;
import com.arcane.Arcane.riot.analysis.domain.ChampionTier;
import com.arcane.Arcane.riot.analysis.repository.ChampionAnalysisSnapshotRepository;
import com.arcane.Arcane.riot.analysis.repository.ChampionMatchUpStatRepository;
import com.arcane.Arcane.riot.analysis.repository.ChampionOptionStatRepository;
import com.arcane.Arcane.riot.analysis.repository.ChampionPositionStatRepository;
import com.arcane.Arcane.riot.analysis.repository.ChampionRuneStatRepository;
import com.arcane.Arcane.web.Statistics.dto.ChampionNameResDto;
import com.arcane.Arcane.web.Statistics.dto.CounterChampionResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {
    private final ChampionAnalysisSnapshotRepository championAnalysisSnapshotRepository;
    private final ChampionPositionStatRepository championPositionStatRepository;
    private final ChampionOptionStatRepository championOptionStatRepository;
    private final ChampionMatchUpStatRepository championMatchUpStatRepository;
    private final ChampionRuneStatRepository championRuneStatRepository;
    private final ChampionRepository championRepository;

    @GetMapping("/tier/{position}")
    public ResponseEntity<List<AnalysisTierResponse>> getPositionTier(@PathVariable String position) {
        String teamPosition = toTeamPosition(position);
        Map<Long, Champion> championMap = findChampionMap();
        String snapshotId = findActiveSnapshotId().orElse(null);
        if (snapshotId == null) {
            return ResponseEntity.ok(List.of());
        }

        List<ChampionPositionStat> stats = championPositionStatRepository.findBySnapshotId(snapshotId)
                .stream()
                .filter(stat -> stat.getTeamPosition().equals(teamPosition))
                .sorted(Comparator.comparing(
                        ChampionPositionStat::getTierScore,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        return ResponseEntity.ok(stats.stream()
                .map(stat -> toTierResponse(stat, championMap))
                .toList());
    }

    @GetMapping("/championDetail/{championName}")
    public ResponseEntity<List<AnalysisChampionDetailResponse>> getChampionDetail(
            @PathVariable String championName
    ) {
        Champion champion = findChampion(championName);
        String snapshotId = findActiveSnapshotId().orElse(null);
        if (snapshotId == null) {
            return ResponseEntity.ok(List.of());
        }

        List<ChampionPositionStat> stats = championPositionStatRepository
                .findBySnapshotIdAndChampionId(snapshotId, champion.getId())
                .stream()
                .sorted(Comparator.comparing(
                        ChampionPositionStat::getGames,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        int totalGames = stats.stream()
                .mapToInt(stat -> stat.getGames() == null ? 0 : stat.getGames())
                .sum();

        return ResponseEntity.ok(stats.stream()
                .map(stat -> toChampionDetailResponse(snapshotId, champion, stat, totalGames))
                .toList());
    }

    @GetMapping("/champions/all")
    public ResponseEntity<Queue<ChampionNameResDto>> getAllChampionsName() {
        Queue<ChampionNameResDto> response = new LinkedList<>();
        championRepository.findAll().stream()
                .sorted(Comparator.comparing(Champion::getNameKo, Comparator.nullsLast(String::compareTo)))
                .map(ChampionNameResDto::of)
                .forEach(response::add);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/champions")
    public ResponseEntity<List<ChampionPositionStatResponse>> getChampionStats(
            @RequestParam(required = false) String position,
            @RequestParam(required = false) ChampionTier tier
    ) {
        return ResponseEntity.ok(findActiveStats().stream()
                .filter(stat -> matchesPosition(stat, position))
                .filter(stat -> tier == null || stat.getTier() == tier)
                .sorted(Comparator.comparing(
                        ChampionPositionStat::getTierScore,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(ChampionPositionStatResponse::from)
                .toList());
    }

    @GetMapping("/champions/{championId}")
    public ResponseEntity<List<ChampionPositionStatResponse>> getChampionStatsByChampion(
            @PathVariable Long championId
    ) {
        return ResponseEntity.ok(findActiveStats().stream()
                .filter(stat -> stat.getChampionId().equals(championId))
                .sorted(Comparator.comparing(
                        ChampionPositionStat::getTierScore,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(ChampionPositionStatResponse::from)
                .toList());
    }

    private List<ChampionPositionStat> findActiveStats() {
        return findActiveSnapshotId()
                .map(championPositionStatRepository::findBySnapshotId)
                .orElse(List.of());
    }

    private boolean matchesPosition(ChampionPositionStat stat, String position) {
        return position == null
                || position.isBlank()
                || stat.getTeamPosition().equalsIgnoreCase(toTeamPosition(position));
    }

    private Optional<String> findActiveSnapshotId() {
        return championAnalysisSnapshotRepository.findByActiveTrue()
                .map(ChampionAnalysisSnapshot::getSnapshotId);
    }

    private Map<Long, Champion> findChampionMap() {
        return championRepository.findAll().stream()
                .collect(Collectors.toMap(Champion::getId, Function.identity()));
    }

    private Champion findChampion(String championName) {
        return championRepository.findByNameEn(championName)
                .or(() -> championRepository.findByNameKo(championName))
                .orElseThrow(() -> new CannotFoundChampion(championName + "라는 이름의 챔피언은 존재하지 않습니다"));
    }

    private AnalysisTierResponse toTierResponse(
            ChampionPositionStat stat,
            Map<Long, Champion> championMap
    ) {
        Champion champion = championMap.get(stat.getChampionId());
        List<CounterChampionResDto> counters = findCounterChampions(
                stat.getSnapshotId(),
                stat.getChampionId(),
                stat.getTeamPosition(),
                championMap,
                3
        );

        return new AnalysisTierResponse(
                championId(champion, stat.getChampionId()),
                championNameKo(champion, stat.getChampionId()),
                championNameEn(champion, stat.getChampionId()),
                championImageFull(champion),
                championVersion(champion),
                tierNumber(stat.getTier()),
                integer(stat.getTierScore()),
                integer(stat.getTierScore()) - 50,
                floatValue(stat.getPickRate()),
                floatValue(stat.getWinRate()),
                counters
        );
    }

    private AnalysisChampionDetailResponse toChampionDetailResponse(
            String snapshotId,
            Champion champion,
            ChampionPositionStat stat,
            int totalGames
    ) {
        List<ChampionOptionStat> itemStats = championOptionStatRepository
                .findBySnapshotIdAndChampionIdAndTeamPositionAndOptionType(
                        snapshotId,
                        champion.getId(),
                        stat.getTeamPosition(),
                        ChampionOptionType.ITEM
                )
                .stream()
                .sorted(Comparator.comparing(
                        ChampionOptionStat::getGames,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        List<ChampionOptionStat> spellStats = championOptionStatRepository
                .findBySnapshotIdAndChampionIdAndTeamPositionAndOptionType(
                        snapshotId,
                        champion.getId(),
                        stat.getTeamPosition(),
                        ChampionOptionType.SPELL
                )
                .stream()
                .sorted(Comparator.comparing(
                        ChampionOptionStat::getGames,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        List<ChampionRuneStat> runeStats = championRuneStatRepository
                .findBySnapshotIdAndChampionIdAndTeamPosition(
                        snapshotId,
                        champion.getId(),
                        stat.getTeamPosition()
                )
                .stream()
                .sorted(Comparator.comparing(
                        ChampionRuneStat::getGames,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        List<AnalysisChampionDetailWith> relativeWinRate = findMatchUps(
                snapshotId,
                champion.getId(),
                stat.getTeamPosition(),
                Comparator.comparing(ChampionMatchUpStat::getWinRate, Comparator.nullsLast(BigDecimal::compareTo))
        );

        List<AnalysisChampionDetailWith> favorableMatchUps = findMatchUps(
                snapshotId,
                champion.getId(),
                stat.getTeamPosition(),
                Comparator.comparing(
                        ChampionMatchUpStat::getWinRate,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );

        AnalysisChampionDetailChampInfo info = new AnalysisChampionDetailChampInfo(
                champion.getId(),
                champion.getNameKo(),
                champion.getNameEn(),
                champion.getImageFull(),
                champion.getVersion(),
                tierNumber(stat.getTier()),
                floatValue(stat.getWinRate()),
                floatValue(stat.getPickRate()),
                0.0f,
                stat.getGames(),
                totalGames <= 0 ? 0.0f : stat.getGames() * 100.0f / totalGames
        );

        AnalysisChampionDetailBuild build = new AnalysisChampionDetailBuild(
                toLegacyPosition(stat.getTeamPosition()),
                itemId(itemStats, 0),
                itemId(itemStats, 1),
                itemId(itemStats, 2),
                itemId(itemStats, 3),
                itemId(itemStats, 4),
                itemId(itemStats, 5),
                spellId(spellStats, 0, true),
                spellId(spellStats, 0, false),
                spellId(spellStats, 1, true),
                spellId(spellStats, 1, false),
                toPerks(runeStats),
                itemStats.stream()
                        .limit(3)
                        .map(this::toOptionStatResponse)
                        .toList(),
                spellStats.stream()
                        .limit(2)
                        .map(this::toOptionStatResponse)
                        .toList()
        );

        return new AnalysisChampionDetailResponse(
                info,
                build,
                new AnalysisChampionDetailList(relativeWinRate),
                new AnalysisChampionDetailList(favorableMatchUps)
        );
    }

    private List<CounterChampionResDto> findCounterChampions(
            String snapshotId,
            Long championId,
            String teamPosition,
            Map<Long, Champion> championMap,
            int limit
    ) {
        return championMatchUpStatRepository
                .findBySnapshotIdAndChampionIdAndTeamPosition(snapshotId, championId, teamPosition)
                .stream()
                .sorted(Comparator.comparing(
                        ChampionMatchUpStat::getWinRate,
                        Comparator.nullsLast(BigDecimal::compareTo)
                ))
                .limit(limit)
                .map(matchUp -> {
                    Champion opponent = championMap.get(matchUp.getOpponentChampionId());
                    return CounterChampionResDto.builder()
                            .championId(championId(opponent, matchUp.getOpponentChampionId()))
                            .championNameEn(championNameEn(opponent, matchUp.getOpponentChampionId()))
                            .championImageFull(championImageFull(opponent))
                            .championImgUrl(championNameEn(opponent, matchUp.getOpponentChampionId()) + ".png")
                            .build();
                })
                .toList();
    }

    private List<AnalysisChampionDetailWith> findMatchUps(
            String snapshotId,
            Long championId,
            String teamPosition,
            Comparator<ChampionMatchUpStat> comparator
    ) {
        Map<Long, Champion> championMap = findChampionMap();

        return championMatchUpStatRepository
                .findBySnapshotIdAndChampionIdAndTeamPosition(snapshotId, championId, teamPosition)
                .stream()
                .sorted(comparator)
                .limit(10)
                .map(matchUp -> {
                    Champion opponent = championMap.get(matchUp.getOpponentChampionId());
                    return new AnalysisChampionDetailWith(
                            championId(opponent, matchUp.getOpponentChampionId()),
                            championNameKo(opponent, matchUp.getOpponentChampionId()),
                            championNameEn(opponent, matchUp.getOpponentChampionId()),
                            championImageFull(opponent),
                            matchUp.getGames(),
                            floatValue(matchUp.getWinRate())
                    );
                })
                .toList();
    }

    private String toTeamPosition(String position) {
        if (position == null) {
            return "";
        }

        return switch (position.trim().toLowerCase()) {
            case "top" -> "TOP";
            case "jug", "jungle" -> "JUNGLE";
            case "mid", "middle" -> "MIDDLE";
            case "adc", "bottom" -> "BOTTOM";
            case "sup", "support", "utility" -> "UTILITY";
            default -> position.trim().toUpperCase();
        };
    }

    private String toLegacyPosition(String teamPosition) {
        return switch (teamPosition) {
            case "TOP" -> "TOP";
            case "JUNGLE" -> "JUG";
            case "MIDDLE" -> "MID";
            case "BOTTOM" -> "ADC";
            case "UTILITY" -> "SUP";
            default -> teamPosition;
        };
    }

    private Integer tierNumber(ChampionTier tier) {
        if (tier == null) {
            return 5;
        }

        return switch (tier) {
            case OP -> 1;
            case TIER_1 -> 2;
            case TIER_2 -> 3;
            case TIER_3 -> 4;
            case TIER_4 -> 5;
        };
    }

    private Integer integer(BigDecimal value) {
        if (value == null) {
            return 0;
        }
        return value.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private Float floatValue(BigDecimal value) {
        return value == null ? 0.0f : value.floatValue();
    }

    private String championNameKo(Champion champion, Long championId) {
        return champion == null ? "Unknown-" + championId : champion.getNameKo();
    }

    private String championNameEn(Champion champion, Long championId) {
        return champion == null ? String.valueOf(championId) : champion.getNameEn();
    }

    private Long championId(Champion champion, Long fallbackChampionId) {
        return champion == null ? fallbackChampionId : champion.getId();
    }

    private String championImageFull(Champion champion) {
        return champion == null ? null : champion.getImageFull();
    }

    private String championVersion(Champion champion) {
        return champion == null ? null : champion.getVersion();
    }

    private Integer itemId(List<ChampionOptionStat> itemStats, int index) {
        if (index >= itemStats.size() || itemStats.get(index).getItemId() == null) {
            return 0;
        }
        return itemStats.get(index).getItemId().intValue();
    }

    private Integer spellId(List<ChampionOptionStat> spellStats, int index, boolean first) {
        if (index >= spellStats.size()) {
            return 0;
        }

        ChampionOptionStat stat = spellStats.get(index);
        Integer spellId = first ? stat.getSpell1Id() : stat.getSpell2Id();
        return spellId == null ? 0 : spellId;
    }

    private AnalysisOptionStatResponse toOptionStatResponse(ChampionOptionStat stat) {
        return new AnalysisOptionStatResponse(
                stat.getItemId() == null ? null : stat.getItemId().intValue(),
                stat.getSpell1Id(),
                stat.getSpell2Id(),
                stat.getGames(),
                floatValue(stat.getWinRate()),
                floatValue(stat.getPickRate())
        );
    }

    private AnalysisPerksResponse emptyPerks() {
        return new AnalysisPerksResponse(
                new AnalysisStatPerksResponse(0, 0, 0),
                new ArrayList<>()
        );
    }

    private AnalysisPerksResponse toPerks(List<ChampionRuneStat> runeStats) {
        if (runeStats.isEmpty()) {
            return emptyPerks();
        }

        ChampionRuneStat stat = runeStats.get(0);
        List<AnalysisStyleResponse> styles = new ArrayList<>();

        List<AnalysisSelectionResponse> primarySelections = selections(
                stat.getPrimaryPerk1(),
                stat.getPrimaryPerk2(),
                stat.getPrimaryPerk3(),
                stat.getPrimaryPerk4()
        );
        if (positive(stat.getPrimaryStyle()) && !primarySelections.isEmpty()) {
            styles.add(new AnalysisStyleResponse(
                    "primaryStyle",
                    stat.getPrimaryStyle(),
                    primarySelections
            ));
        }

        List<AnalysisSelectionResponse> subSelections = selections(
                stat.getSubPerk1(),
                stat.getSubPerk2()
        );
        if (positive(stat.getSubStyle()) && !subSelections.isEmpty()) {
            styles.add(new AnalysisStyleResponse(
                    "subStyle",
                    stat.getSubStyle(),
                    subSelections
            ));
        }

        return new AnalysisPerksResponse(
                new AnalysisStatPerksResponse(
                        zero(stat.getDefense()),
                        zero(stat.getFlex()),
                        zero(stat.getOffense())
                ),
                styles
        );
    }

    private List<AnalysisSelectionResponse> selections(Integer... perkIds) {
        List<AnalysisSelectionResponse> selections = new ArrayList<>();
        for (Integer perkId : perkIds) {
            if (positive(perkId)) {
                selections.add(new AnalysisSelectionResponse(perkId, 0, 0, 0));
            }
        }
        return selections;
    }

    private boolean positive(Integer value) {
        return value != null && value > 0;
    }

    private Integer zero(Integer value) {
        return value == null ? 0 : value;
    }

    public record AnalysisTierResponse(
            Long championId,
            String championName,
            String championNameEn,
            String championImageFull,
            String version,
            Integer tier,
            Integer score,
            Integer scoreDiff,
            Float pickRate,
            Float winRate,
            List<CounterChampionResDto> counterChampions
    ) {
    }

    public record AnalysisChampionDetailResponse(
            AnalysisChampionDetailChampInfo detailChampInfo,
            AnalysisChampionDetailBuild detailChampBuild,
            AnalysisChampionDetailList relativeWinRate,
            AnalysisChampionDetailList synergyChampion
    ) {
    }

    public record AnalysisChampionDetailChampInfo(
            Long championId,
            String championName,
            String championNameEn,
            String championImageFull,
            String version,
            Integer tier,
            Float winRate,
            Float pickRate,
            Float banRate,
            Integer gameCount,
            Float percent
    ) {
    }

    public record AnalysisChampionDetailBuild(
            String lane,
            Integer item01,
            Integer item02,
            Integer item03,
            Integer item04,
            Integer item05,
            Integer item06,
            Integer summoner1Id,
            Integer summoner2Id,
            Integer summoner3Id,
            Integer summoner4Id,
            AnalysisPerksResponse perks,
            List<AnalysisOptionStatResponse> topItems,
            List<AnalysisOptionStatResponse> topSummonerSpells
    ) {
    }

    public record AnalysisOptionStatResponse(
            Integer itemId,
            Integer spell1Id,
            Integer spell2Id,
            Integer games,
            Float winRate,
            Float pickRate
    ) {
    }

    public record AnalysisPerksResponse(
            AnalysisStatPerksResponse statPerks,
            List<AnalysisStyleResponse> styles
    ) {
    }

    public record AnalysisStatPerksResponse(
            Integer defense,
            Integer flex,
            Integer offense
    ) {
    }

    public record AnalysisStyleResponse(
            String description,
            Integer style,
            List<AnalysisSelectionResponse> selections
    ) {
    }

    public record AnalysisSelectionResponse(
            Integer perk,
            Integer var1,
            Integer var2,
            Integer var3
    ) {
    }

    public record AnalysisChampionDetailList(
            List<AnalysisChampionDetailWith> champions
    ) {
    }

    public record AnalysisChampionDetailWith(
            Long championId,
            String championName,
            String championNameEn,
            String championImageFull,
            Integer gameCount,
            Float winRate
    ) {
    }

    public record ChampionPositionStatResponse(
            String snapshotId,
            Long championId,
            Integer queueId,
            String teamPosition,
            Integer games,
            Integer wins,
            Integer losses,
            BigDecimal winRate,
            BigDecimal adjustedWinRate,
            BigDecimal pickRate,
            BigDecimal recencyScore,
            BigDecimal frequencyScore,
            BigDecimal performanceScore,
            BigDecimal tierScore,
            ChampionTier tier,
            String tierLabel,
            BigDecimal avgKills,
            BigDecimal avgDeaths,
            BigDecimal avgAssists,
            BigDecimal avgKda,
            BigDecimal avgCs,
            BigDecimal avgDamageDealt,
            BigDecimal avgDamageTaken,
            BigDecimal avgVisionScore
    ) {
        public static ChampionPositionStatResponse from(ChampionPositionStat stat) {
            ChampionTier tier = stat.getTier();

            return new ChampionPositionStatResponse(
                    stat.getSnapshotId(),
                    stat.getChampionId(),
                    stat.getQueueId(),
                    stat.getTeamPosition(),
                    stat.getGames(),
                    stat.getWins(),
                    stat.getLosses(),
                    stat.getWinRate(),
                    stat.getAdjustedWinRate(),
                    stat.getPickRate(),
                    stat.getRecencyScore(),
                    stat.getFrequencyScore(),
                    stat.getPerformanceScore(),
                    stat.getTierScore(),
                    tier,
                    tier == null ? null : tier.getLabel(),
                    stat.getAvgKills(),
                    stat.getAvgDeaths(),
                    stat.getAvgAssists(),
                    stat.getAvgKda(),
                    stat.getAvgCs(),
                    stat.getAvgDamageDealt(),
                    stat.getAvgDamageTaken(),
                    stat.getAvgVisionScore()
            );
        }
    }
}
