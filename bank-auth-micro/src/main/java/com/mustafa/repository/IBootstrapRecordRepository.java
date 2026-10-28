package com.mustafa.repository;

import com.mustafa.entity.BootstrapRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IBootstrapRecordRepository extends JpaRepository<BootstrapRecord, String> {
}
