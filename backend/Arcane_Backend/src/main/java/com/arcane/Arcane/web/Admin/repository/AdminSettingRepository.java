package com.arcane.Arcane.web.Admin.repository;

import com.arcane.Arcane.web.Admin.domain.AdminSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminSettingRepository extends JpaRepository<AdminSetting, String> {
}
