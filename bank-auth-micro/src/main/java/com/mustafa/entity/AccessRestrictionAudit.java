package com.mustafa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "app_user_access_restriction_audit", indexes = {
        @Index(name = "idx_access_restriction_audit_target_time", columnList = "target_app_user_id, occurred_at")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessRestrictionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Numeric local identifiers avoid copying TCKN/VKN or Keycloak tokens to the audit record.
    @Column(name = "target_app_user_id", nullable = false)
    private Long targetAppUserId;

    @Column(name = "actor_app_user_id", nullable = false)
    private Long actorAppUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Action action;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_state", nullable = false, length = 24)
    private AppUser.AccessRestrictionStatus previousState;

    @Enumerated(EnumType.STRING)
    @Column(name = "resulting_state", nullable = false, length = 24)
    private AppUser.AccessRestrictionStatus resultingState;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    public enum Action {
        RESTRICTED,
        REVIEW_REQUESTED,
        REVIEW_APPROVED,
        REVIEW_REJECTED,
        RESTRICTION_LIFTED
    }
}
