package com.mipt.dbAPI;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigration {
  private final JdbcTemplate jdbcTemplate;

  public DatabaseMigration(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @PostConstruct
  public void migrate() {
    jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS email TEXT");
    jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS users_email_idx ON users USING HASH (email)");
  }
}
