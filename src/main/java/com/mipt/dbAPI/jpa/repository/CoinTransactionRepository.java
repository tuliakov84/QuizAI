package com.mipt.dbAPI.jpa.repository;

import com.mipt.dbAPI.jpa.entity.CoinTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoinTransactionRepository extends JpaRepository<CoinTransactionEntity, Integer> {
  List<CoinTransactionEntity> findByUser_IdOrderByIdDesc(Integer userId);
}
