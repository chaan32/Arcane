package com.arcane.Arcane.riot.Data.Champion;

import com.arcane.Arcane.riot.Data.Champion.dto.ChampionResponseDto;
import com.arcane.Arcane.riot.Data.Champion.dto.ChampionReturnDto;
import com.arcane.Arcane.riot.Data.Champion.repository.ChampionRepository;
import com.arcane.Arcane.riot.Data.Champion.repository.ChampionSkinRepository;
import com.arcane.Arcane.riot.Data.Champion.repository.ChampionSpellRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service("inGameChampionService")
@RequiredArgsConstructor
@Slf4j
public class ChampionService {

    @Value("${riot.patch-version}")
    private String patchVersion;

    private final ChampionRepository championRepository;
    private final ChampionSpellRepository championSpellRepository;
    private final ChampionSkinRepository championSkinRepository;
    private final RestTemplate restTemplate;

    @Transactional(readOnly = true)
    public List<Long> getChampionIds() {
        return championRepository.findChampionIdAll();
    }

    /**
     * 1. 전체 챔피언 목록을 먼저 가져오고
     * 2. 각 챔피언별로 상세 URL을 다시 호출해서 저장함
     */
    public void importChampions() {

        String listUrl = "https://ddragon.leagueoflegends.com/cdn/" + patchVersion + "/data/ko_KR/champion.json";
        log.info("Requesting Champion List from: {}", listUrl);

        ChampionResponseDto response = restTemplate.getForObject(listUrl, ChampionResponseDto.class);

        if (response == null || response.getData() == null) {
            log.error("챔피언 목록을 가져오는데 실패했습니다.");
            return;
        }

        // 챔피언 ID 목록 추출
        for (String championId : response.getData().keySet()) {
            try {

                saveChampionDetail(championId);
            } catch (Exception e) {
                log.error("챔피언 저장 실패: {} - 이유: {}", championId, e.getMessage());
            }
        }
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveChampionDetail(String championId) {

        // ★ 핵심: 개별 상세 URL 생성 (예: .../champion/Blitzcrank.json)
        String detailUrl = "https://ddragon.leagueoflegends.com/cdn/" + patchVersion + "/data/ko_KR/champion/" + championId + ".json";

        // 상세 정보 요청
        ChampionResponseDto detailResponse = restTemplate.getForObject(detailUrl, ChampionResponseDto.class);

        if (detailResponse == null || detailResponse.getData() == null) {
            log.error("상세 정보 조회 실패: {}", championId);
            return;
        }

        ChampionDto dto = detailResponse.getData().get(championId);

        if (dto == null) return;



        //  패시브
        ChampionPassive passiveEntity = null;
        if (dto.getPassive() != null) {
            passiveEntity = ChampionPassive.builder()
                    .name(dto.getPassive().getName())
                    .description(dto.getPassive().getDescription())
                    .imageFull(dto.getPassive().getImage().getFull())
                    .build();
        }
        // 챔피언
        Champion champion = Champion.builder()
                .id(Long.parseLong(dto.getKey()))
                .nameKo(dto.getName())
                .nameEn(dto.getId())
                .title(dto.getTitle())
                .blurb(dto.getBlurb())
                .imageFull(dto.getImage().getFull())
                .tags(String.join(",", dto.getTags()))
                .version(patchVersion)
                .active(true)
                .updatedAt(LocalDateTime.now())
                .info(dto.getInfo())
                .stats(dto.getStats())
                .passive(passiveEntity)
                .build();

        Champion savedChampion = championRepository.save(champion);

        // 스킬
        List<ChampionSpell> spellList = new ArrayList<>();
        String[] spellKeys = {"Q", "W", "E", "R"};
        List<ChampionDto.SpellDto> spellDtos = dto.getSpells();

        if (spellDtos != null) {
            for (int i = 0; i < spellDtos.size(); i++) {
                if (i >= 4) break;
                ChampionDto.SpellDto sDto = spellDtos.get(i);

                ChampionSpell spell = ChampionSpell.builder()
                        .champion(savedChampion)
                        .spellId(sDto.getId())
                        .name(sDto.getName())
                        .description(sDto.getDescription())
                        .imageFull(sDto.getImage().getFull())
                        .cooldownBurn(sDto.getCooldownBurn())
                        .costBurn(sDto.getCostBurn())
                        .spellKey(spellKeys[i])
                        .version(patchVersion)
                        .active(true)
                        .updatedAt(LocalDateTime.now())
                        .build();

                spellList.add(spell);
            }
            championSpellRepository.saveAll(spellList);
        }

        //  스킨
        List<ChampionSkin> skinList = new ArrayList<>();
        if (dto.getSkins() != null) {
            for (ChampionDto.SkinDto skinDto : dto.getSkins()) {
                ChampionSkin skin = ChampionSkin.builder()
                        .champion(savedChampion)
                        .skinId(skinDto.getId())
                        .num(skinDto.getNum())
                        .name(skinDto.getName())
                        .chromas(skinDto.isChromas())
                        .build();

                skinList.add(skin);
            }
            championSkinRepository.saveAll(skinList);
        }

        log.info("상세 저장 완료: {} (스킨: {}, 스펠: {})", savedChampion.getNameKo(), skinList.size(), spellList.size());
    }

    public Optional<Champion> getChampionById(Long id){
        return championRepository.findById(id);
    }
    public ChampionReturnDto getChampionInfoDto(Long id){
        Champion champion = getChampionById(id).orElseThrow();
        return new ChampionReturnDto(champion);
    }
    public ChampionReturnDto getChampionInfoDtoByName(String nameEn){
        Champion byNameEn = championRepository.findByNameEn(nameEn).orElseThrow();
        return new ChampionReturnDto(byNameEn);
    }
}
