# Frontend API Refactor and Performance Optimization

Date: 2026-06-04

## 목적

프론트엔드에 남아 있던 백엔드 API 주소 하드코딩을 제거하고, 반복 API 요청이 발생하던 화면을 캐시 기반 구조로 정리했다. 배포 환경에서는 `NEXT_PUBLIC_API_URL`만 바꾸면 동일 코드가 다른 백엔드 주소를 바라보도록 만들었다.

## 변경 내용

### 1. API 주소 중앙화

- `constants/api.ts`에 `API_URL`, `getApiUrl()`을 정리했다.
- `services/apiClient.ts`를 추가해 `apiFetch()`, `apiJson()`, `ApiRequestError`를 공통화했다.
- `app/champions/page.tsx`, `app/ranking/page.tsx`, `app/summoner/[name]/page.tsx`의 `http://localhost:8080` 직접 호출을 제거했다.
- 인증/마이페이지/온보딩/커뮤니티 서비스도 공통 API 클라이언트를 사용하도록 맞췄다.

효과:
- API 서버 주소 변경 시 파일 하나 또는 환경변수 하나만 수정하면 된다.
- 요청 실패 처리 방식이 서비스별로 갈라지는 문제를 줄였다.

### 2. React Query 캐시 적용

- 챔피언 목록 페이지의 라인별 티어, 전체 챔피언 목록 조회를 React Query로 전환했다.
- 랭킹 페이지의 티어/페이지별 랭킹 조회를 React Query로 전환했다.
- 챔피언 상세 훅 `useChampionDetail`을 React Query 기반으로 바꿨다.
- 챔피언 빌드 탭의 스펠/룬 훅도 React Query 캐시를 사용하도록 바꿨다.
- 전역 QueryClient 기본값을 조정했다.
  - `staleTime`: 1분
  - `gcTime`: 10분
  - `refetchOnWindowFocus`: false
  - `retry`: 1

효과:
- 같은 챔피언, 같은 랭킹 페이지, 같은 스펠/룬 ID를 다시 볼 때 중복 네트워크 요청이 줄어든다.
- 탭 전환, 페이지 재방문, 상세 페이지 이동 후 뒤로 가기에서 캐시된 응답을 재사용할 수 있다.
- 브라우저 포커스 복귀만으로 불필요한 자동 재요청이 발생하는 일을 줄였다.

### 3. 챔피언 목록 페이지 요청 최적화

- 전체 포지션 조회에서 이미 받은 라인별 티어 데이터를 개별 라인 탭에서도 재사용하게 했다.
- `ChampionLaneCell`을 `memo`로 분리하고, 셀마다 `useRouter()`를 호출하던 구조를 부모의 단일 네비게이션 핸들러로 정리했다.

효과:
- 기본 진입 후 `전체 -> 탑/정글/미드/원딜/서폿` 전환 시 이미 받은 라인 데이터는 재요청하지 않는다.
- 많은 셀을 렌더링하는 티어표에서 불필요한 훅 호출과 렌더링 비용을 줄였다.

### 4. DDragon 데이터 캐시

- `services/dataDragonApi.ts`를 추가했다.
- 챔피언 목록 JSON, 아이템 목록 JSON, 챔피언 ID 이미지 맵을 모듈 레벨 Promise 캐시로 재사용한다.
- 공략 작성 챔피언 피커, 아이템 피커, 패치노트 챔피언 피커, 소환사 상세의 챔피언 이미지 맵 조회를 같은 서비스로 연결했다.

효과:
- 같은 세션에서 DDragon 챔피언/아이템 목록 JSON을 화면마다 반복 요청하지 않는다.
- 외부 정적 데이터 요청이 줄어 초기 상호작용 후 화면 이동이 가벼워진다.

### 5. 소환사 상세 보조 데이터 요청량 감소

- 전적 더보기 후 스펠/룬 데이터를 다시 가져올 때 기존에 이미 로드한 ID는 제외하고 새 ID만 요청하도록 변경했다.
- 새로 받은 데이터는 기존 `spell`, `rune` 맵에 병합한다.

효과:
- 전적 페이지를 추가 로드할수록 같은 스펠/룬 ID를 반복 호출하던 비용이 줄어든다.
- 전적 더보기 시 백엔드 보조 데이터 API 호출 수가 실제 필요한 ID 수만큼으로 제한된다.

### 6. 소환사 상세 페이지 1차 구조 분리

