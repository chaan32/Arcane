package com.arcane.Arcane.web.Guide.service;

import com.arcane.Arcane.common.Exception.Fail.DeleteFail;
import com.arcane.Arcane.common.Exception.Fail.EditFail;
import com.arcane.Arcane.common.Exception.Normal.CannotFoundGuide;
import com.arcane.Arcane.common.Logging.ApiLogSupport;
import com.arcane.Arcane.web.Guide.domain.Guide;
import com.arcane.Arcane.web.Guide.dto.GuideDto;
import com.arcane.Arcane.web.Guide.repository.GuideRepository;
import com.arcane.Arcane.riot.Data.Champion.Champion;
import com.arcane.Arcane.web.User.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class GuideService {

    private final GuideRepository guideRepository;

    // 공략 저장
    public Guide save(GuideDto.GuideRequest dto, User author, Champion champion) {
        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "guide",
                "create",
                "START",
                "title=" + dto.getTitle() + ", contentLength=" + contentLength(dto) + ", authorId=" + author.getId() + ", champion=" + champion.getNameKo()
        );
        Guide guide = new Guide(
                dto.getTitle(),
                dto.getContent(),
                champion,
                author
        );

        Guide savedGuide = guideRepository.save(guide);
        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "guide",
                "create",
                "SUCCESS",
                "guideId=" + savedGuide.getId() + ", authorId=" + author.getId()
        );
        return savedGuide;
//        return guide;
    }

    // 전체 조회
    public List<Guide> findAll() {
        List<Guide> guides = guideRepository.findAllByOrderByUpdatedAtDesc();
        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "guide",
                "findAll",
                "SUCCESS",
                "count=" + guides.size()
        );
        return guides;
    }

    // 단일 조회
    // 수정 사항 *) view를 여기서 호출하지 않기로 함
    public Optional<Guide> findById(Long id) throws CannotFoundGuide {
//        Guide guide = guideRepository.findById(id)
//                .orElseThrow(() -> new CannotFoundGuide("해당 공략이 존재하지 않습니다."));
//        guide.view();
//        return guide;

        Optional<Guide> guide = guideRepository.findById(id);
        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "guide",
                "findById",
                guide.isPresent() ? "SUCCESS" : "NOT_FOUND",
                "guideId=" + id
        );
        return guide;
    }
    public List<Guide> findByChampion(Champion champion) {
        List<Guide> guides = guideRepository.findByChampion(champion);
        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "guide",
                "findByChampion",
                "SUCCESS",
                "champion=" + champion.getNameKo() + ", count=" + guides.size()
        );
        return guides;
    }

    // 수정
    // 수정 사항 4) edit 메소드 안에서 해당 유저가 작성한 게 맞는지 판별 후에 수정 하기
    public Guide edit(Long id, GuideDto.GuideRequest dto, User author) throws CannotFoundGuide {
        Guide guide = guideRepository.findById(id)
                .orElseThrow(() -> new CannotFoundGuide("해당 공략이 존재하지 않습니다."));

        if (isAuthor(guide, author)) {
            guide.update(dto.getTitle(), dto.getContent());
        }
        else {
            log.warn(
                    ApiLogSupport.BUSINESS_FLOW,
                    "guide",
                    "edit",
                    "FORBIDDEN",
                    "guideId=" + id + ", requestUserId=" + author.getId() + ", authorId=" + guide.getAuthor().getId()
            );
            throw new EditFail("작성자만 수정 가능합니다.");
        }

        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "guide",
                "edit",
                "SUCCESS",
                "guideId=" + id + ", authorId=" + author.getId() + ", contentLength=" + contentLength(dto)
        );
        return guide;
    }

    // 삭제
    // 수정 사항 6) delete 메소드 안에서 해당 유저가 작성한 게 맞는지 판별 후에 삭제 하기
    public void deleteById(Long id, User author) throws CannotFoundGuide {
        Guide guide = findById(id)
                .orElseThrow(()-> new CannotFoundGuide("해당 공략 정보를 찾을 수 없습니다."));

        if (isAuthor(guide, author)) {
            guideRepository.deleteById(id);
        }
        else {
            log.warn(
                    ApiLogSupport.BUSINESS_FLOW,
                    "guide",
                    "delete",
                    "FORBIDDEN",
                    "guideId=" + id + ", requestUserId=" + author.getId() + ", authorId=" + guide.getAuthor().getId()
            );
            throw new DeleteFail("작성자만 삭제 가능합니다.");
        }

        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "guide",
                "delete",
                "SUCCESS",
                "guideId=" + id + ", authorId=" + author.getId()
        );
    }

    // 좋아요
    public void addLikeById(Long id) {
        guideRepository.findById(id).ifPresent(Guide::like);
        log.info(ApiLogSupport.BUSINESS_FLOW, "guide", "like", "SUCCESS", "guideId=" + id);
    }

    //싫어요
    public void addDislikeById(Long id) {
        guideRepository.findById(id).ifPresent(Guide::dislike);
        log.info(ApiLogSupport.BUSINESS_FLOW, "guide", "dislike", "SUCCESS", "guideId=" + id);
    }

    // 조회수 증가
    // 수정 사항 7) 찾을 때 이 메소드 호출해서 조회수 올리기 -> 수정 전에는 해당 메소드가 사용되지 않았음 -> 그냥 객체의 view() 메소드 이용하면 됨 JPA로 해당 객체의 값을 변경하면 알아서 반영됨
    public void addViews(Guide guide){
        guide.view();
        log.info(ApiLogSupport.BUSINESS_FLOW, "guide", "view", "SUCCESS", "guideId=" + guide.getId());
    }

    private boolean isAuthor(Guide guide, User author) {
        return guide.getAuthor().equals(author);
    }

    private int contentLength(GuideDto.GuideRequest dto) {
        return dto.getContent() == null ? 0 : dto.getContent().length();
    }
}
