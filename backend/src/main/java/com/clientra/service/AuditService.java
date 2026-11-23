package com.clientra.service;

import com.clientra.dto.Dtos.AuditDto;
import com.clientra.entity.AuditLog;
import com.clientra.repository.AuditRepository;
import com.clientra.security.AuthUser;
import com.clientra.security.CurrentUser;
import com.clientra.util.TimeUtil;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Append-only audit trail (synopsis 11.6). Other services call log(...) for every sensitive
 * operation: login/logout, lead create/update/delete, role changes, imports/exports, config changes.
 * It records WHO (user id + name), WHAT (action), WHICH record and WHEN.
 */
@Service
public class AuditService {
    private final AuditRepository repo;
    private final CurrentUser current;

    public AuditService(AuditRepository repo, CurrentUser current) {
        this.repo = repo;
        this.current = current;
    }

    /** Logs on behalf of whoever is signed in (or "System" when there is no request, e.g. the scheduler). */
    public void log(String action, String entity, String detail) {
        AuthUser u = current.getOrNull();
        logAs(u == null ? null : u.userId(), u == null ? "System" : u.fullName(), action, entity, detail);
    }

    /** Logs with an explicit actor (used by login, where no one is authenticated yet). */
    public void logAs(Integer userId, String userName, String action, String entity, String detail) {
        AuditLog a = new AuditLog();
        a.setUserId(userId);
        a.setUserName(userName);
        a.setAction(action);
        a.setEntity(entity);
        a.setDetail(detail);
        a.setTimestamp(Instant.now());
        repo.save(a);
    }

    /** Newest first, optional action and date-range filters, capped at 1000 rows. */
    @Transactional(readOnly = true)
    public List<AuditDto> search(String action, LocalDate from, LocalDate to) {
        Specification<AuditLog> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (action != null && !action.isBlank()) p.add(cb.equal(root.get("action"), action));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.<Instant>get("timestamp"), TimeUtil.startOfDay(from)));
            if (to != null) p.add(cb.lessThanOrEqualTo(root.<Instant>get("timestamp"), TimeUtil.endOfDay(to)));
            return cb.and(p.toArray(new Predicate[0]));
        };
        List<AuditLog> rows = repo.findAll(spec, PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "timestamp"))).getContent();
        List<AuditDto> out = new ArrayList<>();
        for (AuditLog a : rows) {
            out.add(new AuditDto(a.getId(), a.getUserId(), a.getUserName(), a.getAction(), a.getEntity(), a.getDetail(), a.getTimestamp()));
        }
        return out;
    }
}
