package com.clientra.repository;

import com.clientra.entity.Reminder;
import com.clientra.entity.ReminderStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderRepository extends JpaRepository<Reminder, Integer> {
    List<Reminder> findByUserIdOrderByReminderTimeAsc(Integer userId);
    List<Reminder> findAllByOrderByReminderTimeAsc();
    /** Pending reminders whose time has passed (dashboard "overdue", WebSocket catch-up on connect). */
    List<Reminder> findByStatusAndReminderTimeLessThanEqual(ReminderStatus status, Instant time);
    List<Reminder> findByUserIdAndStatusAndReminderTimeLessThanEqual(Integer userId, ReminderStatus status, Instant time);
    /** Scheduler queries: alerts that have not been sent yet. */
    List<Reminder> findByStatusAndDueNotifiedFalseAndReminderTimeLessThanEqual(ReminderStatus status, Instant time);
    List<Reminder> findByStatusAndOverdueNotifiedFalseAndReminderTimeLessThanEqual(ReminderStatus status, Instant time);

    @Modifying
    @Query("delete from Reminder r where r.leadId = :leadId")
    void deleteByLeadId(@Param("leadId") Integer leadId);
}
