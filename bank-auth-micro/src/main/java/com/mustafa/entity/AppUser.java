package com.mustafa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Bireyler için TC No, Şirketler için Vergi No
    @Column(unique = true, nullable = false, length = 11)
    private String identityNumber;

    // YENİ KİMLİK KÖPRÜMÜZ: Keycloak'taki müşterinin UUID'sini burada tutacağız.
    @Column(name = "keycloak_id", unique = true)
    private String keycloakId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'PENDING'")
    private ApprovalStatus status = ApprovalStatus.PENDING;

    // Independent from the registration approval status. The database default
    // keeps existing rows unrestricted when Hibernate adds this column.
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "access_restriction_status", nullable = false, length = 24,
            columnDefinition = "varchar(24) default 'UNRESTRICTED'")
    private AccessRestrictionStatus accessRestrictionStatus = AccessRestrictionStatus.UNRESTRICTED;

    // --- ENUMLAR ---
    public enum Role {
        ADMIN,
        RETAIL_CUSTOMER,
        CORPORATE_MANAGER
    }

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }

    public enum AccessRestrictionStatus {
        UNRESTRICTED,
        RESTRICTED,
        REVIEW_PENDING,
        REVIEW_REJECTED
    }
}
