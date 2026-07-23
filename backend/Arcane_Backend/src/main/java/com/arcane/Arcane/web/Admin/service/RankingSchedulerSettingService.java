package com.arcane.Arcane.web.Admin.service;

import com.arcane.Arcane.web.Admin.domain.AdminSetting;
import com.arcane.Arcane.web.Admin.repository.AdminSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RankingSchedulerSettingService {
    private static final String AUTOMATIC_UPDATE_ENABLED_KEY = "ranking.scheduler.enabled";

    private final AdminSettingRepository adminSettingRepository;

    @Transactional(readOnly = true)
    public boolean loadAutomaticUpdateEnabled(boolean defaultValue) {
        return adminSettingRepository.findById(AUTOMATIC_UPDATE_ENABLED_KEY)
                .map(AdminSetting::getValue)
                .map(Boolean::parseBoolean)
                .orElse(defaultValue);
    }

    @Transactional
    public void saveAutomaticUpdateEnabled(boolean enabled) {
        AdminSetting setting = adminSettingRepository.findById(AUTOMATIC_UPDATE_ENABLED_KEY)
                .orElseGet(() -> new AdminSetting(AUTOMATIC_UPDATE_ENABLED_KEY, Boolean.toString(enabled)));
        setting.updateValue(Boolean.toString(enabled));
        adminSettingRepository.save(setting);
    }
}
