package com.clientra.service;

import com.clientra.dto.Dtos.RulesDto;
import com.clientra.entity.NotificationRules;
import com.clientra.repository.NotificationRulesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** E-mail notification rules configured by the administrator (Module 8). */
@Service
public class SettingsService {
    private final NotificationRulesRepository repo;
    private final AuditService audit;

    public SettingsService(NotificationRulesRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    /** The single settings row (created with defaults on first use). */
    @Transactional
    public NotificationRules rules() {
        return repo.findById(1).orElseGet(() -> repo.save(new NotificationRules()));
    }

    @Transactional(readOnly = true)
    public RulesDto get() {
        NotificationRules r = repo.findById(1).orElseGet(NotificationRules::new);
        return toDto(r);
    }

    @Transactional
    public RulesDto save(RulesDto d) {
        NotificationRules r = rules();
        r.setEmailOnDue(d.emailOnDue());
        r.setEmailOnOverdue(d.emailOnOverdue());
        r.setEmailOnNegative(d.emailOnNegative());
        r.setWeeklyDigest(d.weeklyDigest());
        repo.save(r);
        audit.log("CONFIG", "Notification rules", "due=" + d.emailOnDue() + ", overdue=" + d.emailOnOverdue()
                + ", negative=" + d.emailOnNegative() + ", digest=" + d.weeklyDigest());
        return toDto(r);
    }

    private RulesDto toDto(NotificationRules r) {
        return new RulesDto(r.isEmailOnDue(), r.isEmailOnOverdue(), r.isEmailOnNegative(), r.isWeeklyDigest());
    }
}
