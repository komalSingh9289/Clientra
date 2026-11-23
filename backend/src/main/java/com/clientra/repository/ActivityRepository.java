package com.clientra.repository;

import com.clientra.entity.Activity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityRepository extends JpaRepository<Activity, Integer> {
    List<Activity> findByLeadIdOrderByActivityDateDesc(Integer leadId);
    List<Activity> findAllByOrderByActivityDateDesc(Pageable page);

    /** Activities of the leads owned by one user (data scope of a Sales Representative). */
    @Query("select a from Activity a where a.leadId in (select l.leadId from Lead l where l.assignedTo = :uid) order by a.activityDate desc")
    List<Activity> findForOwner(@Param("uid") Integer uid, Pageable page);

    /** [leadId, latest activity time] for every lead that has activity; used to compute "idle days". */
    @Query("select a.leadId, max(a.activityDate) from Activity a group by a.leadId")
    List<Object[]> lastActivityByLead();

    long countByUserId(Integer userId);

    @Modifying
    @Query("delete from Activity a where a.leadId = :leadId")
    void deleteByLeadId(@Param("leadId") Integer leadId);
}
