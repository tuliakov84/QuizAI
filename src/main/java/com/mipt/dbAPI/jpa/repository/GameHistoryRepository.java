package com.mipt.dbAPI.jpa.repository;

import com.mipt.dbAPI.jpa.entity.GameHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameHistoryRepository extends JpaRepository<GameHistoryEntity, Integer> {

  List<GameHistoryEntity> findByUser_IdOrderByIdAsc(Integer userId);
}
