package com.arcane.Arcane.web.PatchNote.dto;

import java.util.List;

public class RiotPatchNoteDto {
    public record PatchNoteListResponse(
            List<PatchNoteSummary> patches
    ) {
    }

    public record PatchNoteSummary(
            String patchVersion,
            String title,
            String url,
            String publishedAt
    ) {
    }

    public record ChampionPatchResponse(
            String championName,
            List<ChampionPatchNote> patches
    ) {
    }

    public record ChampionPatchNote(
            String patchVersion,
            String title,
            String url,
            String publishedAt,
            List<ChampionPatchChange> changes
    ) {
    }

    public record ChampionPatchChange(
            String sectionTitle,
            List<String> items
    ) {
    }
}