- `app/summoner/[name]/_lib/summonerFormatters.ts`를 추가해 시간, 경기 시간, 티어 표기, 참가자 티어 배지 계산을 순수 함수로 분리했다.
- `app/summoner/[name]/_components/SummonerStatusViews.tsx`를 추가해 로딩, 찾을 수 없음, API 제한 상태 화면을 분리했다.
- `app/summoner/[name]/_components/MatchListControls.tsx`를 추가해 전적 필터 탭과 더보기 버튼을 독립 컴포넌트로 분리하고 `memo`를 적용했다.
- 전적 더보기 핸들러 `loadMoreMatches`를 `useCallback`으로 고정해 자식 컴포넌트에 전달되는 함수 참조가 불필요하게 바뀌지 않도록 했다.
- `app/summoner/[name]/page.tsx`는 3,485줄로 줄었고, 새 분리 파일 3개에 360줄을 이동했다.

효과:
- 순수 포맷터는 UI 렌더링 없이 단위 테스트를 붙일 수 있는 형태가 됐다.
- 상태 화면과 전적 컨트롤은 부모 페이지의 데이터 상태가 바뀌어도 필요한 prop이 바뀌지 않으면 재렌더링 비용을 줄일 수 있다.
- 대형 페이지에서 변경 이유가 다른 코드가 섞이는 문제를 줄여 이후 `MatchCard`, 전적 상세 테이블, 요약 패널 분리를 더 안전하게 진행할 수 있다.

### 7. 소환사 상세 페이지 2차 전적 카드 분리

- `app/summoner/[name]/_components/MatchCard.tsx`를 추가해 전적 카드 요약, 참가자 미니 리스트, 확장 토글을 부모 페이지에서 분리했다.
- `app/summoner/[name]/_components/MatchDetailPanel.tsx`를 추가해 확장 상세 패널, 팀 테이블, 참가자 행을 `memo` 기반 컴포넌트로 분리했다.
- AI 점수 값, AI 점수 순위, 순위 라벨 계산을 `summonerFormatters.ts`의 순수 함수로 이동했다.
- 소환사 이동 핸들러와 전적 확장 토글 핸들러를 `useCallback`으로 고정해 `MatchCard`에 전달되는 함수 참조가 불필요하게 바뀌지 않도록 했다.
- `app/summoner/[name]/page.tsx`는 2,664줄까지 줄었고, 전적 카드/상세 패널 757줄을 독립 컴포넌트로 이동했다.

효과:
- 부모 페이지는 데이터 조회, 필터링, 날짜 구분, 섹션 조립 역할에 집중하고 전적 카드 렌더링 책임을 내려놓았다.
- 전적 상세 패널은 펼쳐진 카드에서만 렌더링되고, 참가자 행 단위가 `memo` 경계를 갖게 되어 이후 props 안정화와 이미지 최적화를 적용하기 쉬워졌다.
- 팀 테이블 JSX 중복을 제거해 블루팀/레드팀 표시, AI 점수, KDA, 피해량, CS, 와드, 아이템 표시 변경을 한 곳에서 수정할 수 있다.

### 8. 소환사 상세 타입 정규화

- `app/summoner/[name]/_types/summonerTypes.ts`를 추가해 `Summoner`, `Profile`, `Mastery`, `Match`, `MatchParticipant`, `MatchPlayerIndex` 타입을 한 곳에 모았다.
- `app/summoner/[name]/page.tsx`, `MatchCard.tsx`, `MatchDetailPanel.tsx`가 같은 `Match` 계약을 import하도록 변경했다.
- `MatchCard.tsx`, `MatchDetailPanel.tsx`에 있던 임시 `MatchDetailParticipant`, `MatchDetailPanelMatch`, `MatchCardMatch` 타입을 제거했다.
- `app/summoner/[name]/page.tsx`의 대형 중복 타입 선언을 제거해 1,676줄까지 줄였다.

효과:
- 백엔드 전적 응답 필드가 바뀌면 소환사 상세 타입 파일 한 곳에서 우선 확인하면 된다.
- 페이지와 전적 카드/상세 패널 사이의 prop 타입이 구조적으로만 맞는 상태가 아니라 명시적으로 같은 타입 계약을 공유하게 됐다.
- 다음 섹션 분리나 `next/image` 전환 시 타입 중복 때문에 생기는 수정 누락 가능성을 줄였다.

### 9. 소환사 전적 이미지 1차 최적화

- `next.config.ts`의 `images.remotePatterns`에 `raw.communitydragon.org`를 추가해 다음 단계의 랭크/숙련도 이미지 최적화 준비를 마쳤다.
- `app/summoner/[name]/_lib/summonerImageUrls.ts`를 추가해 챔피언, 아이템, 스펠, 룬 이미지 URL 생성과 상대 경로 보정을 공통화했다.
- `app/summoner/[name]/_components/MatchImage.tsx`를 추가해 `next/image`와 고정 크기 placeholder를 공통으로 사용하게 했다.
- `MatchCard.tsx`, `MatchDetailPanel.tsx`, `SummonerStatusViews.tsx`의 `<img>`를 `next/image` 기반 렌더링으로 전환했다.

