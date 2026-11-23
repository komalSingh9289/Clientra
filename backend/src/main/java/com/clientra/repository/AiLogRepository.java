package com.clientra.repository;

import com.clientra.entity.AiLog;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiLogRepository extends JpaRepository<AiLog, Integer> {
    List<AiLog> findAllByOrderByTimestampDesc(Pageable page);
    List<AiLog> findByLeadId(Integer leadId);

    @Query("select g from AiLog g where g.leadId in (select l.leadId from Lead l where l.assignedTo = :uid) order by g.timestamp desc")
    List<AiLog> findForOwner(@Param("uid") Integer uid, Pageable page);
}
