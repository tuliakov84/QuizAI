package com.mipt.dbAPI.jpa.repository;

import com.mipt.dbAPI.jpa.entity.CoinTransactionEntity;
import com.mipt.bank.CoinTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface CoinTransactionRepository extends JpaRepository<CoinTransactionEntity, Integer> {
  List<CoinTransactionEntity> findByUser_IdOrderByIdDesc(Integer userId);

  @Query("""
      select coalesce(sum(c.amountDelta), 0)
      from CoinTransactionEntity c
      where c.user.id = :userId
        and c.transactionType = :transactionType
        and c.createdAt >= :from
        and c.createdAt < :to
      """)
  Integer sumAmountDeltaByUserAndTypeBetween(
      @Param("userId") Integer userId,
      @Param("transactionType") CoinTransactionType transactionType,
      @Param("from") Timestamp from,
      @Param("to") Timestamp to
  );
}
