package com.mipt.dbAPI.jpa.repository;

import com.mipt.dbAPI.jpa.entity.AchievementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AchievementRepository extends JpaRepository<AchievementEntity, Integer> {

  @Query("""
      select a.id
      from AchievementEntity a
      where a.profilePicNeeded = :profilePicNeeded
        and a.descriptionNeeded = :descriptionNeeded
        and a.gamesNumberNeeded >= :gamesNumberNeeded
        and a.globalPointsNeeded >= :globalPointsNeeded
        and a.globalRatingPlaceNeeded >= :globalRatingPlaceNeeded
        and a.currentGamePointsNeeded >= :currentGamePointsNeeded
        and a.currentGameRatingNeeded >= :currentGameRatingNeeded
        and a.currentGameLevelDifficultyNeeded >= :currentGameLevelDifficultyNeeded
        and not exists (
          select 1
          from UserAchievementEntity ua
          where ua.user.id = :userId and ua.achievement.id = a.id
        )
      order by a.id
      """)
  List<Integer> findQualifiedUnattachedAchievementIds(
      @Param("userId") Integer userId,
      @Param("profilePicNeeded") boolean profilePicNeeded,
      @Param("descriptionNeeded") boolean descriptionNeeded,
      @Param("gamesNumberNeeded") int gamesNumberNeeded,
      @Param("globalPointsNeeded") int globalPointsNeeded,
      @Param("globalRatingPlaceNeeded") int globalRatingPlaceNeeded,
      @Param("currentGamePointsNeeded") int currentGamePointsNeeded,
      @Param("currentGameRatingNeeded") int currentGameRatingNeeded,
      @Param("currentGameLevelDifficultyNeeded") int currentGameLevelDifficultyNeeded
  );
}
