package com.mipt.dbAPI.jpa.repository;

import com.mipt.dbAPI.jpa.entity.AnsweredCorrectlyQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnsweredCorrectlyQuestionRepository extends JpaRepository<AnsweredCorrectlyQuestionEntity, Integer> {

  List<AnsweredCorrectlyQuestionEntity> findByUser_IdOrderByIdAsc(Integer userId);

  boolean existsByUser_IdAndQuestion_Id(Integer userId, Integer questionId);
}
