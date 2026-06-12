package com.arcane.Arcane.riot.summoner.service;

import com.arcane.Arcane.common.Exception.Normal.CannotFoundChampion;
import com.arcane.Arcane.common.Exception.RiotAPI.CannotFoundSummoner;
import com.arcane.Arcane.riot.Data.Champion.Champion;
import com.arcane.Arcane.riot.Data.Champion.repository.ChampionRepository;
import com.arcane.Arcane.riot.Data.Champion.ChampionService;
import com.arcane.Arcane.riot.Match.domain.Match;
import com.arcane.Arcane.riot.Match.domain.MatchParticipant;
import com.arcane.Arcane.riot.Match.dto.InfoDto;
import com.arcane.Arcane.riot.Match.dto.MatchDto;
import com.arcane.Arcane.riot.Match.dto.ParticipantDto;
import com.arcane.Arcane.riot.Match.dto.v3.MatchInfoResDto;
import com.arcane.Arcane.riot.Match.dto.v3.MetaDataResDto;
import com.arcane.Arcane.riot.Match.dto.v3.MyDataResDto;
import com.arcane.Arcane.riot.Match.dto.v3.ParticipantsResDto;
import com.arcane.Arcane.riot.Match.mongo.MatchRawMongoService;
import com.arcane.Arcane.riot.Match.service.MatchService;
import com.arcane.Arcane.riot.RiotInform.dto.MasteryDto;
import com.arcane.Arcane.riot.RiotInform.dto.ProfileResDto;
import com.arcane.Arcane.riot.RiotInform.dto.RiotAccountDto;
import com.arcane.Arcane.riot.RiotInform.service.RiotApiService;
import com.arcane.Arcane.riot.summoner.domain.Summoner;
import com.arcane.Arcane.riot.summoner.dto.SummonerDto;
import com.arcane.Arcane.riot.summoner.dto.SummonerKeywordResDto;
import com.arcane.Arcane.riot.summoner.dto.SummonerTierResDto;
import com.arcane.Arcane.riot.summoner.repository.SummonerRepository;
import com.arcane.Arcane.model.service.PythonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummonerService {
    private final SummonerRepository summonerRepository;
    private final RiotApiService riotApiService;
    private final ChampionRepository championRepository;
    private final MatchService matchService;
    private final ChampionService championService;
    private final PythonService pythonService;
    private final SummonerWriteService summonerWriteService;
    private final MatchRawMongoService matchRawMongoService;
    private final ConcurrentMap<String, Object> summonerCreationLocks = new ConcurrentHashMap<>();




    /** 페이지네이션으로 통합 전적 조회 메소드 👊
     *
     * @param gameName
     * @param tagLine
     * @return
     * @throws CannotFoundSummoner
     */
    public Queue<MatchInfoResDto> getSummonerMatches(String gameName, String tagLine, Boolean refresh, int page) throws CannotFoundSummoner {
        // 매치를 저장할 큐
        Queue<MatchInfoResDto> matchInfoResDtos = new LinkedList<>();

        // 닉변해도 여기서는 무조건 나올 것
        Optional<Summoner> s = getOptionalSummonerObj(gameName, tagLine);
        String puuid = getPuuid(gameName, tagLine);

        // String 배열로 matchId를 가져 왔어 (20개)
        String[] summonerMatchesId = riotApiService.getSummonerMatches(puuid, page);

        // step 1) : matchId를 통해서 Match_info 객체를 받아오기  => 있을 수도 있고 없을 수도 있음
        for (String matchId : summonerMatchesId) { // match id 가져와서 반복문 돌림

            // matchId로 Match 정보가 있는지 먼저 파악
            Optional<Match> match = matchService.getMatchByMatchId(matchId);

            // step 2-1) : match가 있다면 바로 matchDataDto 구성하기
            if (match.isPresent()) {
                addPresentedMatch(matchInfoResDtos, match.get(), puuid);
            }
            // step 2-2) : match가 없다면 만들고 matchDataDto 구성하기
            else {
                addEmptyMatch(matchInfoResDtos, matchId, puuid);
            }
        }

        if (Boolean.TRUE.equals(refresh)) {
            markSummonerRefreshed(s, puuid);
        }

        return matchInfoResDtos;
    }
    /** 키워드를 통해서 소환사 빠르게 찾는거  👊
     * @param keyword
     * @return
     */
    public List<SummonerKeywordResDto> findSummonerByKeyword(String keyword){
        List<Summoner> result;

        if (keyword.contains("#")){ // 태그 라인까지 같이 검색을 한 경우
            // # 기준으로 분리
            String[] parts = keyword.split("#", 2);

            String gameName = parts[0].trim();
            String tagLine = parts.length > 1 ? parts[1].trim() : "";

            if (tagLine.isBlank()){
                result = summonerRepository.findByGameNameContainingIgnoreCase(gameName);
            }
            else {
                result = summonerRepository.findByGameNameContainingIgnoreCaseAndTagLineContainingIgnoreCase(gameName, tagLine);
            }
        }
        else {
            result = summonerRepository.findByGameNameContainingIgnoreCase(keyword);
        }
        return result.stream()
                .map(SummonerKeywordResDto::of)
                .toList();
    }
    /** 뭘 하든 일단 반드시 PUUID를 반환해주는 메소드 👊 (캐시 이용)
     *
     * @param gameName
     * @param tagLine
     * @return
     * @throws CannotFoundSummoner
     */
    public String getPuuid(String gameName, String tagLine) throws CannotFoundSummoner {
        // 여기서는 DB에 있든 없든 무조건 PUUID를 반환해줌
        // 1) DB에 있는지 조회 함
        Optional<Summoner> optSummoner = summonerRepository
                .findSummonerByTrimmedGameNameAndTagLine(gameName.replace(" ", ""), tagLine);
        // 2) 있으면 걍 찾아서 나감
        if (optSummoner.isPresent()){
            return optSummoner.get().getPuuid();
        }
        // 3) 없으면 riotAPI 호출해서 보내줘
        return riotApiService.getSummonerPuuid(gameName, tagLine);
    }
    /** gameName, tagLine을 통해서 숙련도 리스트를 반환하는 메소드 👊
     *
     * @param gameName
     * @param tagLine
     * @return
     * @throws CannotFoundSummoner
     */
    public List<MasteryDto> getSummonerMasteryInfo(String gameName, String tagLine) throws CannotFoundSummoner {
        String puuid = getPuuid(gameName, tagLine);

        List<MasteryDto> masteryInfo = riotApiService.getMasteryInfo(puuid);
        setChampionName(masteryInfo);

        return masteryInfo;
    }
    /** gameName, tagLine을 통해서 티어 정보를 반한 하는 메소드 👊
     *
     * @param gameName
     * @param tagLine
     * @param refresh
     * @return
     * @throws CannotFoundSummoner
     */
    public SummonerTierResDto getSummonerTierInfo(String gameName, String tagLine, Boolean refresh) throws CannotFoundSummoner {

        Optional<Summoner> optionalSummoner = getOptionalSummonerObj(gameName, tagLine);

        if (optionalSummoner.isPresent()
                && !Boolean.TRUE.equals(refresh)
                && hasTierInfo(optionalSummoner.get())) {
            // 1) 이미 DB에 존재하는 경우
            // 2) 갱신이 아닌 경우
            // 3) 다만 참가자 저장 과정에서 만들어진 불완전 row가 아닌 경우 (Tier가 null로 존재하는 경우엔 얘는 불완전)
            return SummonerTierResDto.of(optionalSummoner.get());
        }

        // 전적 갱신을 했거나, DB에 없거나, DB에 Summoner만 있고 티어 정보가 없는 사람 -> riot에서 정보를 받아서 리턴 해줘야 함
        // 1차로 puuid 얻기;
        String puuid = getPuuid(gameName, tagLine);
        SummonerDto tierInfo = riotApiService.getSummonerTierInfo(
                SummonerDto.setInform(gameName, tagLine, puuid)
        );
        // 얻은 puuid로 새롭게 데이터 저장 (갱신을 하든, 뭘 하든 일단 새롭게 나타내줘야 하기 때문)
        Optional<Summoner> summoner = summonerRepository.findSummonerByPuuid(puuid);
        Summoner savedS = saveOrUpateSummoner(tierInfo, summoner);
        return SummonerTierResDto.of(savedS);
    }
    /** Summoner 정보 CRUD 👊
     *
     * @param puuid
     * @return
     */
    public Optional<Summoner> getSummonerByPuuid(String puuid){
        return summonerRepository.findSummonerByPuuid(puuid);
    }
    public Summoner saveSummoner(RiotAccountDto riotAccountDto){
        return saveSummonerIfAbsent(new Summoner(riotAccountDto));
    }
    public void updateSummoners(List<Summoner> summoners){
        summonerRepository.saveAll(summoners);
    }

    /** 프로필 정보 가져오기 (iconId, level, iconurl) 👊
     *  업데이트를 해야만 새로운 거 가져다줌
     * @param gameName
     * @param tagLine
     * @param refresh
     * @return
     * @throws CannotFoundSummoner
     */
    public ProfileResDto getProfile(String gameName, String tagLine, Boolean refresh) throws CannotFoundSummoner {

        Summoner summoner = resolveSummoner(gameName.replace(" ", ""), tagLine);

        if (summoner.getIconId() != null && summoner.getLevel() != null && !refresh) {
            return ProfileResDto.of(summoner);
        }

        ProfileResDto profileInfo = riotApiService.getProfileInfo(summoner.getPuuid());
        summoner.updateProfile(profileInfo);
        summoner.markRefreshed();
        Summoner saved = summonerWriteService.saveAndFlush(summoner);
        return ProfileResDto.of(saved);
    }


    // ---------------------------- Helper --------------------------------------------
    /** gameName, tagLine으로 Optional<Summoner>를 반환하는 메소드
     *
     * @param gameName
     * @param tagLine
     * @return
     */
    @Transactional(readOnly = true)
    protected Optional<Summoner> getOptionalSummonerObj(String gameName, String tagLine) {
        return summonerRepository.findSummonerByTrimmedGameNameAndTagLine(gameName.replace(" ", ""), tagLine);
    }
    private Summoner findOrCreateSummoner(ParticipantDto participantDto) {
        return summonerRepository.findSummonerByPuuid(participantDto.getPuuid())
                .orElseGet(() -> saveSummonerIfAbsent(new Summoner(participantDto)));
    }
    private Summoner saveSummonerIfAbsent(Summoner newSummoner) {
        String puuid = newSummoner.getPuuid();
        Object lock = summonerCreationLocks.computeIfAbsent(puuid, ignored -> new Object());

        synchronized (lock) {
            return summonerRepository.findSummonerByPuuid(puuid)
                    .orElseGet(() -> summonerWriteService.insertIgnoreAndFind(newSummoner));
        }
    }
    private void markSummonerRefreshed(Optional<Summoner> summoner, String puuid) {
        Summoner target = summoner
                .or(() -> summonerRepository.findSummonerByPuuid(puuid))
                .orElse(null);

        if (target == null) {
            return;
        }

        target.markRefreshed();
        summonerWriteService.saveAndFlush(target);
    }
    private boolean isClassicGame(Match match){
        return match.getGameMode().equals("CLASSIC");
    }
    private boolean isClassicGame(MatchDto match){
        return match != null
                && match.getInfo() != null
                && "CLASSIC".equals(match.getInfo().getGameMode());
    }
    /** Summoner 객체를 저장과 업데이트를 하는 메소드
     *
     * @param dto
     * @param summoner
     * @return
     */
    private Summoner saveOrUpateSummoner(SummonerDto dto, Optional<Summoner> summoner){
        if (summoner.isPresent()){
            // 기존에 puuid가 같은 소환사가 있다면 정보를 업데이트
            Summoner target = summoner.get();
            // 닉네임이 바뀌었을 수 있으므로 updateTier 내부에서
            // gameName, tagLine, trimmedGameName을 모두 갱신
            target.updateTier(dto);
            target.markRefreshed();
            return summonerWriteService.saveAndFlush(target);
        }
        else {
            Summoner target = Summoner.update(dto);
            return saveSummonerIfAbsent(target);
        }
    }
    private boolean hasTierInfo(Summoner summoner) {
        return hasText(summoner.getSoloRankTier())
                || summoner.getSoloRankLP() != null
                || summoner.getSoloRankWin() != null
                || summoner.getSoloRankDefeat() != null
                || hasText(summoner.getFlexRankTier())
                || summoner.getFlexRankLP() != null
                || summoner.getFlexRankWin() != null
                || summoner.getFlexRankDefeat() != null;
    }
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
    /** 숙련도 할 때 세팅하는 메소드
     *
     * @param dtos
     * @return
     */
    private List<MasteryDto> setChampionName(List<MasteryDto> dtos) {
        for (MasteryDto dto : dtos) {
            // 1. championId로 Champion 정보를 조회합니다.
            Optional<Champion> championOptional = championRepository.findById(dto.getChampionId());

            // 2. ifPresent를 사용하여 Optional 객체가 비어있지 않은 경우에만 내부 로직을 실행합니다.
            //    이렇게 하면 데이터가 없을 때 .get()으로 인한 오류를 원천적으로 방지할 수 있습니다.
            championOptional.ifPresent(champion -> {
                // 3. (핵심) 조회한 champion 객체의 이름을 MasteryDto에 설정합니다.
                dto.setChampionName(champion.getNameKo());
            });

            // 4. (선택사항) 만약 데이터가 없는 경우를 확인하고 싶다면 아래와 같이 처리할 수 있습니다.
            if (championOptional.isEmpty()) {
                log.warn("DB에서 Champion 정보를 찾을 수 없습니다. champId: {}", dto.getChampionId());
            }
        }
        return dtos;
    }
    /** 참여자 정보 넣기
     *
     * @param participants
     * @return
     */
    private ParticipantsResDto addParticipant(List<MatchParticipant> participants) {

        ParticipantsResDto participantsResDto = new ParticipantsResDto();
        Long championId0 = participants.get(0).getChampionId();
        Champion champion0 = championService.getChampionById(championId0).orElseThrow(()->new CannotFoundChampion(championId0+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer0(participants.get(0),champion0);

        Long championId1 = participants.get(1).getChampionId();
        Champion champion1 = championService.getChampionById(championId1).orElseThrow(()->new CannotFoundChampion(championId1+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer1(participants.get(1),champion1);

        Long championId2 = participants.get(2).getChampionId();
        Champion champion2 = championService.getChampionById(championId2).orElseThrow(()->new CannotFoundChampion(championId2+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer2(participants.get(2),champion2);

        Long championId3 = participants.get(3).getChampionId();
        Champion champion3 = championService.getChampionById(championId3).orElseThrow(()->new CannotFoundChampion(championId3+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer3(participants.get(3),champion3);

        Long championId4 = participants.get(4).getChampionId();
        Champion champion4 = championService.getChampionById(championId4).orElseThrow(()->new CannotFoundChampion(championId4+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer4(participants.get(4),champion4);



        Long championId5 = participants.get(5).getChampionId();
        Champion champion5 = championService.getChampionById(championId5).orElseThrow(()->new CannotFoundChampion(championId5+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer5(participants.get(5),champion5);

        Long championId6 = participants.get(6).getChampionId();
        Champion champion6 = championService.getChampionById(championId6).orElseThrow(()->new CannotFoundChampion(championId6+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer6(participants.get(6),champion6);

        Long championId7 = participants.get(7).getChampionId();
        Champion champion7 = championService.getChampionById(championId7).orElseThrow(()->new CannotFoundChampion(championId7+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer7(participants.get(7),champion7);

        Long championId8 = participants.get(8).getChampionId();
        Champion champion8 = championService.getChampionById(championId8).orElseThrow(()->new CannotFoundChampion(championId8+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer8(participants.get(8),champion8);

        Long championId9 = participants.get(9).getChampionId();
        Champion champion9 = championService.getChampionById(championId9).orElseThrow(()->new CannotFoundChampion(championId9+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer9(participants.get(9),champion9);

        return participantsResDto;
    }

    /** 우리 DB에 없는 데이터를 가지고 DB에 저장
     *
     * @param matchInfoResDtos
     * @param matchId
     * @param puuid
     */
    private void addEmptyMatch(Queue<MatchInfoResDto> matchInfoResDtos, String matchId, String puuid){
        // step 3-2) : riot API 요청을 통해서 해당 matchId의 값을 받기
        MatchDto matchDto = riotApiService.getMatchInfo(matchId);

        matchRawMongoService.saveParticipants(matchId, matchDto);
        // match에서 classic이 아니면 넘어가기
        if (!this.isClassicGame(matchDto)) {
            return;
        }
        // step 4-2) : matchDto 속의 infoDto를 통해서 participantDto를 통해, Summoner에 저장이 되어 있는 Summoner인지 체크하기
        InfoDto infoDto = matchDto.getInfo();
        List<ParticipantDto> participantsDtoFromApi = infoDto.getParticipants(); // 참가자 정보 찾아 왔음


        //여기서 queueID 넣어야 함
        // 기본 정보 저장
        Match newMatch = Match.builder()
                .matchId(matchId) // Riot API에서 받은 matchId
                .gameMode(infoDto.getGameMode())
                .gameVersion(infoDto.getGameVersion())
                .gameDuration(infoDto.getGameDuration())
                .gameCreation(infoDto.getGameCreation())
                .gameEndTimestamp(infoDto.getGameEndTimestamp())
                .queueId(infoDto.getQueueId())
                .build();

        List<MatchParticipant> matchParticipants = new ArrayList<>();
        for (ParticipantDto participantDto : participantsDtoFromApi) {
            // step 5-2) : puuid로 Summoner를 찾거나, 없으면 새로 저장
            Summoner summoner = findOrCreateSummoner(participantDto);

            MatchParticipant participant = MatchParticipant.builder()
                    .match(newMatch)
                    .summoner(summoner)
                    .win(participantDto.getWin())
                    .championId(participantDto.getChampionId())
                    .champLevel(participantDto.getChampLevel())
                    .teamPosition(participantDto.getTeamPosition())
                    .item0(participantDto.getItem0())
                    .item1(participantDto.getItem1())
                    .item2(participantDto.getItem2())
                    .item3(participantDto.getItem3())
                    .item4(participantDto.getItem4())
                    .item5(participantDto.getItem5())
                    .item6(participantDto.getItem6())
                    .perks(participantDto.getPerks())
                    .kda(participantDto.getKda())
                    .kills(participantDto.getKills())
                    .assists(participantDto.getAssists())
                    .deaths(participantDto.getDeaths())
                    .totalMinionKills(participantDto.getTotalMinionsKilled() + participantDto.getNeutralMinionsKilled())
                    .totalDamageTaken(participantDto.getTotalDamageTaken())
                    .totalDamageDealtToChampions(participantDto.getTotalDamageDealtToChampions())
                    .doubleKills(participantDto.getDoubleKills())
                    .tripleKills(participantDto.getTripleKills())
                    .quadraKills(participantDto.getQuadraKills())
                    .pentaKills(participantDto.getPentaKills())
                    .summoner1Casts(participantDto.getSummoner1Casts())
                    .summoner1Id(participantDto.getSummoner1Id())
                    .summoner2Id(participantDto.getSummoner2Id())
                    .summoner2Casts(participantDto.getSummoner2Casts())
                    .spell1Casts(participantDto.getSpell1Casts())
                    .spell2Casts(participantDto.getSpell2Casts())
                    .spell3Casts(participantDto.getSpell3Casts())
                    .spell4Casts(participantDto.getSpell4Casts())
                    .wardKilled(participantDto.getWardKilled())
                    .wardPlaced(participantDto.getWardPlaced())
                    .visionWardsBoughtInGame(participantDto.getVisionWardsBoughtInGame())
                    .visionScore(participantDto.getVisionScore())
                    .build();
            matchParticipants.add(participant);
        }

        List<Integer> predictedScores = pythonService.predictScores(infoDto, participantsDtoFromApi);
        for (int index = 0; index < matchParticipants.size(); index++) {
            Integer predictedScore = predictedScores.get(index);
            MatchParticipant participant = matchParticipants.get(index);
            participant.setTeamLuckScore(predictedScore);
            participant.setOurScore(predictedScore);
        }

        newMatch.setParticipants(matchParticipants);

        ParticipantsResDto participantsResDto = addParticipant(matchParticipants);

        MetaDataResDto metaDataResDto = MetaDataResDto.of(newMatch);
        MatchParticipant myDataByPuuid = newMatch.getMyDataByPuuid(puuid);
        Long championId = myDataByPuuid.getChampionId();
        Champion champion = championService.getChampionById(championId).orElseThrow(()->new CannotFoundChampion(championId+"에 해당하는 챔피언이 없습니다."));
        MyDataResDto myDataResDto = MyDataResDto.of(myDataByPuuid,champion);
        myDataResDto.setPerks(myDataByPuuid);

        matchInfoResDtos.add(MatchInfoResDto.of(metaDataResDto, myDataResDto, participantsResDto));
        matchService.saveIfAbsent(newMatch);
    }
    private void addPresentedMatch(Queue<MatchInfoResDto> matchInfoResDtos, Match match, String puuid){

        // match에서 classic이 아니면 넘어가기
        if (!this.isClassicGame(match) ){
            return;
        }
        // 참가자들 정보
        List<MatchParticipant> participants = match.getParticipants();

        // 참가자들 전체 객체에 넣기 (Response 용)
        ParticipantsResDto participantsResDto = addParticipant(participants);

        // 메타 데이터 넣기
        MetaDataResDto metaDataResDto = MetaDataResDto.of(match);

        // 내 정보 저장
        MatchParticipant myDataByPuuid = match.getMyDataByPuuid(puuid);
        Long championId = myDataByPuuid.getChampionId();
        Champion champion = championService.getChampionById(championId).orElseThrow(()->new CannotFoundChampion(championId+"에 해당하는 챔피언이 없습니다."));
        MyDataResDto myDataResDto = MyDataResDto.of(myDataByPuuid,champion);
        myDataResDto.setPerks(myDataByPuuid);

        // step 4-2) : MatchDataDto 객체를 List에 넣어준다
        MatchInfoResDto dto = MatchInfoResDto.of(metaDataResDto, myDataResDto, participantsResDto);
        matchInfoResDtos.add(dto);
    }
    private Summoner resolveSummoner(String gameName, String tagLine) throws CannotFoundSummoner {
        // 1) DB에서 gameName, tagLIne으로 먼저 찾음
        Optional<Summoner> optionalSummonerObj = getOptionalSummonerObj(gameName, tagLine);

        // 2) 있으면 호출 ㄴㄴ
        if (optionalSummonerObj.isPresent()){
            return optionalSummonerObj.get();
        }

        // 3) 없으면 호출 (puuid 얻기)
        RiotAccountDto account = riotApiService.getSummonerInfo(gameName, tagLine);

        // 4) 저장
        return saveSummonerIfAbsent(new Summoner(account));
    }
}
