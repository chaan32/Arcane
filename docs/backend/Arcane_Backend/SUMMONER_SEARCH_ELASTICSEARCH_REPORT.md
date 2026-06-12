# 소환사 검색 Elasticsearch 도입 결과

## 측정 조건

- 대상 데이터: `summoner` 테이블 38,202건
- Elasticsearch 인덱스: `arcane_summoners`
- 색인 결과: 38,202건 색인 완료
- 색인 시간: 5,611ms
- 측정 방식: 동일 키워드 기준 DB LIKE 검색과 Elasticsearch 검색을 각각 20회 반복 측정
- 응답 기준: 서비스 내부 검색 처리 시간 `elapsedMs`

## 성능 측정 결과

| 검색어 | 케이스 | DB LIKE 평균 | Elasticsearch 평균 | 개선율 |
| --- | --- | ---: | ---: | ---: |
| `hide on bush#kr` | `#` 포함 완성 검색 | 27ms | 15ms | 44.44% |
| `hide` | 소환사명 부분 검색 | 31ms | 10ms | 67.74% |
| `a` | 넓은 자동완성 검색 | 48ms | 9ms | 81.25% |

## `#` 검색 검증

| 검색어 | DB LIKE 결과 수 | Elasticsearch 결과 수 | 비고 |
| --- | ---: | ---: | --- |
| `hide on bush#kr` | 4건 | 4건 | `gameName contains` + `tagLine contains` |
| `hide on bush#` | 18건 | 18건 | 태그 입력 전 상태에서도 검색 가능 |

## AS-IS

소환사 자동완성 검색은 사용자가 검색창에 입력할 때마다 `/api/v1/summoner/contain/{keyword}` API를 호출하고, 백엔드에서는 MySQL `LIKE` 기반으로 `gameName` 또는 `gameName + tagLine`을 검색했습니다.

이 방식은 구현은 단순하지만 검색어가 짧을수록 후보 row가 많이 조회되고, 현재 구현에서는 DB에서 조회한 결과를 Java `List`로 받은 뒤 애플리케이션 레벨에서 `limit`을 적용하기 때문에 불필요한 row 로딩이 발생했습니다.

실제 38,202건의 소환사 데이터 기준, 넓은 자동완성 검색어인 `a`는 DB LIKE 평균 48ms, 최대 139ms가 걸렸고, `hide` 같은 부분 검색도 평균 31ms가 소요되었습니다. 검색창 자동완성은 입력마다 반복 호출되는 기능이므로 단일 요청의 지연이 작아 보여도 사용자가 빠르게 입력할수록 API 부하가 누적될 수 있는 구조였습니다.

또한 `hide on bush#kr`, `hide on bush#`처럼 `#`를 포함한 Riot ID 검색은 반드시 유지되어야 했기 때문에 단순히 검색 엔진을 붙이는 것만으로는 부족했고, 기존 DB 검색과 동일하게 `gameName contains` + `tagLine contains` 의미를 보장해야 했습니다.

## TO-BE

소환사 검색 전용 Elasticsearch 인덱스 `arcane_summoners`를 추가하고, `gameName`, `trimmedGameName`, `tagLine`, `displayName`, `searchKey`를 색인했습니다. 일반 소환사명 검색은 ngram 기반 검색을 사용하고, `#`가 포함된 Riot ID 검색은 기존 DB LIKE 의미와 동일하게 `gameName.keyword`와 `tagLine.keyword`에 대한 contains 조건으로 처리했습니다.

프론트가 기존에 사용하던 `/api/v1/summoner/contain/{keyword}` API는 유지하되 내부 구현만 Elasticsearch 우선 검색으로 변경했습니다. Elasticsearch 장애나 인덱스 문제가 발생하면 DB LIKE 검색으로 fallback되도록 구성해 기능 안정성을 유지했습니다.

또한 Elasticsearch 검색 결과는 다시 MySQL을 조회하지 않고 `_source`에서 바로 자동완성 DTO를 생성하도록 개선했습니다. 이를 통해 검색 경로에서 불필요한 DB round-trip을 제거했습니다.

최종 측정 결과, `hide on bush#kr` 검색은 27ms에서 15ms로 44.44% 개선되었고, 부분 검색 `hide`는 31ms에서 10ms로 67.74% 개선되었습니다. 특히 자동완성에서 흔히 발생하는 넓은 검색어 `a`는 48ms에서 9ms로 81.25% 개선되었습니다.

결과적으로 소환사 자동완성 검색은 기존 API 계약을 유지하면서도 검색 부하를 DB에서 Elasticsearch로 분리했고, `#` 포함 Riot ID 검색도 기존과 동일하게 동작하도록 개선했습니다.
