package com.mipt.dbAPI.jpa.repository;

import com.mipt.dbAPI.jpa.entity.TopicEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRepository extends JpaRepository<TopicEntity, Integer> {

  List<TopicEntity> findAllByOrderByIdAsc();
}
