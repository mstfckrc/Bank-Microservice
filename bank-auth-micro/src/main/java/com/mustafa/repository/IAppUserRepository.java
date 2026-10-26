package com.mustafa.repository;

import com.mustafa.entity.AppUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IAppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByIdentityNumber(String identityNumber);
    Optional<AppUser> findByKeycloakId(String keycloakId);
    boolean existsByIdentityNumber(String identityNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select appUser from AppUser appUser where appUser.identityNumber = :identityNumber")
    Optional<AppUser> findByIdentityNumberForUpdate(@Param("identityNumber") String identityNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select appUser from AppUser appUser where appUser.keycloakId = :keycloakId")
    Optional<AppUser> findByKeycloakIdForUpdate(@Param("keycloakId") String keycloakId);
}
