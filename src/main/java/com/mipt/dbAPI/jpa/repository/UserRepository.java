package com.mipt.dbAPI.jpa.repository;

import com.mipt.dbAPI.jpa.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {

  Optional<UserEntity> findBySession(String session);

  Optional<UserEntity> findByUsername(String username);

  boolean existsByUsername(String username);

  List<UserEntity> findByCurrentGame_Id(Integer gameId);

  List<UserEntity> findByCurrentGame_IdOrderByCurrentGamePointsDesc(Integer gameId);

  List<UserEntity> findByCurrentGame_IdOrderByIdAsc(Integer gameId);

  @Query("select count(u.id) from UserEntity u where u.currentGame.id = :gameId")
  long countParticipantsInGame(@Param("gameId") Integer gameId);

  List<UserEntity> findTop100ByOrderByGlobalPointsDescGlobalPossiblePointsAsc();
}
