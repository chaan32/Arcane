package com.arcane.Arcane.web.Guide.controller;


import com.arcane.Arcane.common.Exception.Normal.CannotFoundChampion;
import com.arcane.Arcane.common.Exception.Normal.CannotFoundGuide;
import com.arcane.Arcane.common.Exception.Normal.CannotFoundUser;
import com.arcane.Arcane.common.Logging.ApiLogSupport;
import com.arcane.Arcane.web.Guide.dto.GuideSearchDto;
import com.arcane.Arcane.web.Guide.service.GuideSearchService;
import com.arcane.Arcane.web.Guide.service.GuideService;
import com.arcane.Arcane.riot.Data.Champion.Champion;
import com.arcane.Arcane.riot.Data.Champion.ChampionService;
import com.arcane.Arcane.web.User.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.arcane.Arcane.web.Guide.dto.GuideDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arcane.Arcane.web.Guide.domain.Guide;
import com.arcane.Arcane.web.User.domain.User;

@Slf4j  //로그 출력
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/strategy")
public class GuideController {
    private final GuideService guideService;
    private final GuideSearchService guideSearchService;
    private final UserService userService;
    private final ChampionService championService;

    // 게시글 생성
    // 수정해야할 것 -> 로그인하지 않은 사람 (토큰이 없는 사람)이 포스팅을 하려할 때 로그인을 요청하는 오류 발생 시키기
    @PostMapping("/upload")
    public ResponseEntity<GuideDto.GuideUploadResponseDto> createGuide(
            @RequestBody GuideDto.GuideRequest guideRequest,
            @AuthenticationPrincipal String loginId)
    {
        try {
            log.info(ApiLogSupport.BUSINESS_FLOW, "guide", "createApi", "START", "title=" + guideRequest.getTitle() + ", loginId=" + loginId);

            // 유저 정보 받아오기
            User author = userService.findByLoginId(loginId).orElseThrow(() -> new CannotFoundUser ("해당 유저 정보를 찾을 수 없습니다."));

            Champion champion = championService.getChampionById(guideRequest.getChampionId())
                    .orElseThrow(()-> new CannotFoundChampion("id = "+ guideRequest.getChampionId()+" 인 챔피언을 찾을 수 없습니다. "));

            Guide saved = guideService.save(guideRequest, author, champion);
            return ResponseEntity.ok(new GuideDto.GuideUploadResponseDto(saved));

        } catch (Exception e) {
            log.error(ApiLogSupport.BUSINESS_FLOW, "guide", "createApi", "FAILED", "title=" + guideRequest.getTitle() + ", reason=" + e.getMessage(), e);
            throw new RuntimeException("공략 create 실패: " + e.getMessage());
        }
    }

    // 전체 공략 조회
    // 수정 사항 1) /list -> /find/all : 명확성 부여find/champion
    @GetMapping("/find/all")
    public ResponseEntity<List<GuideDto.GuideListResponseDto>> getGuideList() throws CannotFoundGuide {
        List<Guide> guideEntityList = guideService.findAll();

        // 수정 사항 2) 비어 있는 경우는 에러가 아님 그냥 빈 리스트를 반환해주면 됨
        /*
        if (guideEntityList.isEmpty()) {
            throw new CannotFoundGuide("공략이 존재하지 않습니다");
        }
        */

        List<GuideDto.GuideListResponseDto> guideDtoList = new ArrayList<>();
        for (Guide guide : guideEntityList) {
            guideDtoList.add(new GuideDto.GuideListResponseDto(guide));
        }

        return ResponseEntity.ok(guideDtoList);
    }
    // 챔피언 별로 공략글 찾기
    @GetMapping("/find/champion/{id}")
    public ResponseEntity<List<GuideDto.GuideListResponseDto>> findChampGuide(@PathVariable Long id) throws CannotFoundGuide {
        Champion champion = championService.getChampionById(id)
                .orElseThrow(() -> new CannotFoundChampion("id = " + id + " 인 챔피언을 찾을 수 없습니다. "));

        List<Guide> byChampion = guideService.findByChampion(champion);

        List<GuideDto.GuideListResponseDto> guideDtoList = new ArrayList<>();

        for (Guide guide : byChampion) {
            guideDtoList.add(new GuideDto.GuideListResponseDto(guide));
        }
        return ResponseEntity.ok(guideDtoList);
    }


    // 단일 공략 조회
    // 수정 사항 2) /detail -> /find : 전체 게시물 찾는거랑 일관성 부여
    // 수정 사항 8) 싫어요는 DTO에 넣지 않아서 추가 함

