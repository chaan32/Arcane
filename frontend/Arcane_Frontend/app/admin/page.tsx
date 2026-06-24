"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import type { ReactNode } from "react";
import { useEffect, useMemo, useState } from "react";
import {
  Activity,
  AlertCircle,
  BarChart3,
  Cpu,
  Database,
  FileText,
  Gauge,
  Loader2,
  RefreshCw,
  Server,
  ShieldCheck,
  ToggleLeft,
  ToggleRight,
} from "lucide-react";
import { useMockAuth } from "@/hooks/useMockAuth";
import { getApiUrl } from "@/constants/api";
import {
  AdminDashboard,
  AdminLogs,
  AdminReadableLogEntry,
  fetchAdminDashboard,
  fetchAdminLogs,
  requestChampionAnalysis,
  requestDatasetCollection,
  requestGameDataSync,
  requestRankingUpdate,
  toggleRankingScheduler,
} from "@/services/adminApi";

const formatBytes = (value: number) => {
  if (value < 0) return "제한 없음";
  if (value < 1024) return `${value} B`;
  const units = ["KB", "MB", "GB", "TB"];
  let size = value / 1024;
  let unitIndex = 0;

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex += 1;
  }

  return `${size.toFixed(size >= 10 ? 1 : 2)} ${units[unitIndex]}`;
};

const formatDuration = (milliseconds: number) => {
  const totalSeconds = Math.floor(milliseconds / 1000);
  const days = Math.floor(totalSeconds / 86400);
  const hours = Math.floor((totalSeconds % 86400) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);

  if (days > 0) return `${days}일 ${hours}시간`;
  if (hours > 0) return `${hours}시간 ${minutes}분`;
  if (minutes > 0) return `${minutes}분`;
  return `${totalSeconds}초`;
};

const formatPercent = (value?: number) => {
  if (value === undefined || value < 0) return "측정 대기";
  return `${(value * 100).toFixed(1)}%`;
};

