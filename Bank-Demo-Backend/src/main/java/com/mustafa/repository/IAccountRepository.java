package com.mustafa.repository;

import com.mustafa.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IAccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByIban(String iban);

    // 🚀 YENİ MİMARİ: Kasaları sahibinin TC kimlik numarasına göre bul!
    List<Account> findByOwnerIdentityNumber(String ownerIdentityNumber);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByIban(String iban);
}