    @GetMapping("/find/{id}")
    public ResponseEntity<GuideDto.GuideResponseDto> getGuideById(@PathVariable Long id) throws CannotFoundGuide {
        Guide guideEntity = guideService.findById(id)
                .orElseThrow(()-> new CannotFoundGuide("해당 공략 정보를 찾을 수 없습니다.")); // 이미 예외처리 포함된 서비스 메서드 👍
        guideService.addViews(guideEntity);

        return ResponseEntity.ok( new GuideDto.GuideResponseDto(guideEntity));
    }
    // 작성자 별로 공략글 찾기
//    @GetMapping("/find/author/{id}")
//    public ResponseEntity<GuideDto.GuideListResponseDto> getGuideByAuthor(@PathVariable Long id) throws CannotFoundGuide {
//
//    }


    // 공략 수정
    // 수정 사항 3) 작성자만 수정할 수 있게 해야함
    // 수정 사항 9) DTO를 업로드 용으로 DTO로 해놔서 제목만 보임
    @PatchMapping("/edit/{id}")
    public ResponseEntity<GuideDto.GuideResponseDto> updateGuide(
            @PathVariable Long id,
            @AuthenticationPrincipal String loginId,
            @RequestBody GuideDto.GuideRequest dto) throws CannotFoundGuide, CannotFoundUser {
        // 1) author 찾기
        User author = userService.findByLoginId(loginId)
                .orElseThrow(() -> new CannotFoundUser("해당 유저 정보를 찾을 수 없습니다"));

        Guide guideEntity = guideService.edit(id, dto, author);
        GuideDto.GuideResponseDto guideDto = new GuideDto.GuideResponseDto(guideEntity);
        return ResponseEntity.ok(guideDto);

    }

    // 공략 삭제
    // 수정 사항 5) 작성자만 삭제할 수 있게 해야 함
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteGuide(@PathVariable Long id, @AuthenticationPrincipal String loginId)  {
        try {
            User author = userService.findByLoginId(loginId)
                    .orElseThrow(() -> new CannotFoundUser("해당 유저 정보를 찾을 수 없습니다."));

            guideService.deleteById(id, author);
            return ResponseEntity.ok("공략 삭제 완료: " + id);
        } catch (Exception e) {
            log.error(ApiLogSupport.BUSINESS_FLOW, "guide", "deleteApi", "FAILED", "guideId=" + id + ", reason=" + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("공략 삭제 실패: " + e.getMessage());
        }
    }

    // 좋아요
    @PostMapping("/detail/{id}/likes")
    public ResponseEntity<Map<String, Object>> addLikeToGuide(@PathVariable Long id)  {
        guideService.addLikeById(id);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", id + "번 글에 좋아요를 눌렀습니다.");
        return ResponseEntity.status(HttpStatus.OK).body(responseBody);
    }

    // 싫어요
    @PostMapping("/detail/{id}/dislikes")
    public ResponseEntity<Map<String, Object>> addDislikeToGuide(@PathVariable Long id)  {
        guideService.addDislikeById(id);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", id +"번 글에 싫어요를 눌렀습니다.");
        return ResponseEntity.status(HttpStatus.OK).body(responseBody);
    }

    @GetMapping("/search/db")
    public ResponseEntity<GuideSearchDto.GuideSearchResponse> searchGuideWithDatabase(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(guideSearchService.searchWithDatabase(keyword, limit));
    }

    @GetMapping("/search/elasticsearch")
    public ResponseEntity<GuideSearchDto.GuideSearchResponse> searchGuideWithElasticsearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(guideSearchService.searchWithElasticsearch(keyword, limit));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/search/seed")
    public ResponseEntity<GuideSearchDto.GuideSeedResponse> seedBenchmarkGuides(
            @RequestParam(defaultValue = "600") int count
    ) {
        return ResponseEntity.ok(guideSearchService.seedBenchmarkGuides(count));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/search/elasticsearch/reindex")
    public ResponseEntity<GuideSearchDto.GuideReindexResponse> reindexGuideSearch() {
        return ResponseEntity.ok(guideSearchService.reindexAllGuides());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search/benchmark")
    public ResponseEntity<GuideSearchDto.GuideSearchBenchmarkResponse> benchmarkGuideSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "10") int iterations
    ) {
        return ResponseEntity.ok(guideSearchService.benchmark(keyword, limit, iterations));
    }



}
