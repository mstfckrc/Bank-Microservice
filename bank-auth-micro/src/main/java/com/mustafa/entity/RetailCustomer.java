package com.mustafa.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "retail_customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetailCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "app_user_id", nullable = false, unique = true)
    private AppUser appUser;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(unique = true, nullable = false, length = 100)
    private String email;
}