import { apiJson } from "@/services/apiClient";
import type { MatchTimelineEvent } from "@/types/matchTimeline";

type FetchMatchTimelineParams = {
  matchId: string;
  puuid: string;
  signal?: AbortSignal;
};

const MAX_TIMELINE_CACHE_SIZE = 100;
const timelineByParticipantsCache = new Map<
  string,
  Promise<Record<string, MatchTimelineEvent[]>>
>();

const createAbortError = () => {
  const error = new Error("Aborted");
  error.name = "AbortError";
  return error;
};

const withAbortSignal = <T>(promise: Promise<T>, signal?: AbortSignal) => {
  if (!signal) return promise;
  if (signal.aborted) return Promise.reject(createAbortError());

  return Promise.race([
    promise,
    new Promise<never>((_, reject) => {
      signal.addEventListener("abort", () => reject(createAbortError()), {
        once: true,
      });
    }),
  ]);
};

export const fetchMatchTimeline = ({
  matchId,
  puuid,
  signal,
}: FetchMatchTimelineParams) => {
  const params = new URLSearchParams({ matchId, puuid });

  return apiJson<MatchTimelineEvent[]>(
    `/api/v1/summoner/match/timeline?${params.toString()}`,
    { method: "GET", signal }
  );
};

export const fetchMatchTimelineByParticipants = ({
  matchId,
  signal,
}: Pick<FetchMatchTimelineParams, "matchId" | "signal">) => {
  const cachedRequest = timelineByParticipantsCache.get(matchId);

  if (cachedRequest) {
    return withAbortSignal(cachedRequest, signal);
  }

  const params = new URLSearchParams({ matchId });
  const request = apiJson<Record<string, MatchTimelineEvent[]>>(
    `/api/v1/summoner/match/timeline/all?${params.toString()}`,
    { method: "GET" }
  ).catch((error) => {
    timelineByParticipantsCache.delete(matchId);
    throw error;
  });

  if (timelineByParticipantsCache.size >= MAX_TIMELINE_CACHE_SIZE) {
    const oldestKey = timelineByParticipantsCache.keys().next().value;
    if (oldestKey) timelineByParticipantsCache.delete(oldestKey);
  }

  timelineByParticipantsCache.set(matchId, request);

  return withAbortSignal(request, signal);
};
