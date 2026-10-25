package com.mustafa.repository;

import com.mustafa.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IAppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByIdentityNumber(String identityNumber);
    Optional<AppUser> findByKeycloakId(String keycloakId);
    boolean existsByIdentityNumber(String identityNumber);
}
