package com.clientra.repository;

import com.clientra.entity.AiResponse;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiResponseRepository extends JpaRepository<AiResponse, Integer> {
    @Modifying
    @Query("delete from AiResponse r where r.logId in :ids")
    void deleteByLogIds(@Param("ids") Collection<Integer> ids);
}
