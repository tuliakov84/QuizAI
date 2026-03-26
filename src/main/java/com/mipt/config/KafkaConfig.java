package com.mipt.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@EnableKafka
@Configuration
public class KafkaConfig {

  /**
   * Создаёт топики при старте приложения (локальный брокер с replication factor 1).
   * Имена совпадают с {@code application.properties}.
   */
  @Bean
  public NewTopic questionGenerationRequestsTopic(
      @Value("${app.kafka.topic.question-generation-requests}") String name
  ) {
    return TopicBuilder.name(name).partitions(3).replicas(1).build();
  }

  @Bean
  public NewTopic questionGenerationResultsTopic(
      @Value("${app.kafka.topic.question-generation-results}") String name
  ) {
    return TopicBuilder.name(name).partitions(3).replicas(1).build();
  }

  /**
   * Топик для Python embedding_service (массив id вопросов), чтобы стек поднимался без ручного kafka-topics.
   */
  @Bean
  public NewTopic embeddingRequestsTopic(
      @Value("${app.kafka.topic.embedding-requests}") String name
  ) {
    return TopicBuilder.name(name).partitions(3).replicas(1).build();
  }
}
