package com.arcane.Arcane.web.PatchNote.controller;

import com.arcane.Arcane.web.PatchNote.dto.RiotPatchNoteDto;
import com.arcane.Arcane.web.PatchNote.service.RiotPatchNoteCrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/patchnote/riot")
public class RiotPatchNoteController {
    private final RiotPatchNoteCrawlerService riotPatchNoteCrawlerService;

    @GetMapping
    public ResponseEntity<RiotPatchNoteDto.PatchNoteListResponse> findPatchNotes(
            @RequestParam(defaultValue = "2026") int fromYear
    ) {
        return ResponseEntity.ok(riotPatchNoteCrawlerService.findPatchNotes(fromYear));
    }

    @GetMapping("/champion/{championName}")
    public ResponseEntity<RiotPatchNoteDto.ChampionPatchResponse> findChampionPatchNotes(
            @PathVariable String championName,
            @RequestParam(defaultValue = "2026") int fromYear
    ) {
        return ResponseEntity.ok(riotPatchNoteCrawlerService.findChampionPatchNotes(championName, fromYear));
    }

    @DeleteMapping("/cache")
    public ResponseEntity<Void> clearCache() {
        riotPatchNoteCrawlerService.clearCache();
        return ResponseEntity.noContent().build();
    }
}
