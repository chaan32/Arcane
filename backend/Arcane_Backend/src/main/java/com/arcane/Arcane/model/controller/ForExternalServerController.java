package com.arcane.Arcane.model.controller;

import com.arcane.Arcane.common.Exception.RiotAPI.CannotFoundSummoner;
import com.arcane.Arcane.model.dto.AiScoreBenchmarkResponse;
import com.arcane.Arcane.model.service.PythonService;
import com.arcane.Arcane.riot.RiotInform.service.RiotApiService;
import com.arcane.Arcane.model.dto.MatchModelDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tune")
@RequiredArgsConstructor
@Slf4j
public class ForExternalServerController {

    private final RiotApiService riotApiService;
    private final PythonService pythonService;

    // 1. 이 주소를 호출하면 화면으로 바로 이동합니다.
    @GetMapping("/learning")
    public ModelAndView learning(@RequestParam String puuid) {
        // 브라우저에게 "static/tune.html" 페이지로 가라고 명령 (puuid 포함)
        return new ModelAndView("redirect:/tune.html?puuid=" + puuid);
    }

    // 2. 이동한 html 화면에서 데이터를 요청할 때 사용하는 API
    @GetMapping("/learning-data")
    public List<MatchModelDto> getLearningData(@RequestParam String puuid) throws CannotFoundSummoner {
        // 20개 매치 데이터를 가져와서 반환
        String[] matchIds = riotApiService.getSummonerMatches(puuid, 1);

        List<MatchModelDto> matchModel = riotApiService.getMatchModel(matchIds);
        log.info("size:{} {}",matchIds.length, matchModel.size());
        return matchModel;
    }

    // 3. 점수 입력 후 '제출' 버튼 눌렀을 때 실행되는 학습 로직
    @PostMapping("/submit")
    public ResponseEntity<String> submitScores(@RequestBody Map<String, Integer> scores) {
        // scores 예시: {"0_0": 85, "0_1": 70, ...}
        // 여기서 AI 모델 파라미터 업데이트 로직을 돌리시면 됩니다.
        System.out.println("수집된 점수: " + scores);
        return ResponseEntity.ok("학습 완료");
    }

    @GetMapping("/score/benchmark")
    public ResponseEntity<AiScoreBenchmarkResponse> benchmarkScoreInference(
            @RequestParam(defaultValue = "10") int count
    ) {
        return ResponseEntity.ok(pythonService.benchmark(count));
    }

}