효과:
- 전적 카드와 전적 상세 패널에서 반복 노출되는 챔피언/아이템/스펠/룬 이미지가 Next 이미지 최적화 경로를 타게 됐다.
- 스펠/룬 데이터가 아직 로드되지 않은 순간에는 깨진 이미지 대신 같은 크기의 placeholder를 렌더링해 레이아웃 흔들림을 줄였다.
- `MatchCard.tsx`, `MatchDetailPanel.tsx`, `SummonerStatusViews.tsx`의 `<img>` lint 경고를 제거했다.

### 10. 소환사 상세 본문 이미지 최적화

- `app/summoner/[name]/page.tsx`의 프로필 아이콘, 랭크 엠블럼, 언랭크 안내 이미지, 포지션 아이콘, 숙련도 챔피언, 요약 챔피언 이미지를 `next/image`로 전환했다.
- 프로필, 랭크, 숙련도 이미지는 `FallbackImage`를 통해 후보 URL을 순차적으로 시도하고 실패 시 같은 크기의 placeholder를 렌더링하도록 했다.
- `profile.profileUrl`처럼 출처가 불명확한 URL은 `next.config.ts`에 허용된 이미지 출처 또는 로컬 경로일 때만 사용하도록 제한했다.

효과:
- 소환사 상세 페이지 본문에서 남아 있던 `<img>` lint 경고를 제거했다.
- 랭크/숙련도 이미지 실패 시 직접 DOM `src`를 수정하던 방식 대신 React state 기반 fallback으로 정리했다.
- 이미지 실패 또는 지연 로딩 상황에서 고정 크기 placeholder가 유지되어 레이아웃 흔들림 가능성을 줄였다.

### 11. 남은 화면 이미지 경고 제거

- `components/common/ExternalImage.tsx`를 추가해 OAuth 프로필, 검색 드롭다운 프로필, 가이드 커버처럼 출처가 동적으로 바뀔 수 있는 이미지를 `next/image` 기반으로 렌더링하도록 했다.
- DDragon/CommunityDragon 이미지는 Next 기본 이미지 최적화를 유지하고, 그 외 외부 프로필 URL만 passthrough loader를 사용해 기존 표시 동작을 보존했다.
- 챔피언 상세의 프로필, 스킬, 매치업, 아이템, 룬, 소환사 주문 이미지를 `next/image`로 전환했다.
- 챔피언 목록, 가이드 목록/상세, 내 정보, 패치노트, 랭킹, 공략 챔피언 선택기, 검색 드롭다운의 남은 `<img>` 사용을 제거했다.
- 가이드 상세 채팅 연결 effect는 메시지 배열을 ref로 분리해 소켓 재연결을 유발하지 않으면서 `react-hooks/exhaustive-deps` 경고를 제거했다.
- `MarkdownPreview`가 문자열로 생성하는 가이드 본문 이미지는 구조 변경 없이 `loading="lazy"`와 `decoding="async"`를 추가했다.

효과:
- 프로젝트 전체 `next lint` 기준 `<img>` 이미지 경고와 hook dependency 경고가 모두 제거됐다.
- 고정 크기 `width`, `height`, `sizes`를 명시해 이미지 로딩 전후의 레이아웃 흔들림 가능성을 줄였다.
- 반복 노출되는 챔피언/아이템/스펠/룬/프로필 이미지가 Next 이미지 최적화 경로를 사용하게 되어 브라우저에 전달되는 이미지 처리와 lazy loading이 개선됐다.
- 외부 프로필 URL 실패 시 깨진 이미지나 직접 DOM style 변경 대신 같은 위치의 fallback UI로 전환된다.
- 마크다운 본문 이미지는 화면 밖에 있을 때 초기 로딩 부담을 줄이고 디코딩이 렌더링을 덜 막도록 했다.

## 검증 결과

- `npm run lint`: 통과
- `npm run build`: 통과

남은 경고:
- 없음. `next lint` 기준 경고와 오류가 모두 제거됐다.

## 다음 개선 후보

- `app/summoner/[name]/page.tsx`에 남은 프로필 카드, 랭크 카드, 포지션 분포, 요약 패널을 섹션 컴포넌트로 단계적으로 분리한다.
- 챔피언 상세 페이지도 `ChampionProfile`, `SummonerSpells`, `ItemBuild`, `RuneTabs`, `ChampionMatchups`를 더 작은 표시 컴포넌트로 나누고 공통 이미지 helper를 정리한다.
- 게시판/패치노트/랭킹의 로딩, 에러, 빈 목록 상태 UI를 공통 상태 컴포넌트로 분리한다.
- 검색 드롭다운과 챔피언 선택기의 리스트 아이템을 `memo`와 stable handler 기준으로 정리해 입력 중 렌더링 비용을 더 줄인다.
