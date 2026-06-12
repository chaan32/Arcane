package com.arcane.Arcane.web.PatchNote.controller;

import com.arcane.Arcane.common.Exception.Fail.DeleteFail;
import com.arcane.Arcane.common.Exception.Fail.EditFail;
import com.arcane.Arcane.common.Exception.Normal.CannotFoundPatchNote;
import com.arcane.Arcane.common.Exception.Normal.CannotFoundUser;
import com.arcane.Arcane.web.PatchNote.domain.PatchNote;
import com.arcane.Arcane.web.PatchNote.dto.PatchNoteDto;
import com.arcane.Arcane.web.PatchNote.service.PatchNoteService;
import com.arcane.Arcane.web.User.domain.User;
import com.arcane.Arcane.web.User.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/patchnote")
public class PatchNoteController {
    private final PatchNoteService patchNoteService;
    private final UserService userService;


    // 패치노트 업로드 하기 (관리자만 가능)
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')") // * ADMIN 역할만 이 메소드를 호출할 수 있도록 제한하기
    public ResponseEntity<PatchNoteDto.PatchNoteUploadResponseDto> createPatchNote(
            @RequestBody PatchNoteDto.PatchNoteRequest patchNotePatchNoteRequestDto,
            // 현재 로그인한 사용자의 loginId를 자동으로 가져 옴
            @AuthenticationPrincipal String loginId) throws CannotFoundUser {

        // 1) 받아온 loginID를 통해서 User 객체를 가져 온다
        User adminUser = userService.findByLoginId(loginId)
                .orElseThrow(() -> new CannotFoundUser("해당 관리자 정보를 찾을 수 없습니다."));

        // 2) PatchNote 게시물 내용과 adminUser의 정보를 넘겨서 DB에 저장할 수 있도록 Service로 넘겨줌
        PatchNote createdPatchNote = patchNoteService.save(patchNotePatchNoteRequestDto, adminUser);

        // 3) 반환받은 PatchNote 객체를 Response로 감싸서 리턴해준다
        return ResponseEntity.ok(new PatchNoteDto.PatchNoteUploadResponseDto(createdPatchNote));
    }

    // 패치노트 찾기
    @GetMapping("/find/{id}")
    public ResponseEntity<PatchNoteDto.PatchNoteResponseDto> findPatchNoteById(@PathVariable("id") Long id) throws CannotFoundPatchNote {

        PatchNote patchNote = patchNoteService.findById(id)
                .orElseThrow(() -> new CannotFoundPatchNote("해당 패치노트를 찾을 수 없습니다."));
        patchNoteService.addViews(patchNote);

        return ResponseEntity.ok(new PatchNoteDto.PatchNoteResponseDto(patchNote));
    }

    // 패치노트 목록 찾아오기
    @GetMapping("/find/all")
    public ResponseEntity<List<PatchNoteDto.PatchNoteListResponseDto>> findAllPatchNotes() throws CannotFoundPatchNote {
        List<PatchNote> patchNoteList = patchNoteService.findAll();

        if (patchNoteList.isEmpty()) {
            throw new CannotFoundPatchNote("패치노트가 없어요");
        }

        List<PatchNoteDto.PatchNoteListResponseDto> listDto = new ArrayList<>();
        for (PatchNote patchNote : patchNoteList) {
            listDto.add(new PatchNoteDto.PatchNoteListResponseDto(patchNote));
        }
        return ResponseEntity.ok(listDto);
    }


    // 패치노트 수정하기 (관리자만 가능)
    @PatchMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatchNoteDto.PatchNoteResponseDto> editPatchNote(@RequestBody PatchNoteDto.PatchNoteRequest patchNotePatchNoteRequestDto, @PathVariable("id") Long id, @AuthenticationPrincipal String loginId) throws CannotFoundUser, CannotFoundPatchNote {

        // 1) 받아온 loginID를 통해서 User 객체를 가져 온다
        User adminUser = userService.findByLoginId(loginId)
                .orElseThrow(() -> new CannotFoundUser("해당 관리자 정보를 찾을 수 없습니다."));

        try {
            // 2) 편집하기
            PatchNote editPatchNote = patchNoteService.edit(patchNotePatchNoteRequestDto, adminUser, id);

            // 3) 리턴
            return ResponseEntity.ok(new PatchNoteDto.PatchNoteResponseDto(editPatchNote));
        } catch (CannotFoundPatchNote e) {
            throw new CannotFoundPatchNote("해당 글을 찾을 수 없습니다.");
        } catch (Exception e) {
            throw new EditFail("해당 글 편집에 문제가 발생했습니다.");
        }
    }

    // 패치노트 삭제하기 (관리자만 가능)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deletePatchNote(@PathVariable("id") Long id, @AuthenticationPrincipal String loginId) throws Exception {
        User adminUser = userService.findByLoginId(loginId)
                .orElseThrow(() -> new CannotFoundUser("해당 관리자 정보를 찾을 수 없습니다."));

        try{
            patchNoteService.deleteById(id);
            return ResponseEntity.ok(new String("해당 글이 삭제 되었습니다."));

        } catch (Exception e) {
            throw new DeleteFail("해당 글 삭제에 문제가 발생했습니다.");
        }
    }

    // 패치내용 글 좋아요
    @PostMapping("/show/{id}/likes")
    public ResponseEntity<Map<String, String>> addLikeToPatchNote(@PathVariable("id") Long id) {

        patchNoteService.addLikeById(id);
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("message", id + "번 글에 좋아요를 눌렀습니다.");
        return ResponseEntity.status(HttpStatus.OK).body(responseBody);
    }

    // 패치내용 글 싫어요
    @PostMapping("/show/{id}/dislikes")
    public ResponseEntity<Map<String, String>> likePatchNote(@PathVariable("id") Long id)  {
        patchNoteService.addDislikeById(id);
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("message", id +"번 글에 싫어요를 눌렀습니다.");
        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }
}
