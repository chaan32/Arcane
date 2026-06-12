package com.arcane.Arcane.riot.Data.Rune;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.arcane.Arcane.riot.Data.Rune.domain.Rune;
import com.arcane.Arcane.riot.Data.Rune.domain.RunePath;
import com.arcane.Arcane.riot.Data.Rune.domain.RuneSlot;
import com.arcane.Arcane.riot.Data.Rune.dto.RuneResponseDto;
import com.arcane.Arcane.riot.Data.Rune.dto.RuneTreePathDto;
import com.arcane.Arcane.riot.Data.Rune.repository.RunePathRepository;
import com.arcane.Arcane.riot.Data.Rune.repository.RuneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RuneService {
    @Value("${riot.patch-version}")
    private String patchVersion;

    private final RunePathRepository runePathRepository;
    private final RuneRepository runeRepository;

    public void importRunes() throws IOException {
        String url = "https://ddragon.leagueoflegends.com/cdn/" + patchVersion + "/data/ko_KR/runesReforged.json";
        // 이미지용 기본 URL (Data Dragon은 img 폴더 경로가 별도입니다)
        String imgBaseUrl = "https://ddragon.leagueoflegends.com/cdn/img/";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new URL(url));

        for (JsonNode pathNode : root) {
            RunePath path = new RunePath();
            path.setId(pathNode.get("id").asLong());
            path.setRuneKey(pathNode.get("key").asText());
            path.setName(pathNode.get("name").asText());
            path.setVersion(patchVersion);
            path.setActive(true);
            path.setUpdatedAt(LocalDateTime.now());

            // ★ 수정 포인트: 저장할 때 전체 URL로 저장
            path.setIcon(imgBaseUrl + pathNode.get("icon").asText());

            List<RuneSlot> slots = new ArrayList<>();
            for (JsonNode slotNode : pathNode.get("slots")) {
                RuneSlot slot = new RuneSlot();
                slot.setRunePath(path);

                List<Rune> runes = new ArrayList<>();
                for (JsonNode runeNode : slotNode.get("runes")) {
                    Rune rune = new Rune();
                    rune.setId(runeNode.get("id").asLong());
                    rune.setRuneKey(runeNode.get("key").asText());
                    rune.setName(runeNode.get("name").asText());
                    rune.setVersion(patchVersion);
                    rune.setActive(true);
                    rune.setUpdatedAt(LocalDateTime.now());

                    // ★ 수정 포인트: 여기도 전체 URL로 저장
                    rune.setIcon(imgBaseUrl + runeNode.get("icon").asText());

                    rune.setShortDesc(runeNode.get("shortDesc").asText());
                    rune.setSlot(slot);
                    runes.add(rune);
                }
                slot.setRunes(runes);
                slots.add(slot);
            }
            path.setSlots(slots);
            runePathRepository.save(path);
        }
    }
    public RuneResponseDto getRuneInfoById(Long id) {
        if (id % 100 == 0) {
            return runePathRepository.findById(id)
                    .map(RuneResponseDto::new)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "존재하지 않는 룬 경로 ID입니다: " + id
                    ));
        }

        return runeRepository.findById(id)
                .map(RuneResponseDto::new)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "존재하지 않는 룬 ID입니다: " + id
                ));
    }
    public Optional<Rune> findRuneById(Long id){
        return runeRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<RuneTreePathDto> getRuneTree() {
        return runePathRepository.findAll().stream()
                .sorted(Comparator.comparing(RunePath::getId))
                .map(RuneTreePathDto::from)
                .toList();
    }
    public Boolean isRuneDbEmpty(){
        return runeRepository.findAll().isEmpty();
    }
}
