# 공략글 검색 Elasticsearch 도입 결과

## 측정 조건

- 측정 대상: 공략글 제목/본문 검색
- MySQL 데이터: 기존 공략 3개 + 벤치마크 공략 600개
- Elasticsearch 인덱스: `arcane_guide_posts`
- 검색어: `arcane-search-benchmark`
- 조회 개수: 상위 20개
- 반복 횟수: 100회
- 색인 결과: 603개 문서 bulk indexing, `errors=false`

## 성능 측정 결과

| 방식 | 측정 범위 | 평균 | 최소 | 최대 | 결과 수 |
| --- | --- | ---: | ---: | ---: | ---: |
| MySQL LIKE | `title/content LIKE '%keyword%'` + 정렬 + limit 20 + count | 4.005ms | 3.590ms | 7.138ms | 20건 / 총 600건 |
| Elasticsearch | `multi_match` 검색 | 0.960ms | 0ms | 27ms | 20건 |
| MySQL PK 조회 | ES 결과 id 20개 상세 조회 | 0.107ms | 0.055ms | 4.304ms | 20건 |
| Elasticsearch 경로 합산 | ES 검색 + MySQL id 상세 조회 | 1.067ms | - | - | 20건 |

## 개선 효과

- 기존 MySQL LIKE 평균: 4.005ms
- Elasticsearch 검색 경로 평균: 1.067ms
- 응답 시간 감소: 약 73.36%
- 속도 개선: 약 3.75배

```text
개선율 = (4.005ms - 1.067ms) / 4.005ms * 100 = 73.36%
속도 향상 = 4.005ms / 1.067ms = 3.75배
```

## AS-IS

공략글 검색은 MySQL에서 `LOWER(title) LIKE '%keyword%' OR LOWER(content) LIKE '%keyword%'` 방식으로 제목과 본문을 직접 스캔했습니다.

공략 본문은 Markdown 기반 `LONGTEXT`이기 때문에 데이터가 늘어날수록 DB가 본문 문자열을 계속 비교해야 합니다. 600개의 벤치마크 공략 기준으로도 같은 검색어를 100회 반복했을 때 평균 4.005ms가 소요되었습니다.

## TO-BE

공략글 전용 Elasticsearch 인덱스 `arcane_guide_posts`를 구성하고, `title`, `content`, `championNameKo`, `championNameEn`, `authorName`을 ngram analyzer로 색인했습니다.

검색은 Elasticsearch `multi_match`로 수행하고, 검색 결과의 guide id를 기준으로 MySQL에서 상세 데이터를 조회하도록 분리했습니다. 동일한 600개 벤치마크 공략 기준으로 Elasticsearch 검색은 평균 0.960ms, id 기반 MySQL 상세 조회는 평균 0.107ms가 소요되어 전체 검색 경로가 평균 1.067ms로 단축되었습니다.

그 결과 기존 MySQL LIKE 대비 약 73.36%의 검색 시간 감소와 약 3.75배의 속도 개선을 확인했습니다.

## 참고

이번 측정은 현재 8080 백엔드 서버가 실행 중이지 않아 API endpoint가 아니라 실행 중인 MySQL/Elasticsearch 컨테이너에서 직접 측정했습니다.

다만 쿼리 구조와 Elasticsearch mapping/search query는 현재 백엔드의 `GuideSearchService` 구현과 동일한 기준으로 맞췄습니다. API 서버가 실행 중일 때는 아래 endpoint로 애플리케이션 레벨 측정도 가능합니다.

```http
POST /api/v1/strategy/search/seed?count=600
POST /api/v1/strategy/search/elasticsearch/reindex
GET  /api/v1/strategy/search/benchmark?keyword=arcane-search-benchmark&limit=20&iterations=100
```
