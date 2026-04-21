package com.mipt.dbAPI.jpa.repository;

import com.mipt.dbAPI.jpa.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Integer> {

  long countByGame_Id(Integer gameId);

  Optional<QuestionEntity> findByGame_IdAndQuestionNumber(Integer gameId, int questionNumber);

  List<QuestionEntity> findByGame_IdAndIdIn(Integer gameId, List<Integer> ids);

  List<QuestionEntity> findByGame_IdOrderByQuestionNumberAscIdAsc(Integer gameId);
}
