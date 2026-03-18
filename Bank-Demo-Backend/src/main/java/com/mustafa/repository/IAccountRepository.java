package com.mustafa.repository;

import com.mustafa.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IAccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByIban(String iban);

    // Artık hesabı Customer ID'sine göre değil, AppUser ID'sine göre arıyoruz
    List<Account> findByAppUserId(Long appUserId);

    // Veya direkt kimlik numarasıyla bulmak için (Servislerde çok işimize yarayacak)
    List<Account> findByAppUser_IdentityNumber(String identityNumber);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByIban(String iban);
}