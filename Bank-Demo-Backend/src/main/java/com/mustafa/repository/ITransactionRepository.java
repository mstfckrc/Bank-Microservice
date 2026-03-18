package com.mustafa.repository;

import com.mustafa.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ITransactionRepository extends JpaRepository<Transaction, Long> {

    // Bir transferin dekontunu referans numarasıyla bulmak için
    Optional<Transaction> findByReferenceNo(String referenceNo);

    // Bir hesabın hem gönderdiği hem de aldığı tüm işlemleri listelemek için
    List<Transaction> findBySenderAccountIdOrReceiverAccountIdOrderByTransactionDateDesc(Long senderAccountId, Long receiverAccountId);
}