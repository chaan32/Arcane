package com.arcane.Arcane.riot.Ranker.Sheduler;


import com.arcane.Arcane.common.Kafka.producer.RankingUpdateProducer;
import com.arcane.Arcane.common.Logging.ApiLogSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class RankerScheduler {
    private final RankingUpdateProducer rankingUpdateProducer;

    private volatile boolean automaticRankingUpdateEnabled = false;
    private volatile boolean rankingUpdatePublishing = false;
    private volatile LocalDateTime lastStartedAt;
    private volatile LocalDateTime lastFinishedAt;
    private volatile String lastTrigger = "NONE";
    private volatile String lastResult = "NOT_RUN";
    private volatile String lastErrorMessage;

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void scheduleRankingUpdate(){
        if (!automaticRankingUpdateEnabled) {
            log.info(ApiLogSupport.BUSINESS_FLOW, "ranking", "scheduledUpdate", "SKIPPED_DISABLED", "enabled=false");
            return;
        }

        runRankingUpdate("SCHEDULED");
    }

    public String runRankingUpdateNow() {
        return runRankingUpdate("MANUAL");
    }

    private String runRankingUpdate(String trigger) {
        synchronized (this) {
            if (rankingUpdatePublishing) {
                lastResult = "SKIPPED_RUNNING";
                log.warn(ApiLogSupport.BUSINESS_FLOW, "ranking", "updateRequest", "SKIPPED_RUNNING", "trigger=" + trigger);
                return null;
            }

            rankingUpdatePublishing = true;
            lastStartedAt = LocalDateTime.now();
            lastTrigger = trigger;
            lastResult = "PUBLISHING";
            lastErrorMessage = null;
        }

        log.info(ApiLogSupport.BUSINESS_FLOW, "ranking", "updateRequest", "START", "trigger=" + trigger);
        try {
            String jobId = rankingUpdateProducer.requestRankingUpdate(null);
            lastResult = "PUBLISHED";
            log.info(ApiLogSupport.BUSINESS_FLOW, "ranking", "updateRequest", "PUBLISHED", "trigger=" + trigger + ", jobId=" + jobId);
            return jobId;
        } catch (Exception e) {
            lastResult = "FAILED";
            lastErrorMessage = e.getMessage();
            log.error(ApiLogSupport.BUSINESS_FLOW, "ranking", "updateRequest", "FAILED", "trigger=" + trigger + ", reason=" + e.getMessage(), e);
            return null;
        } finally {
            synchronized (this) {
                lastFinishedAt = LocalDateTime.now();
                rankingUpdatePublishing = false;
            }
        }
    }

    public void setAutomaticRankingUpdateEnabled(boolean enabled) {
        this.automaticRankingUpdateEnabled = enabled;
        log.warn(ApiLogSupport.BUSINESS_FLOW, "ranking", "automaticUpdate", "UPDATED", "enabled=" + enabled);
    }

    public boolean isAutomaticRankingUpdateEnabled() {
        return automaticRankingUpdateEnabled;
    }

    public boolean isRankingUpdateRunning() {
        return rankingUpdatePublishing;
    }

    public LocalDateTime getLastStartedAt() {
        return lastStartedAt;
    }

    public LocalDateTime getLastFinishedAt() {
        return lastFinishedAt;
    }

    public String getLastTrigger() {
        return lastTrigger;
    }

    public String getLastResult() {
        return lastResult;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

}
