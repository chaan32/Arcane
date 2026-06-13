# ARCANE : League of Legends Analytics Platform
<p align="center"> <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21"> <img src="https://img.shields.io/badge/Spring_Boot-3.4.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot 3.4.1"> <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL"> <img src="https://img.shields.io/badge/Redis-FF4438?style=for-the-badge&logo=redis&logoColor=white" alt="Redis"> <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker"> <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" alt="Swagger"> </p>

---

### ARCANE은 라이엇 게임즈의 빅데이터를 실시간으로 수집하고 분석하여, 플레이어에게 단순한 전적 검색 이상의 가치를 제공하는 고성능 분석 플랫폼입니다. 독자적인 알고리즘을 통해 게임 기여도를 수치화하고, 최상위권 랭커 데이터를 기반으로 최적의 승리 전략을 제시합니다.

---

## 🚀 Key Features
💎 Advanced Analytics (OurScore & TeamScore)

Performance Metric: 단순 KDA를 넘어 딜량, 시야, 오브젝트 관여도를 종합한 OurScore를 산출하여 개인의 실질 기여도를 측정합니다.

Match Evaluation: 팀 전체의 유기적인 플레이를 TeamScore로 객체화하여 승패의 핵심 원인을 분석합니다.

## ⚡ Real-time High-Tier Ranking

Zero-Downtime Update: Redis의 Atomic Swap(Temp-to-Real) 기법을 활용하여, 랭킹 업데이트 중에도 서비스 중단 없이 실시간 챌린저/그마/마스터 리스트를 제공합니다.

Pipelining Technique: 대량의 랭커 데이터를 Redis Pipeline을 통해 고속으로 동기화하여 지연 시간을 최소화했습니다.

## 🛡️ Strategic Guidance

Matchup Analysis: 특정 챔피언 간의 상대 승률과 상성 데이터를 분석하여 맞춤형 빌드를 추천합니다.

Position-based Statistics: 탑부터 서포터까지 각 포지션별 정교한 통계 지표를 제공합니다.

## 🛠 Technical Architecture
### ⚙️ Backend Core & Infrastructure

Language & Framework: 최신 LTS 버전인 Java 21과 Spring Boot 3.4.1을 기반으로 구축되어 강력한 성능과 생산성을 보장합니다.

Database Strategy:

MySQL: 유저 정보, 매치 히스토리 등 영속성이 필요한 데이터를 정규화하여 관리합니다.

Redis: 실시간 랭킹 데이터 및 패치 버전 정보 캐싱을 통해 DB 부하를 줄이고 응답 속도를 극대화합니다.

Containerization: Docker와 Docker Compose를 활용하여 애플리케이션 및 인프라(MySQL, Redis) 환경을 코드 기반으로 관리하고 배포의 일관성을 유지합니다.

### ⚡ Data Flow & Performance Optimization

Zero-Downtime Ranking Update: Redis의 Atomic Swap(rename) 기법을 도입하여, 대량의 랭킹 업데이트 작업 중에도 서비스 중단 없이 실시간 데이터를 제공합니다.

High-Speed Bulk Storage: Redis Pipelining(executePipelined) 기술을 사용하여 대규모 랭커 데이터를 한 번의 네트워크 요청으로 고속 동기화합니다.

Automated Data Lifecycle: RankerScheduler를 통해 20분 주기로 최신 랭크 정보를 자동 동기화하며 시스템의 데이터 선도를 유지합니다.

### 📡 API Resilience & Reliability

Error Resilience: Riot API의 호출 제한(429 Too Many Requests)에 대응하기 위해 재시도 로직과 지연 처리 시스템을 구축하여 데이터 수집의 안정성을 확보했습니다.

Clean Data Management: GameNameTrimFilter를 통해 유저 검색 시 발생하는 불필요한 공백을 정규화하여 검색 정확도를 높였습니다.

---

## 🛠 Tech Stack Summary

| Category | Technology |
| :--- | :--- |
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.4.1 |
| **Database** | MySQL 8.0, Redis |
| **Infrastructure** | Docker, Docker Compose |

---

## ⚖️ Disclaimer
ARCANE isn't endorsed by Riot Games and doesn't reflect the views or opinions of Riot Games or anyone officially involved in producing or managing League of Legends. League of Legends and Riot Games are trademarks or registered trademarks of Riot Games, Inc. League of Legends © Riot Games, Inc.
