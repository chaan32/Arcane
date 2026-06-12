import {
  getAuthToken,
  isAuthErrorResponse,
  signOutForAuthError,
} from "@/lib/mockAuth";
import { apiFetch } from "@/services/apiClient";

export interface RankingSchedulerStatus {
  enabled: boolean;
  running: boolean;
  intervalMinutes: number;
  lastStartedAt: string | null;
  lastFinishedAt: string | null;
  lastTrigger: string;
  lastResult: string;
  lastErrorMessage: string | null;
  latestWorkerJob?: RankingUpdateJobStatus | null;
  recentWorkerJobs?: RankingUpdateJobStatus[];
}

export interface RankingUpdateJobStatus {
  jobId: string;
  status: string;
  requestedBy: number | null;
  requestedAt: string | null;
  completedAt: string | null;
  failedAt: string | null;
  message: string | null;
  traceId: string | null;
}

export interface ServerStatus {
  generatedAt: string;
  uptimeMs: number;
  availableProcessors: number;
  memory: {
    heapUsedBytes: number;
    heapCommittedBytes: number;
    heapMaxBytes: number;
    nonHeapUsedBytes: number;
    nonHeapCommittedBytes: number;
    nonHeapMaxBytes: number;
  };
  threads: {
    live: number;
    daemon: number;
    peak: number;
  };
  cpu: {
    systemLoadAverage?: number;
    processCpuLoad?: number;
    systemCpuLoad?: number;
    committedVirtualMemoryBytes?: number;
  };
  http: {
    requestCount: number;
    averageMs: number;
    maxMs: number;
  };
  monitoring: {
    prometheusUrl: string;
    metricsUrl: string;
    healthUrl: string;
  };
}

export interface DatasetCollectJobStatus {
  jobId: string;
  status: string;
  phase: string;
  message: string | null;
  traceId: string | null;
  rankingKey: string | null;
  rankerLimit: number;
  matchCount: number;
  queueId: number;
  totalPuuids: number;
  processedPuuids: number;
  candidateMatchIds: number;
  uniqueMatchIds: number;
  processedMatches: number;
  savedMysqlMatches: number;
  savedMongoParticipants: number;
  skippedDuplicateMatches: number;
  failedMatches: number;
  retryCount: number;
  requestedAt: string | null;
  startedAt: string | null;
  completedAt: string | null;
  failedAt: string | null;
  updatedAt: string | null;
}

export interface DatasetCollectionStatus {
  latestJob: DatasetCollectJobStatus | null;
  recentJobs: DatasetCollectJobStatus[];
}

export interface ChampionAnalysisJobStatus {
  jobId: string;
  status: string;
  requestedBy: number | null;
  requestedAt: string | null;
  completedAt: string | null;
  failedAt: string | null;
  message: string | null;
  traceId: string | null;
  snapshotId: string | null;
  progressPercent: number;
  phase: string | null;
  totalParticipants: number;
  totalMatches: number;
  totalChampions: number;
  patchVersion: string | null;
  sourceCollection: string | null;
  queueId: number | null;
}

export interface ChampionAnalysisStatus {
  latestJob: ChampionAnalysisJobStatus | null;
  recentJobs: ChampionAnalysisJobStatus[];
}

export interface GameDataSyncJobStatus {
  jobId: string;
  status: string;
  requestedBy: number | null;
  requestedAt: string | null;
  completedAt: string | null;
  failedAt: string | null;
  message: string | null;
  traceId: string | null;
  version: string | null;
  championCount: number;
  itemCount: number;
  summonerSpellCount: number;
  runeCount: number;
}

export interface GameDataSyncStatus {
  latestJob: GameDataSyncJobStatus | null;
  recentJobs: GameDataSyncJobStatus[];
}

export interface AdminLogs {
  files: AdminLogFile[];
  selectedFileName: string | null;
  filePath: string;
  maxLines: number;
  lines: string[];
  message: string;
}

export interface AdminLogFile {
  fileName: string;
  filePath: string;
  displayName: string;
  source: "api" | "worker";
  serverName: string;
  active: boolean;
  compressed: boolean;
  sizeBytes: number;
  lastModifiedAt: string;
}

export interface AdminDashboard {
  rankingScheduler: RankingSchedulerStatus;
  datasetCollection: DatasetCollectionStatus;
  championAnalysis: ChampionAnalysisStatus;
  gameDataSync: GameDataSyncStatus;
  serverStatus: ServerStatus;
  logs: AdminLogs;
}

export interface RankingUpdateRequestResponse {
  jobId: string;
  message: string;
}

export interface DatasetCollectRequestResponse {
  jobId: string;
  message: string;
}

export interface DatasetCollectRequest {
  rankingKey: string;
  rankerLimit: number;
  matchCount: number;
  queueId: number;
}

export interface ChampionAnalysisRequestResponse {
  jobId: string;
  message: string;
}

export interface GameDataSyncRequestResponse {
  jobId: string;
  message: string;
}

const adminJson = async <T>(path: string, init?: RequestInit): Promise<T> => {
  const token = getAuthToken();
  if (!token) {
    throw new Error("로그인이 필요합니다.");
  }

  const response = await apiFetch(path, {
    ...init,
    headers: {
      ...(init?.headers ?? {}),
      Authorization: `Bearer ${token}`,
    },
  });

  const data = (await response.json().catch(() => ({}))) as {
    code?: string;
    message?: string;
  };

  if (isAuthErrorResponse(response.status, data)) {
    signOutForAuthError();
    throw new Error(data.message ?? "로그인이 만료되었습니다. 다시 로그인해주세요.");
  }

  if (!response.ok) {
    throw new Error(data.message ?? `관리자 API 요청에 실패했습니다. (${response.status})`);
  }

  return data as T;
};

export const fetchAdminDashboard = () =>
  adminJson<AdminDashboard>("/api/v1/admin/dashboard");

export const toggleRankingScheduler = (enabled: boolean) =>
  adminJson<RankingSchedulerStatus>("/api/v1/admin/ranking-scheduler", {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ enabled }),
  });

export const requestRankingUpdate = () =>
  adminJson<RankingUpdateRequestResponse>("/api/v1/admin/ranking-update", {
    method: "POST",
  });

export const requestDatasetCollection = (request: DatasetCollectRequest) =>
  adminJson<DatasetCollectRequestResponse>("/api/v1/admin/dataset-collection", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

export const requestChampionAnalysis = () =>
  adminJson<ChampionAnalysisRequestResponse>("/api/v1/admin/champion-analysis", {
    method: "POST",
  });

export const requestGameDataSync = () =>
  adminJson<GameDataSyncRequestResponse>("/api/v1/admin/game-data-sync", {
    method: "POST",
  });

export const fetchAdminLogs = (lines = 200, fileName?: string | null) => {
  const params = new URLSearchParams({ lines: String(lines) });
  if (fileName) {
    params.set("fileName", fileName);
  }

  return adminJson<AdminLogs>(`/api/v1/admin/logs?${params.toString()}`);
};
