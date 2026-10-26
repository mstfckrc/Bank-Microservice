package com.mustafa.repository;

import com.mustafa.entity.AccessRestrictionAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IAccessRestrictionAuditRepository extends JpaRepository<AccessRestrictionAudit, Long> {
}
