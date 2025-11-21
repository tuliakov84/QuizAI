package com.mipt.dbAPI;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@EnableConfigurationProperties(DatabaseProperties.class)
public class DbServiceConfig {
  
  @Bean
  @Lazy
  public DbService dbService(DatabaseProperties databaseProperties) {
    return new DbService(
        databaseProperties.getUrl(),
        databaseProperties.getUser(),
        databaseProperties.getPassword()
    );
  }
}
