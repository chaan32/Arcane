package com.arcane.Arcane.model.controller;

import com.arcane.Arcane.model.dto.AiScoreBenchmarkResponse;
import com.arcane.Arcane.model.service.PythonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/modeling")
@RequiredArgsConstructor
public class AiScoreBenchmarkController {
    private final PythonService pythonService;

    @GetMapping("/score/benchmark")
    public ResponseEntity<AiScoreBenchmarkResponse> benchmarkScoreInference(
            @RequestParam(defaultValue = "10") int count
    ) {
        return ResponseEntity.ok(pythonService.benchmark(count));
    }
}
