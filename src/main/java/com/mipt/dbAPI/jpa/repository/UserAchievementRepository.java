package com.mipt.dbAPI.jpa.repository;

import com.mipt.dbAPI.jpa.entity.UserAchievementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAchievementRepository extends JpaRepository<UserAchievementEntity, Integer> {

  List<UserAchievementEntity> findByUser_Id(Integer userId);

  List<UserAchievementEntity> findByUser_IdOrderByAchievement_Id(Integer userId);

  boolean existsByUser_IdAndAchievement_Id(Integer userId, Integer achievementId);
}
