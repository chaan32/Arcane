package com.arcane.Arcane.web.Admin.service;

import com.arcane.Arcane.web.Admin.domain.AdminSetting;
import com.arcane.Arcane.web.Admin.repository.AdminSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RankingSchedulerSettingServiceTest {

    private AdminSettingRepository adminSettingRepository;
    private RankingSchedulerSettingService service;

    @BeforeEach
    void setUp() {
        adminSettingRepository = mock(AdminSettingRepository.class);
        service = new RankingSchedulerSettingService(adminSettingRepository);
    }

    @Test
    void returnsSavedEnabledState() {
        when(adminSettingRepository.findById("ranking.scheduler.enabled"))
                .thenReturn(Optional.of(new AdminSetting("ranking.scheduler.enabled", "true")));

        boolean enabled = service.loadAutomaticUpdateEnabled(false);

        assertThat(enabled).isTrue();
    }

    @Test
    void returnsDefaultStateWhenSettingDoesNotExist() {
        when(adminSettingRepository.findById("ranking.scheduler.enabled"))
                .thenReturn(Optional.empty());

        boolean enabled = service.loadAutomaticUpdateEnabled(false);

        assertThat(enabled).isFalse();
    }

    @Test
    void savesEnabledStateUsingStableSettingKey() {
        when(adminSettingRepository.findById("ranking.scheduler.enabled"))
                .thenReturn(Optional.empty());
        ArgumentCaptor<AdminSetting> settingCaptor = ArgumentCaptor.forClass(AdminSetting.class);

        service.saveAutomaticUpdateEnabled(true);

        verify(adminSettingRepository).save(settingCaptor.capture());
        assertThat(settingCaptor.getValue().getKey()).isEqualTo("ranking.scheduler.enabled");
        assertThat(settingCaptor.getValue().getValue()).isEqualTo("true");
    }
}
