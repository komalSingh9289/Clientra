package com.clientra.repository;

import com.clientra.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditRepository extends JpaRepository<AuditLog, Integer>, JpaSpecificationExecutor<AuditLog> {
}