const formatDateTime = (value: string | null) => {
  if (!value) return "기록 없음";
  return new Intl.DateTimeFormat("ko-KR", {
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
};

const readableLogLevelClass = (level?: string) => {
  switch (level) {
    case "ERROR":
      return "bg-[#ffe4e8] text-[#c0392b] ring-[#ffc6ce]";
    case "WARN":
      return "bg-[#fff1dc] text-[#d66b12] ring-[#ffd8a8]";
    default:
      return "bg-[#e9f8f1] text-[#25825c] ring-[#c8ecd9]";
  }
};

const readableLogSourceLabel = (source?: string) => {
  if (source === "worker") return "Worker";
  return "API";
};

const formatActivityMeta = (activity: AdminReadableLogEntry) => {
  const parts = [
    activity.status !== null ? `status ${activity.status}` : null,
    activity.elapsedMs !== null ? `${activity.elapsedMs}ms` : null,
    activity.user ? `user ${activity.user}` : null,
    activity.traceId ? `trace ${activity.traceId}` : null,
  ].filter(Boolean);

  return parts.length ? parts.join(" · ") : "추가 메타데이터 없음";
};

const clampPercent = (value?: number | null) => {
  if (typeof value !== "number" || Number.isNaN(value)) {
    return 0;
  }
  return Math.min(100, Math.max(0, value));
};

const formatJobStatus = (status?: string | null) => {
  switch (status) {
    case "PUBLISHED":
      return "요청 전송됨";
    case "RUNNING":
      return "진행 중";
    case "COMPLETED":
      return "완료";
    case "FAILED":
      return "실패";
    case "PUBLISH_FAILED":
      return "전송 실패";
    default:
      return status ?? "상태 없음";
  }
};

const datasetRankingOptions = [
  { label: "전체", value: "ranking:all", detail: "챌린저/그마/마스터 통합" },
  { label: "챌린저", value: "ranking:challenger", detail: "최대 약 300명" },
  { label: "그마", value: "ranking:grandmaster", detail: "그랜드마스터" },
  { label: "마스터", value: "ranking:master", detail: "마스터" },
];

const rankerLimitOptions = [300, 500, 1000, 5000, 10000];
const matchCountOptions = [10, 15, 20, 30];

export default function AdminPage() {
  const router = useRouter();
  const { user } = useMockAuth();
  const [dashboard, setDashboard] = useState<AdminDashboard | null>(null);
  const [logs, setLogs] = useState<AdminLogs | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isToggling, setIsToggling] = useState(false);
  const [isRequestingRankingUpdate, setIsRequestingRankingUpdate] = useState(false);
  const [isRequestingDatasetCollection, setIsRequestingDatasetCollection] = useState(false);
  const [isRequestingChampionAnalysis, setIsRequestingChampionAnalysis] = useState(false);
  const [isRequestingGameDataSync, setIsRequestingGameDataSync] = useState(false);
  const [isRefreshingLogs, setIsRefreshingLogs] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [rankingUpdateMessage, setRankingUpdateMessage] = useState("");
  const [datasetCollectionMessage, setDatasetCollectionMessage] = useState("");
  const [championAnalysisMessage, setChampionAnalysisMessage] = useState("");
  const [gameDataSyncMessage, setGameDataSyncMessage] = useState("");
  const [datasetRankingKey, setDatasetRankingKey] = useState("ranking:all");
  const [datasetRankerLimit, setDatasetRankerLimit] = useState(500);
  const [datasetMatchCount, setDatasetMatchCount] = useState(15);
  const readableActivities = logs?.activities ?? [];

  const isClearlyNotAdmin = user?.role === "USER";

  const heapUsageRate = useMemo(() => {
    const memory = dashboard?.serverStatus.memory;
    if (!memory || memory.heapMaxBytes <= 0) return null;
    return (memory.heapUsedBytes / memory.heapMaxBytes) * 100;
  }, [dashboard]);

  const datasetProgressRate = useMemo(() => {
    const job = dashboard?.datasetCollection.latestJob;
    if (!job) return 0;
    if (job.uniqueMatchIds > 0) {
      return Math.min(100, (job.processedMatches / job.uniqueMatchIds) * 100);
    }
    if (job.totalPuuids > 0) {
      return Math.min(100, (job.processedPuuids / job.totalPuuids) * 100);
    }
    return 0;
  }, [dashboard]);

  const loadDashboard = async () => {
    setIsLoading(true);
    setErrorMessage("");

    try {
      const data = await fetchAdminDashboard();
      setDashboard(data);
      setLogs(data.logs);
    } catch (error) {
      const message = error instanceof Error ? error.message : "관리자 정보를 불러오지 못했습니다.";
      setErrorMessage(message);
      if (message.includes("만료") || message.includes("로그인")) {
        router.replace("/");
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (isClearlyNotAdmin) {
      setIsLoading(false);
      setErrorMessage("관리자 권한이 필요합니다.");
      return;
    }

    void loadDashboard();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isClearlyNotAdmin]);

  useEffect(() => {
    const datasetStatus = dashboard?.datasetCollection.latestJob?.status;
    const championAnalysisStatus = dashboard?.championAnalysis.latestJob?.status;
    const gameDataSyncStatus = dashboard?.gameDataSync.latestJob?.status;
    const shouldPoll = [datasetStatus, championAnalysisStatus, gameDataSyncStatus].some((status) =>
      status ? ["PUBLISHED", "RUNNING"].includes(status) : false
    );

    if (!shouldPoll) {
      return;
    }

    const intervalId = window.setInterval(() => {
      void fetchAdminDashboard()
        .then((nextDashboard) => {
          setDashboard(nextDashboard);
          setLogs(nextDashboard.logs);
        })
        .catch(() => undefined);
    }, 5000);

    return () => window.clearInterval(intervalId);
  }, [
    dashboard?.datasetCollection.latestJob?.status,
    dashboard?.championAnalysis.latestJob?.status,
    dashboard?.gameDataSync.latestJob?.status,
  ]);

  const handleToggleRankingScheduler = async () => {
    if (!dashboard || isToggling) return;

    const nextEnabled = !dashboard.rankingScheduler.enabled;
    setIsToggling(true);
    setErrorMessage("");
    setRankingUpdateMessage("");

    try {
      const nextStatus = await toggleRankingScheduler(nextEnabled);
      setDashboard((current) =>
        current ? { ...current, rankingScheduler: nextStatus } : current
      );
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "랭킹 스케줄러 설정을 바꾸지 못했습니다.");
    } finally {
      setIsToggling(false);
    }
  };

  const handleRequestRankingUpdate = async () => {
    if (isRequestingRankingUpdate) return;

    setIsRequestingRankingUpdate(true);
    setErrorMessage("");
    setRankingUpdateMessage("");

    try {
      const response = await requestRankingUpdate();
      setRankingUpdateMessage(`${response.message} jobId=${response.jobId}`);

      const nextDashboard = await fetchAdminDashboard();
      setDashboard(nextDashboard);
      setLogs(nextDashboard.logs);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "랭킹 업데이트 요청을 보내지 못했습니다.");
    } finally {
      setIsRequestingRankingUpdate(false);
    }
  };

  const handleRequestDatasetCollection = async () => {
    if (isRequestingDatasetCollection) return;

    const safeRankerLimit = Math.max(1, Math.min(10000, datasetRankerLimit));
    const safeMatchCount = Math.max(1, Math.min(100, datasetMatchCount));

    setIsRequestingDatasetCollection(true);
    setErrorMessage("");
    setDatasetCollectionMessage("");

    try {
      const response = await requestDatasetCollection({
        rankingKey: datasetRankingKey,
        rankerLimit: safeRankerLimit,
        matchCount: safeMatchCount,
        queueId: 420,
      });
      setDatasetRankerLimit(safeRankerLimit);
      setDatasetMatchCount(safeMatchCount);
      setDatasetCollectionMessage(
        `${response.message} 상위 ${safeRankerLimit.toLocaleString("ko-KR")}명 · 1인 ${safeMatchCount}게임 · jobId=${response.jobId}`,
      );

      const nextDashboard = await fetchAdminDashboard();
      setDashboard(nextDashboard);
      setLogs(nextDashboard.logs);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "데이터 수집 요청을 보내지 못했습니다.");
    } finally {
      setIsRequestingDatasetCollection(false);
    }
  };

  const handleRequestChampionAnalysis = async () => {
    if (isRequestingChampionAnalysis) return;

    setIsRequestingChampionAnalysis(true);
    setErrorMessage("");
    setChampionAnalysisMessage("");

    try {
      const response = await requestChampionAnalysis();
      setChampionAnalysisMessage(`${response.message} jobId=${response.jobId}`);

      const nextDashboard = await fetchAdminDashboard();
      setDashboard(nextDashboard);
      setLogs(nextDashboard.logs);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "챔피언 분석 요청을 보내지 못했습니다.");
    } finally {
      setIsRequestingChampionAnalysis(false);
    }
  };

  const handleRequestGameDataSync = async () => {
    if (isRequestingGameDataSync) return;

    setIsRequestingGameDataSync(true);
    setErrorMessage("");
    setGameDataSyncMessage("");

    try {
      const response = await requestGameDataSync();
      setGameDataSyncMessage(`${response.message} jobId=${response.jobId}`);

      const nextDashboard = await fetchAdminDashboard();
      setDashboard(nextDashboard);
      setLogs(nextDashboard.logs);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "게임 데이터 동기화 요청을 보내지 못했습니다.");
    } finally {
      setIsRequestingGameDataSync(false);
    }
  };

  const handleRefreshLogs = async () => {
    setIsRefreshingLogs(true);
    setErrorMessage("");

    try {
      setLogs(await fetchAdminLogs(200, logs?.selectedFileName));
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "로그를 새로고침하지 못했습니다.");
    } finally {
      setIsRefreshingLogs(false);
    }
  };

  const handleSelectLogFile = async (fileName: string) => {
    if (isRefreshingLogs || fileName === logs?.selectedFileName) return;

    setIsRefreshingLogs(true);
    setErrorMessage("");

    try {
      setLogs(await fetchAdminLogs(200, fileName));
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "선택한 로그 파일을 불러오지 못했습니다.");
    } finally {
      setIsRefreshingLogs(false);
    }
  };

  if (isLoading) {
    return (
      <main className="min-h-[calc(100vh-5rem)] px-4 py-14 text-[#69324b]">
        <section className="mx-auto flex max-w-[34rem] flex-col items-center rounded-[2rem] bg-white/92 px-8 py-12 shadow-[0_28px_80px_rgba(205,79,134,0.16)] ring-1 ring-[#ffd1e3]/70">
          <Loader2 className="h-10 w-10 animate-spin text-[#e75491]" />
          <p className="mt-4 text-sm font-black text-[#a76886]">관리자 대시보드를 불러오는 중입니다.</p>
        </section>
      </main>
    );
  }

  if (!dashboard) {
    return (
      <main className="min-h-[calc(100vh-5rem)] px-4 py-14 text-[#69324b]">
        <section className="mx-auto max-w-[38rem] rounded-[2rem] bg-white/92 px-8 py-12 text-center shadow-[0_28px_80px_rgba(205,79,134,0.16)] ring-1 ring-[#ffd1e3]/70">
          <AlertCircle className="mx-auto h-12 w-12 text-[#e75491]" />
          <h1 className="mt-4 text-2xl font-black">관리자 대시보드를 열 수 없습니다</h1>
          <p className="mt-3 text-sm font-bold text-[#a76886]">{errorMessage}</p>
          <Link
            href="/"
            className="mt-6 inline-flex h-12 items-center justify-center rounded-full bg-[#f45f9c] px-6 text-sm font-black text-white shadow-[0_14px_30px_rgba(231,84,145,0.24)]"
          >
            메인으로
          </Link>
        </section>
      </main>
    );
  }

  const { rankingScheduler, serverStatus } = dashboard;

  return (
    <main className="min-h-[calc(100vh-5rem)] px-4 py-10 text-[#69324b] lg:py-14">
      <section className="mx-auto max-w-[92rem]">
        <div className="rounded-[2.25rem] bg-white/92 p-6 shadow-[0_30px_90px_rgba(205,79,134,0.15)] ring-1 ring-[#ffd1e3]/70 lg:p-8">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <div className="inline-flex items-center gap-2 rounded-full bg-[#fff0f7] px-3 py-1 text-xs font-black text-[#e75491] ring-1 ring-[#ffd1e3]">
                <ShieldCheck className="h-4 w-4" />
                Admin Dashboard
              </div>
              <h1 className="mt-4 text-3xl font-black lg:text-5xl">관리자 대시보드</h1>
              <p className="mt-3 max-w-[44rem] break-keep text-sm font-bold leading-6 text-[#a76886]">
                랭킹 자동 갱신, 서버 상태 지표, 애플리케이션 로그를 한 곳에서 확인합니다.
              </p>
            </div>
            <button
              type="button"
              onClick={() => void loadDashboard()}
              className="inline-flex h-12 items-center gap-2 rounded-full border border-[#ffd1e3] bg-[#fff7fb] px-5 text-sm font-black text-[#e75491] transition-colors hover:bg-[#ffe0ee]"
            >
              <RefreshCw className="h-4 w-4" />
              새로고침
            </button>
          </div>

          {errorMessage && (
            <div className="mt-5 rounded-[1.25rem] bg-[#fff0f7] px-4 py-3 text-sm font-black text-[#e75491] ring-1 ring-[#ffd1e3]">
              {errorMessage}
            </div>
          )}

          <div className="mt-8 grid gap-5 xl:grid-cols-[0.9fr_1.1fr]">
            <section className="rounded-[2rem] bg-[#fff7fb] p-5 ring-1 ring-[#ffd1e3]/70">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-sm font-black text-[#a76886]">랭킹 자동 갱신</p>
                  <h2 className="mt-1 text-2xl font-black">
                    {rankingScheduler.enabled ? "20분마다 실행 중" : "자동 실행 꺼짐"}
                  </h2>
                  <p className="mt-2 text-sm font-bold text-[#a76886]">
                    상태: {rankingScheduler.running ? "업데이트 진행 중" : rankingScheduler.lastResult}
                  </p>
                </div>
                <div className="flex flex-wrap justify-end gap-2">
                  <button
                    type="button"
                    onClick={() => void handleRequestRankingUpdate()}
                    disabled={isRequestingRankingUpdate}
                    className="inline-flex h-14 items-center gap-2 rounded-full bg-[#f45f9c] px-4 text-sm font-black text-white shadow-[0_14px_30px_rgba(231,84,145,0.22)] transition-transform hover:-translate-y-0.5 disabled:translate-y-0 disabled:opacity-60"
                  >
                    {isRequestingRankingUpdate ? (
                      <Loader2 className="h-5 w-5 animate-spin" />
                    ) : (
                      <RefreshCw className="h-5 w-5" />
                    )}
                    지금 업데이트
                  </button>
                  <button
                    type="button"
                    onClick={handleToggleRankingScheduler}
                    disabled={isToggling}
                    className="inline-flex h-14 items-center gap-2 rounded-full bg-white px-4 text-sm font-black text-[#e75491] shadow-[0_14px_30px_rgba(205,79,134,0.12)] ring-1 ring-[#ffd1e3] disabled:opacity-60"
                  >
                    {isToggling ? (
                      <Loader2 className="h-5 w-5 animate-spin" />
                    ) : rankingScheduler.enabled ? (
                      <ToggleRight className="h-7 w-7" />
                    ) : (
                      <ToggleLeft className="h-7 w-7" />
                    )}
                    {rankingScheduler.enabled ? "활성화" : "비활성화"}
                  </button>
                </div>
              </div>
              <div className="mt-5 grid gap-3 sm:grid-cols-2">
                <MetricCard
                  label="실행 주기"
                  value={`${rankingScheduler.intervalMinutes}분`}
                  icon={<RefreshCw className="h-5 w-5" />}
                />
                <MetricCard
                  label="마지막 트리거"
                  value={rankingScheduler.lastTrigger}
                  icon={<Activity className="h-5 w-5" />}
                />
                <MetricCard
                  label="마지막 시작"
                  value={formatDateTime(rankingScheduler.lastStartedAt)}
                  icon={<BarChart3 className="h-5 w-5" />}
                />
                <MetricCard
                  label="마지막 완료"
                  value={formatDateTime(rankingScheduler.lastFinishedAt)}
                  icon={<Gauge className="h-5 w-5" />}
                />
              </div>
              {rankingScheduler.lastErrorMessage && (
                <p className="mt-4 rounded-[1rem] bg-white px-4 py-3 text-sm font-bold text-[#e75491] ring-1 ring-[#ffd1e3]">
                  {rankingScheduler.lastErrorMessage}
                </p>
              )}
              {rankingUpdateMessage && (
                <p className="mt-4 rounded-[1rem] bg-white px-4 py-3 text-sm font-bold text-[#e75491] ring-1 ring-[#ffd1e3]">
                  {rankingUpdateMessage}
                </p>
              )}
              {rankingScheduler.latestWorkerJob && (
                <div className="mt-4 rounded-[1.25rem] bg-white/92 px-4 py-3 text-sm font-bold text-[#a76886] ring-1 ring-[#ffd1e3]/80">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <p className="font-black text-[#69324b]">최근 worker 작업</p>
                    <span className="rounded-full bg-[#fff0f7] px-3 py-1 text-xs font-black text-[#e75491]">
                      {formatJobStatus(rankingScheduler.latestWorkerJob.status)}
                    </span>
                  </div>
                  <p className="mt-2 break-keep">{rankingScheduler.latestWorkerJob.message ?? "메시지 없음"}</p>
                  <p className="mt-1 break-all text-xs text-[#bd7b98]">
                    jobId={rankingScheduler.latestWorkerJob.jobId}
                  </p>
                  <p className="mt-1 text-xs text-[#bd7b98]">
                    요청 {formatDateTime(rankingScheduler.latestWorkerJob.requestedAt)} · 완료{" "}
                    {formatDateTime(rankingScheduler.latestWorkerJob.completedAt)}
                  </p>
                </div>
              )}
            </section>

            <section className="rounded-[2rem] bg-[#fff7fb] p-5 ring-1 ring-[#ffd1e3]/70">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="text-sm font-black text-[#a76886]">서버 상태</p>
                  <h2 className="mt-1 text-2xl font-black">Actuator / Micrometer 지표</h2>
                </div>
                <Server className="h-8 w-8 text-[#e75491]" />
              </div>
              <div className="mt-5 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                <MetricCard
                  label="Uptime"
                  value={formatDuration(serverStatus.uptimeMs)}
                  icon={<Activity className="h-5 w-5" />}
                />
                <MetricCard
                  label="Heap 사용량"
                  value={heapUsageRate === null ? "측정 대기" : `${heapUsageRate.toFixed(1)}%`}
                  detail={`${formatBytes(serverStatus.memory.heapUsedBytes)} / ${formatBytes(serverStatus.memory.heapMaxBytes)}`}
                  icon={<Server className="h-5 w-5" />}
                />
                <MetricCard
                  label="Process CPU"
                  value={formatPercent(serverStatus.cpu.processCpuLoad)}
                  icon={<Cpu className="h-5 w-5" />}
                />
                <MetricCard
                  label="Live Threads"
                  value={`${serverStatus.threads.live}`}
                  detail={`peak ${serverStatus.threads.peak}`}
                  icon={<Gauge className="h-5 w-5" />}
                />
                <MetricCard
                  label="HTTP 요청 수"
                  value={`${serverStatus.http.requestCount}`}
                  detail={`평균 ${serverStatus.http.averageMs.toFixed(1)}ms`}
                  icon={<BarChart3 className="h-5 w-5" />}
                />
                <MetricCard
                  label="최대 응답 시간"
                  value={`${serverStatus.http.maxMs.toFixed(1)}ms`}
                  icon={<Activity className="h-5 w-5" />}
                />
              </div>
              <div className="mt-5 flex flex-wrap gap-2">
                {Object.entries(serverStatus.monitoring).map(([key, path]) => (
                  <a
                    key={key}
                    href={getApiUrl(path)}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex h-10 items-center rounded-full bg-white px-4 text-xs font-black text-[#e75491] ring-1 ring-[#ffd1e3] transition-colors hover:bg-[#ffe0ee]"
                  >
                    {key.replace("Url", "")}
                  </a>
                ))}
              </div>
            </section>
          </div>

          <section className="mt-5 rounded-[2rem] bg-[#fff7fb] p-5 ring-1 ring-[#ffd1e3]/70">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <div className="inline-flex items-center gap-2 rounded-full bg-white px-3 py-1 text-xs font-black text-[#e75491] ring-1 ring-[#ffd1e3]">
                  <Database className="h-4 w-4" />
                  Dataset Collection
                </div>
                <h2 className="mt-3 text-2xl font-black">상위 랭커 게임 학습 데이터 수집</h2>
                <p className="mt-2 max-w-[54rem] break-keep text-sm font-bold leading-6 text-[#a76886]">
                  Redis 랭킹 상위 PUUID를 기준으로 원하는 인원과 게임 수만큼 최근 솔로랭크 매치를 수집하고, MySQL과 MongoDB에 중복 없이 저장합니다.
                </p>
              </div>
              <button
                type="button"
                onClick={() => void handleRequestDatasetCollection()}
                disabled={isRequestingDatasetCollection}
                className="inline-flex h-14 items-center gap-2 rounded-full bg-[#f45f9c] px-5 text-sm font-black text-white shadow-[0_14px_30px_rgba(231,84,145,0.22)] transition-transform hover:-translate-y-0.5 disabled:translate-y-0 disabled:opacity-60"
              >
                {isRequestingDatasetCollection ? (
                  <Loader2 className="h-5 w-5 animate-spin" />
                ) : (
                  <Database className="h-5 w-5" />
                )}
                데이터 수집 시작
              </button>
            </div>

            <div className="mt-5 grid gap-3 xl:grid-cols-[1.15fr_0.85fr]">
              <div className="rounded-[1.5rem] bg-white/92 p-4 ring-1 ring-[#ffd1e3]/70">
                <p className="text-xs font-black text-[#a76886]">랭킹 범위</p>
                <div className="mt-3 grid gap-2 sm:grid-cols-2 xl:grid-cols-4">
                  {datasetRankingOptions.map((option) => {
                    const selected = datasetRankingKey === option.value;
                    return (
                      <button
                        key={option.value}
                        type="button"
                        onClick={() => setDatasetRankingKey(option.value)}
                        className={`rounded-[1rem] px-4 py-3 text-left transition-colors ring-1 ${
                          selected
                            ? "bg-[#f45f9c] text-white ring-[#f45f9c]"
                            : "bg-[#fff7fb] text-[#69324b] ring-[#ffd1e3] hover:bg-[#ffe0ee]"
                        }`}
                      >
                        <span className="block text-sm font-black">{option.label}</span>
                        <span className={`mt-1 block text-xs font-bold ${selected ? "text-white/85" : "text-[#a76886]"}`}>
                          {option.detail}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="rounded-[1.5rem] bg-white/92 p-4 ring-1 ring-[#ffd1e3]/70">
                <div className="grid gap-4 sm:grid-cols-2">
                  <div>
                    <p className="text-xs font-black text-[#a76886]">수집 랭커 수</p>
                    <div className="mt-3 flex flex-wrap gap-2">
                      {rankerLimitOptions.map((limit) => (
                        <button
                          key={limit}
                          type="button"
                          onClick={() => setDatasetRankerLimit(limit)}
                          className={`h-9 rounded-full px-3 text-xs font-black transition-colors ring-1 ${
                            datasetRankerLimit === limit
                              ? "bg-[#f45f9c] text-white ring-[#f45f9c]"
                              : "bg-[#fff7fb] text-[#e75491] ring-[#ffd1e3] hover:bg-[#ffe0ee]"
                          }`}
                        >
                          {limit.toLocaleString("ko-KR")}명
                        </button>
                      ))}
                    </div>
                    <input
                      type="number"
                      min={1}
                      max={10000}
                      value={datasetRankerLimit}
                      onChange={(event) => setDatasetRankerLimit(Number(event.target.value))}
                      className="mt-3 h-11 w-full rounded-full border border-[#ffd1e3] bg-[#fff7fb] px-4 text-sm font-black text-[#69324b] outline-none focus:border-[#f45f9c]"
                    />
                  </div>

                  <div>
                    <p className="text-xs font-black text-[#a76886]">1인당 게임 수</p>
                    <div className="mt-3 flex flex-wrap gap-2">
                      {matchCountOptions.map((count) => (
                        <button
                          key={count}
                          type="button"
                          onClick={() => setDatasetMatchCount(count)}
                          className={`h-9 rounded-full px-3 text-xs font-black transition-colors ring-1 ${
                            datasetMatchCount === count
                              ? "bg-[#f45f9c] text-white ring-[#f45f9c]"
                              : "bg-[#fff7fb] text-[#e75491] ring-[#ffd1e3] hover:bg-[#ffe0ee]"
                          }`}
                        >
                          {count}게임
                        </button>
                      ))}
                    </div>
                    <input
                      type="number"
                      min={1}
                      max={100}
                      value={datasetMatchCount}
                      onChange={(event) => setDatasetMatchCount(Number(event.target.value))}
                      className="mt-3 h-11 w-full rounded-full border border-[#ffd1e3] bg-[#fff7fb] px-4 text-sm font-black text-[#69324b] outline-none focus:border-[#f45f9c]"
                    />
                  </div>
                </div>
              </div>
            </div>

            {datasetCollectionMessage && (
              <p className="mt-4 rounded-[1rem] bg-white px-4 py-3 text-sm font-bold text-[#e75491] ring-1 ring-[#ffd1e3]">
                {datasetCollectionMessage}
              </p>
            )}

            {dashboard.datasetCollection.latestJob ? (
              <div className="mt-5 rounded-[1.5rem] bg-white/92 p-5 shadow-[0_14px_30px_rgba(205,79,134,0.08)] ring-1 ring-[#ffd1e3]/70">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="text-xs font-black text-[#a76886]">
                      {dashboard.datasetCollection.latestJob.rankingKey ?? "ranking:challenger"}
                    </p>
                    <h3 className="mt-1 text-xl font-black text-[#69324b]">
                      {formatJobStatus(dashboard.datasetCollection.latestJob.status)}
                    </h3>
                    <p className="mt-1 break-keep text-sm font-bold text-[#a76886]">
                      {dashboard.datasetCollection.latestJob.message ?? "진행 메시지 없음"}
                    </p>
                  </div>
                  <span className="rounded-full bg-[#fff0f7] px-4 py-2 text-xs font-black text-[#e75491] ring-1 ring-[#ffd1e3]">
                    {dashboard.datasetCollection.latestJob.phase}
                  </span>
                </div>

                <div className="mt-5 h-3 overflow-hidden rounded-full bg-[#ffe0ee]">
                  <div
                    className="h-full rounded-full bg-[#f45f9c] transition-all duration-500"
                    style={{ width: `${datasetProgressRate}%` }}
                  />
                </div>
                <p className="mt-2 text-right text-xs font-black text-[#a76886]">
                  {datasetProgressRate.toFixed(1)}%
                </p>

                <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                  <MetricCard
                    label="PUUID 처리"
                    value={`${dashboard.datasetCollection.latestJob.processedPuuids} / ${dashboard.datasetCollection.latestJob.totalPuuids}`}
                    detail={`상위 ${dashboard.datasetCollection.latestJob.rankerLimit}명 요청`}
                    icon={<Database className="h-5 w-5" />}
                  />
                  <MetricCard
                    label="Unique Match"
                    value={`${dashboard.datasetCollection.latestJob.uniqueMatchIds}`}
                    detail={`후보 ${dashboard.datasetCollection.latestJob.candidateMatchIds}개`}
                    icon={<BarChart3 className="h-5 w-5" />}
                  />
                  <MetricCard
                    label="저장된 Mongo raw"
                    value={`${dashboard.datasetCollection.latestJob.savedMongoParticipants}`}
                    detail={`MySQL match ${dashboard.datasetCollection.latestJob.savedMysqlMatches}개`}
                    icon={<Database className="h-5 w-5" />}
                  />
                  <MetricCard
                    label="실패 / 재시도"
                    value={`${dashboard.datasetCollection.latestJob.failedMatches} / ${dashboard.datasetCollection.latestJob.retryCount}`}
                    detail={`중복 스킵 ${dashboard.datasetCollection.latestJob.skippedDuplicateMatches}`}
                    icon={<AlertCircle className="h-5 w-5" />}
                  />
                </div>

                <p className="mt-4 break-all text-xs font-bold text-[#bd7b98]">
                  jobId={dashboard.datasetCollection.latestJob.jobId} · updatedAt{" "}
                  {formatDateTime(dashboard.datasetCollection.latestJob.updatedAt)}
                </p>
              </div>
            ) : (
              <div className="mt-5 rounded-[1.5rem] bg-white/82 px-5 py-8 text-center text-sm font-black text-[#a76886] ring-1 ring-[#ffd1e3]/70">
                아직 실행된 데이터 수집 작업이 없습니다.
              </div>
            )}
          </section>

          <section className="mt-5 rounded-[2rem] bg-[#fff7fb] p-5 ring-1 ring-[#ffd1e3]/70">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <div className="inline-flex items-center gap-2 rounded-full bg-white px-3 py-1 text-xs font-black text-[#e75491] ring-1 ring-[#ffd1e3]">
                  <RefreshCw className="h-4 w-4" />
                  Game Data Sync
                </div>
                <h2 className="mt-3 text-2xl font-black">게임 데이터 동기화</h2>
                <p className="mt-2 max-w-[54rem] break-keep text-sm font-bold leading-6 text-[#a76886]">
                  Data Dragon 최신 버전을 기준으로 챔피언, 스킬, 아이템, 소환사 주문, 룬 메타데이터를 Worker에서 동기화합니다.
                </p>
              </div>
              <button
                type="button"
                onClick={() => void handleRequestGameDataSync()}
                disabled={isRequestingGameDataSync}
                className="inline-flex h-14 items-center gap-2 rounded-full bg-[#f45f9c] px-5 text-sm font-black text-white shadow-[0_14px_30px_rgba(231,84,145,0.22)] transition-transform hover:-translate-y-0.5 disabled:translate-y-0 disabled:opacity-60"
              >
                {isRequestingGameDataSync ? (
                  <Loader2 className="h-5 w-5 animate-spin" />
                ) : (
                  <RefreshCw className="h-5 w-5" />
                )}
                게임 데이터 동기화
              </button>
            </div>

            {gameDataSyncMessage && (
              <p className="mt-4 rounded-[1rem] bg-white px-4 py-3 text-sm font-bold text-[#e75491] ring-1 ring-[#ffd1e3]">
                {gameDataSyncMessage}
              </p>
            )}

            {dashboard.gameDataSync.latestJob ? (
              <div className="mt-5 rounded-[1.5rem] bg-white/92 p-5 shadow-[0_14px_30px_rgba(205,79,134,0.08)] ring-1 ring-[#ffd1e3]/70">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="text-xs font-black text-[#a76886]">
                      Data Dragon {dashboard.gameDataSync.latestJob.version ?? "version 대기"}
                    </p>
                    <h3 className="mt-1 text-xl font-black text-[#69324b]">
                      {formatJobStatus(dashboard.gameDataSync.latestJob.status)}
                    </h3>
                    <p className="mt-1 break-keep text-sm font-bold text-[#a76886]">
                      {dashboard.gameDataSync.latestJob.message ?? "진행 메시지 없음"}
                    </p>
                  </div>
                  <span className="rounded-full bg-[#fff0f7] px-4 py-2 text-xs font-black text-[#e75491] ring-1 ring-[#ffd1e3]">
                    {dashboard.gameDataSync.latestJob.version ?? "동기화 대기"}
                  </span>
                </div>

                <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                  <MetricCard
                    label="챔피언"
                    value={`${dashboard.gameDataSync.latestJob.championCount}`}
                    detail="챔피언/스킬 메타"
                    icon={<Database className="h-5 w-5" />}
                  />
                  <MetricCard
                    label="아이템"
                    value={`${dashboard.gameDataSync.latestJob.itemCount}`}
                    detail="Data Dragon item"
                    icon={<BarChart3 className="h-5 w-5" />}
                  />
                  <MetricCard
                    label="소환사 주문"
                    value={`${dashboard.gameDataSync.latestJob.summonerSpellCount}`}
                    detail="spell metadata"
                    icon={<Gauge className="h-5 w-5" />}
                  />
                  <MetricCard
                    label="룬"
                    value={`${dashboard.gameDataSync.latestJob.runeCount}`}
                    detail="runesReforged"
                    icon={<Activity className="h-5 w-5" />}
                  />
                </div>

                <p className="mt-4 break-all text-xs font-bold text-[#bd7b98]">
                  jobId={dashboard.gameDataSync.latestJob.jobId} · requestedAt{" "}
                  {formatDateTime(dashboard.gameDataSync.latestJob.requestedAt)}
                </p>
              </div>
            ) : (
              <div className="mt-5 rounded-[1.5rem] bg-white/82 px-5 py-8 text-center text-sm font-black text-[#a76886] ring-1 ring-[#ffd1e3]/70">
                아직 실행된 게임 데이터 동기화 작업이 없습니다.
              </div>
            )}
          </section>

          <section className="mt-5 rounded-[2rem] bg-[#fff7fb] p-5 ring-1 ring-[#ffd1e3]/70">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <div className="inline-flex items-center gap-2 rounded-full bg-white px-3 py-1 text-xs font-black text-[#e75491] ring-1 ring-[#ffd1e3]">
                  <BarChart3 className="h-4 w-4" />
                  Champion Analysis
                </div>
                <h2 className="mt-3 text-2xl font-black">챔피언 분석 실행</h2>
                <p className="mt-2 max-w-[54rem] break-keep text-sm font-bold leading-6 text-[#a76886]">
                  MongoDB에 저장된 참가자 raw 데이터를 기준으로 챔피언별 픽, 승패, 아이템, 스펠, 상성 통계 스냅샷을 계산합니다.
                </p>
              </div>
              <button
                type="button"
                onClick={() => void handleRequestChampionAnalysis()}
                disabled={isRequestingChampionAnalysis}
                className="inline-flex h-14 items-center gap-2 rounded-full bg-[#f45f9c] px-5 text-sm font-black text-white shadow-[0_14px_30px_rgba(231,84,145,0.22)] transition-transform hover:-translate-y-0.5 disabled:translate-y-0 disabled:opacity-60"
              >
                {isRequestingChampionAnalysis ? (
                  <Loader2 className="h-5 w-5 animate-spin" />
                ) : (
                  <BarChart3 className="h-5 w-5" />
                )}
                챔피언 분석 시작
              </button>
            </div>

            {championAnalysisMessage && (
              <p className="mt-4 rounded-[1rem] bg-white px-4 py-3 text-sm font-bold text-[#e75491] ring-1 ring-[#ffd1e3]">
                {championAnalysisMessage}
              </p>
            )}

            {dashboard.championAnalysis.latestJob ? (
              <div className="mt-5 rounded-[1.5rem] bg-white/92 p-5 shadow-[0_14px_30px_rgba(205,79,134,0.08)] ring-1 ring-[#ffd1e3]/70">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="text-xs font-black text-[#a76886]">
                      {dashboard.championAnalysis.latestJob.sourceCollection ?? "riot_match_participant_raw"}
                    </p>
                    <h3 className="mt-1 text-xl font-black text-[#69324b]">
                      {formatJobStatus(dashboard.championAnalysis.latestJob.status)}
                    </h3>
                    <p className="mt-1 break-keep text-sm font-bold text-[#a76886]">
                      {dashboard.championAnalysis.latestJob.message ?? "진행 메시지 없음"}
                    </p>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="rounded-full bg-[#fff0f7] px-4 py-2 text-xs font-black text-[#e75491] ring-1 ring-[#ffd1e3]">
                      {clampPercent(dashboard.championAnalysis.latestJob.progressPercent)}%
                    </span>
                    <span className="rounded-full bg-[#fff0f7] px-4 py-2 text-xs font-black text-[#e75491] ring-1 ring-[#ffd1e3]">
                      patch {dashboard.championAnalysis.latestJob.patchVersion ?? "대기"}
                    </span>
                  </div>
                </div>

                <div className="mt-4">
                  <div className="flex items-center justify-between text-xs font-black text-[#bd7b98]">
                    <span>{dashboard.championAnalysis.latestJob.phase ?? "REQUESTED"}</span>
                    <span>{clampPercent(dashboard.championAnalysis.latestJob.progressPercent)}%</span>
                  </div>
                  <div className="mt-2 h-3 overflow-hidden rounded-full bg-[#ffe4ef]">
                    <div
                      className="h-full rounded-full bg-[#f45f9c] transition-[width] duration-500"
                      style={{
                        width: `${clampPercent(dashboard.championAnalysis.latestJob.progressPercent)}%`,
                      }}
                    />
                  </div>
                </div>

                <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                  <MetricCard
                    label="분석 참가자"
                    value={`${dashboard.championAnalysis.latestJob.totalParticipants}`}
                    detail="Mongo raw participant"
                    icon={<Database className="h-5 w-5" />}
                  />
                  <MetricCard
                    label="분석 매치"
                    value={`${dashboard.championAnalysis.latestJob.totalMatches}`}
                    detail="중복 matchId 제거 기준"
                    icon={<BarChart3 className="h-5 w-5" />}
                  />
                  <MetricCard
                    label="분석 챔피언"
                    value={`${dashboard.championAnalysis.latestJob.totalChampions}`}
                    detail="championId distinct"
                    icon={<Gauge className="h-5 w-5" />}
                  />
                  <MetricCard
                    label="Snapshot"
                    value={dashboard.championAnalysis.latestJob.snapshotId ? "생성됨" : "대기 중"}
                    detail={dashboard.championAnalysis.latestJob.snapshotId ?? "worker 처리 대기"}
                    icon={<Activity className="h-5 w-5" />}
                  />
                </div>

                <p className="mt-4 break-all text-xs font-bold text-[#bd7b98]">
                  jobId={dashboard.championAnalysis.latestJob.jobId} · requestedAt{" "}
                  {formatDateTime(dashboard.championAnalysis.latestJob.requestedAt)}
                </p>
              </div>
            ) : (
              <div className="mt-5 rounded-[1.5rem] bg-white/82 px-5 py-8 text-center text-sm font-black text-[#a76886] ring-1 ring-[#ffd1e3]/70">
                아직 실행된 챔피언 분석 작업이 없습니다.
              </div>
            )}
          </section>

          <section className="mt-5 rounded-[2rem] bg-[#fff7fb] p-5 ring-1 ring-[#ffd1e3]/70">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-sm font-black text-[#a76886]">로그 모니터링</p>
                <h2 className="mt-1 text-2xl font-black">시간별 애플리케이션 로그</h2>
                <p className="mt-2 break-all text-xs font-bold text-[#bd7b98]">{logs?.filePath}</p>
              </div>
              <button
                type="button"
                onClick={handleRefreshLogs}
                disabled={isRefreshingLogs}
                className="inline-flex h-11 items-center gap-2 rounded-full bg-white px-4 text-sm font-black text-[#e75491] ring-1 ring-[#ffd1e3] disabled:opacity-60"
              >
                {isRefreshingLogs ? <Loader2 className="h-4 w-4 animate-spin" /> : <FileText className="h-4 w-4" />}
                로그 새로고침
              </button>
            </div>

            <div className="mt-5 rounded-[1.5rem] bg-white/82 p-3 shadow-[inset_0_0_0_1px_rgba(255,209,227,0.72)]">
              <div className="mb-3 flex items-center justify-between gap-3 px-1">
                <p className="text-xs font-black text-[#a76886]">로그 파일 선택</p>
                <p className="text-xs font-bold text-[#bd7b98]">
                  최근 {logs?.files.length ?? 0}개 파일
                </p>
              </div>
              {logs?.files.length ? (
                <div className="flex gap-2 overflow-x-auto pb-1">
                  {logs.files.map((file) => {
                    const isSelected = file.fileName === logs.selectedFileName;

                    return (
                      <button
                        key={file.filePath}
                        type="button"
                        onClick={() => void handleSelectLogFile(file.fileName)}
                        disabled={isRefreshingLogs}
                        className={`flex min-w-[11rem] flex-col items-start rounded-[1.1rem] px-4 py-3 text-left transition-all disabled:opacity-60 ${
                          isSelected
                            ? "bg-[#f45f9c] text-white shadow-[0_14px_30px_rgba(231,84,145,0.24)]"
                            : "bg-[#fff7fb] text-[#69324b] ring-1 ring-[#ffd1e3] hover:bg-[#ffe0ee]"
                          }`}
                      >
                        <span
                          className={`mb-1 rounded-full px-2 py-0.5 text-[0.68rem] font-black ${
                            isSelected ? "bg-white/18 text-white" : "bg-white text-[#e75491] ring-1 ring-[#ffd1e3]"
                          }`}
                        >
                          {file.serverName}
                        </span>
                        <span className="text-sm font-black">{file.displayName}</span>
                        <span
                          className={`mt-1 text-xs font-bold ${
                            isSelected ? "text-white/78" : "text-[#a76886]"
                          }`}
                        >
                          {file.active ? "실시간 작성 중" : file.compressed ? "압축 로그" : "로그 파일"} · {formatBytes(file.sizeBytes)}
                        </span>
                      </button>
                    );
                  })}
                </div>
              ) : (
                <div className="rounded-[1rem] bg-[#fff7fb] px-4 py-5 text-center text-sm font-bold text-[#a76886]">
                  생성된 로그 파일이 없습니다.
                </div>
              )}
            </div>

            <div className="mt-5 rounded-[1.5rem] bg-white/90 p-4 ring-1 ring-[#ffd1e3]/70">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="text-xs font-black text-[#a76886]">읽기 쉬운 활동 로그</p>
                  <h3 className="mt-1 text-lg font-black text-[#3d1b2b]">사람이 읽는 운영 이벤트</h3>
                </div>
                <span className="rounded-full bg-[#fff7fb] px-3 py-1 text-xs font-black text-[#e75491] ring-1 ring-[#ffd1e3]">
                  최근 {readableActivities.length}건
                </span>
              </div>

              <div className="mt-4 max-h-[20rem] space-y-2 overflow-auto pr-1">
                {readableActivities.length ? (
                  readableActivities
                    .slice(-40)
                    .reverse()
                    .map((activity, index) => (
                      <div
                        key={`${activity.traceId ?? activity.occurredAt ?? index}-${index}`}
                        className="rounded-[1.1rem] bg-[#fff7fb] px-4 py-3 ring-1 ring-[#ffd1e3]/80"
                      >
                        <div className="flex flex-wrap items-center gap-2 text-[0.7rem] font-black">
                          <span className={`rounded-full px-2 py-0.5 ring-1 ${readableLogLevelClass(activity.level)}`}>
                            {activity.level}
                          </span>
                          <span className="rounded-full bg-white px-2 py-0.5 text-[#e75491] ring-1 ring-[#ffd1e3]">
                            {readableLogSourceLabel(activity.source)}
                          </span>
                          <span className="rounded-full bg-white px-2 py-0.5 text-[#a76886] ring-1 ring-[#ffd1e3]">
                            {activity.category}
                          </span>
                          <span className="text-[#bd7b98]">{activity.occurredAt ?? "시간 정보 없음"}</span>
                        </div>
                        <p className="mt-2 break-keep text-sm font-black text-[#3d1b2b]">{activity.message}</p>
                        {activity.detail ? (
                          <p className="mt-1 break-all text-xs font-bold text-[#a76886]">{activity.detail}</p>
                        ) : null}
                        <p className="mt-1 break-all text-[0.7rem] font-bold text-[#bd7b98]">
                          {formatActivityMeta(activity)}
                        </p>
                      </div>
                    ))
                ) : (
                  <div className="rounded-[1rem] bg-[#fff7fb] px-4 py-6 text-center text-sm font-bold text-[#a76886]">
                    아직 사람이 읽을 수 있는 활동 로그가 없습니다. 신규 요청부터 더 자세히 표시됩니다.
                  </div>
                )}
              </div>
            </div>

            <div className="mt-5 flex items-center justify-between gap-3 px-1">
              <p className="text-xs font-black text-[#a76886]">원본 로그</p>
              <p className="text-xs font-bold text-[#bd7b98]">문제 추적용 상세 로그</p>
            </div>
            <div className="mt-5 max-h-[28rem] overflow-auto rounded-[1.5rem] bg-[#281421] p-4 text-xs font-bold leading-6 text-[#ffe6f1] shadow-inner">
              {logs?.lines.length ? (
                logs.lines.map((line, index) => (
                  <pre key={`${index}-${line}`} className="whitespace-pre-wrap break-words">
                    {line}
                  </pre>
                ))
              ) : (
                <p className="text-[#f6bed5]">{logs?.message ?? "표시할 로그가 없습니다."}</p>
              )}
            </div>
          </section>
        </div>
      </section>
    </main>
  );
}

function MetricCard({
  label,
  value,
  detail,
  icon,
}: {
  label: string;
  value: string;
  detail?: string;
  icon: ReactNode;
}) {
  return (
    <div className="rounded-[1.5rem] bg-white/92 p-4 shadow-[0_14px_30px_rgba(205,79,134,0.08)] ring-1 ring-[#ffd1e3]/70">
      <div className="flex items-center justify-between gap-3 text-[#e75491]">
        <p className="text-xs font-black text-[#a76886]">{label}</p>
        {icon}
      </div>
      <p className="mt-3 text-xl font-black text-[#69324b]">{value}</p>
      {detail && <p className="mt-1 text-xs font-bold text-[#a76886]">{detail}</p>}
    </div>
  );
}
