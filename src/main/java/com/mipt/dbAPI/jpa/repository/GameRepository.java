package com.mipt.dbAPI.jpa.repository;

import com.mipt.dbAPI.jpa.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<GameEntity, Integer> {

  Optional<GameEntity> findByIdAndStatus(Integer id, int status);

  List<GameEntity> findByIsPrivateFalseAndStatusOrderByIdAsc(int status);

  List<GameEntity> findByIsPrivateFalseAndStatusAndTopic_IdOrderByIdAsc(int status, Integer topicId);
}
