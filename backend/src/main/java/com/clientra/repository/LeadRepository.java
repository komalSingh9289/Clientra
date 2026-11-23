package com.clientra.repository;

import com.clientra.entity.Lead;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** JpaSpecificationExecutor lets LeadService build the search/filter query dynamically (still parameterised). */
public interface LeadRepository extends JpaRepository<Lead, Integer>, JpaSpecificationExecutor<Lead> {
    List<Lead> findByAssignedTo(Integer userId);
    boolean existsByStageId(Integer stageId);
    long countByFlaggedTrue();
    List<Lead> findByEmailIgnoreCase(String email);
    /** Phone comparison ignores spaces, so "+91 98100 00000" equals "+919810000000". */
    @Query("select l from Lead l where replace(l.phone, ' ', '') = :phone")
    List<Lead> findByNormalizedPhone(@Param("phone") String phone);
}
