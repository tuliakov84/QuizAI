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
        and coalesce(a.gamesNumberNeeded, 0) >= :gamesNumberNeeded
        and coalesce(a.globalPointsNeeded, 0) >= :globalPointsNeeded
        and coalesce(a.globalRatingPlaceNeeded, 0) >= :globalRatingPlaceNeeded
        and coalesce(a.currentGamePointsNeeded, 0) >= :currentGamePointsNeeded
        and coalesce(a.currentGameRatingNeeded, 0) >= :currentGameRatingNeeded
        and coalesce(a.currentGameLevelDifficultyNeeded, 0) >= :currentGameLevelDifficultyNeeded
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
