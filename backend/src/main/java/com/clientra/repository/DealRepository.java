package com.clientra.repository;

import com.clientra.entity.Deal;
import com.clientra.entity.DealStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DealRepository extends JpaRepository<Deal, Integer> {
    List<Deal> findByLeadId(Integer leadId);

    /** [leadId, sum(amount)] of the OPEN deals of every lead. */
    @Query("select d.leadId, sum(d.amount) from Deal d where d.status = :status group by d.leadId")
    List<Object[]> sumByLeadForStatus(@Param("status") DealStatus status);

    @Query("select d from Deal d where d.leadId in (select l.leadId from Lead l where l.assignedTo = :uid)")
    List<Deal> findForOwner(@Param("uid") Integer uid);

    @Modifying
    @Query("delete from Deal d where d.leadId = :leadId")
    void deleteByLeadId(@Param("leadId") Integer leadId);
}
