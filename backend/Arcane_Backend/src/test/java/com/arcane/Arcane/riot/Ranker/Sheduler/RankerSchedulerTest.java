package com.arcane.Arcane.riot.Ranker.Sheduler;

import com.arcane.Arcane.common.Kafka.producer.RankingUpdateProducer;
import com.arcane.Arcane.web.Admin.service.RankingSchedulerSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RankerSchedulerTest {

    private RankingUpdateProducer rankingUpdateProducer;
    private RankingSchedulerSettingService rankingSchedulerSettingService;
    private RankerScheduler rankerScheduler;

    @BeforeEach
    void setUp() {
        rankingUpdateProducer = mock(RankingUpdateProducer.class);
        rankingSchedulerSettingService = mock(RankingSchedulerSettingService.class);
        rankerScheduler = new RankerScheduler(rankingUpdateProducer, rankingSchedulerSettingService);
    }

    @Test
    void restoresEnabledStateFromPersistentSetting() {
        when(rankingSchedulerSettingService.loadAutomaticUpdateEnabled(false)).thenReturn(true);

        rankerScheduler.restoreAutomaticRankingUpdateEnabled();

        assertThat(rankerScheduler.isAutomaticRankingUpdateEnabled()).isTrue();
    }

    @Test
    void persistsEnabledStateWhenAdministratorChangesIt() {
        rankerScheduler.setAutomaticRankingUpdateEnabled(true);

        verify(rankingSchedulerSettingService).saveAutomaticUpdateEnabled(true);
        assertThat(rankerScheduler.isAutomaticRankingUpdateEnabled()).isTrue();
    }

    @Test
    void exposesOneHourIntervalToTheDashboard() {
        assertThat(rankerScheduler.getIntervalMinutes()).isEqualTo(60);
    }

    @Test
    void runsAtTheBeginningOfEveryHourInKorea() throws NoSuchMethodException {
        Scheduled scheduled = RankerScheduler.class
                .getDeclaredMethod("scheduleRankingUpdate")
                .getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("0 0 * * * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
    }
}
