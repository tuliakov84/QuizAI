package com.mipt;

import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.Achievement;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DbServicePerformanceTest {

  //
  // INITIALIZING CONTAINER
  //

  @Container
  private static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:13-alpine")
          .withDatabaseName("testdb")
          .withUsername("testuser")
          .withPassword("testpass")
          .withInitScript("init.sql")
          .withReuse(true)
          .waitingFor(Wait.forListeningPort())
          .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 1))
          .withStartupTimeout(Duration.ofSeconds(90));

  static DbService dbService;

  @BeforeAll
  static void setUp() {
    assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
    String jdbcUrl = postgres.getJdbcUrl();
    String username = postgres.getUsername();
    String password = postgres.getPassword();

    dbService = new DbService(jdbcUrl, username, password);
  }

  @AfterEach
  void tearDown() throws SQLException {
    dbService.eraseAllData("DELETE_ALL_RECORDS_IN_DATABASE");
  }

  // Helper method to print performance metrics
  private void printPerformanceMetrics(String testName, long totalTimeMs, int operations, double throughput) {
    System.out.println("\n=== " + testName + " ===");
    System.out.println("Total time: " + totalTimeMs + " ms");
    System.out.println("Operations: " + operations);
    System.out.println("Throughput: " + String.format("%.2f", throughput) + " ops/sec");
    System.out.println("Average latency: " + String.format("%.2f", (double) totalTimeMs / operations) + " ms");
  }

  //
  // PERFORMANCE TESTS
  //

  @Test
  @Order(1)
  void testConcurrentUserRegistration() throws InterruptedException {
    // Тест производительности регистрации пользователей
    // Проверяем выдержку не менее 100 пользователей в минуту (≈1.67 ops/sec)
    int numberOfUsers = 120; // Тестируем 120 пользователей для проверки 100+ в минуту
    int threadPoolSize = 30;
    ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
    CountDownLatch latch = new CountDownLatch(numberOfUsers);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger errorCount = new AtomicInteger(0);

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < numberOfUsers; i++) {
      final int userId = i;
      executor.submit(() -> {
        try {
          dbService.register("user" + userId, "password" + userId);
          successCount.incrementAndGet();
        } catch (Exception e) {
          errorCount.incrementAndGet();
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await(60, TimeUnit.SECONDS);
    long endTime = System.currentTimeMillis();
    executor.shutdown();

    long totalTime = endTime - startTime;
    double throughput = (successCount.get() * 1000.0) / totalTime;
    double usersPerMinute = (successCount.get() * 60000.0) / totalTime;

    printPerformanceMetrics("Concurrent User Registration", totalTime, successCount.get(), throughput);
    System.out.println("Users per minute: " + String.format("%.2f", usersPerMinute));

    assertTrue(successCount.get() >= 100, "At least 100 registrations should succeed");
    assertTrue(throughput >= 1.67, "Throughput should be at least 1.67 ops/sec (100 users/min)");
    assertTrue(usersPerMinute >= 100, "Should handle at least 100 users per minute");
  }

  @Test
  @Order(2)
  void testConcurrentUserAuthorization() throws InterruptedException, SQLException, DatabaseAccessException {
    // Тест производительности авторизации пользователей
    // Проверяем выдержку не менее 100 пользователей в минуту
    int numberOfUsers = 120;
    int threadPoolSize = 30;

    // Подготовка: регистрируем пользователей
    for (int i = 0; i < numberOfUsers; i++) {
      dbService.register("authUser" + i, "pass" + i);
    }

    ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
    CountDownLatch latch = new CountDownLatch(numberOfUsers);
    AtomicInteger successCount = new AtomicInteger(0);

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < numberOfUsers; i++) {
      final int userId = i;
      executor.submit(() -> {
        try {
          dbService.authorize("authUser" + userId, "pass" + userId, "session" + userId);
          successCount.incrementAndGet();
        } catch (Exception e) {
          // Ignore errors for performance test
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await(60, TimeUnit.SECONDS);
    long endTime = System.currentTimeMillis();
    executor.shutdown();

    long totalTime = endTime - startTime;
    double throughput = (successCount.get() * 1000.0) / totalTime;
    double usersPerMinute = (successCount.get() * 60000.0) / totalTime;

    printPerformanceMetrics("Concurrent User Authorization", totalTime, successCount.get(), throughput);
    System.out.println("Users per minute: " + String.format("%.2f", usersPerMinute));

    assertTrue(successCount.get() >= 100, "At least 100 authorizations should succeed");
    assertTrue(throughput >= 1.67, "Throughput should be at least 1.67 ops/sec (100 users/min)");
    assertTrue(usersPerMinute >= 100, "Should handle at least 100 users per minute");
  }

  @Test
  @Order(3)
  void testFrequentSessionValidation() throws InterruptedException, SQLException, DatabaseAccessException {
    // Тест производительности частых проверок сессий (getUserId)
    int numberOfUsers = 30;
    int operationsPerUser = 100;

    // Подготовка: регистрируем и авторизуем пользователей
    for (int i = 0; i < numberOfUsers; i++) {
      dbService.register("sessionUser" + i, "pass" + i);
      dbService.authorize("sessionUser" + i, "pass" + i, "session" + i);
    }

    ExecutorService executor = Executors.newFixedThreadPool(20);
    CountDownLatch latch = new CountDownLatch(numberOfUsers * operationsPerUser);
    AtomicInteger successCount = new AtomicInteger(0);

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < numberOfUsers; i++) {
      final int userId = i;
      for (int j = 0; j < operationsPerUser; j++) {
        executor.submit(() -> {
          try {
            Integer id = dbService.getUserId("session" + userId);
            if (id != null) {
              successCount.incrementAndGet();
            }
          } catch (Exception e) {
            // Ignore errors
          } finally {
            latch.countDown();
          }
        });
      }
    }

    latch.await(60, TimeUnit.SECONDS);
    long endTime = System.currentTimeMillis();
    executor.shutdown();

    long totalTime = endTime - startTime;
    double throughput = (successCount.get() * 1000.0) / totalTime;

    printPerformanceMetrics("Frequent Session Validation (getUserId)", totalTime, successCount.get(), throughput);

    // Проверка сессий должна быть быстрой, минимум 100 ops/sec
    // Это соответствует проверке сессий для 100+ пользователей в минуту
    assertTrue(throughput >= 100, "Session validation should be fast, at least 100 ops/sec");
  }

  @Test
  @Order(4)
  void testConcurrentGameCreation() throws InterruptedException, SQLException, DatabaseAccessException {
    // Тест производительности создания игр
    // Проверяем выдержку не менее 100 операций в минуту
    int numberOfGames = 120;
    int threadPoolSize = 30;

    // Подготовка: регистрируем пользователей
    for (int i = 0; i < numberOfGames; i++) {
      dbService.register("gameCreator" + i, "pass" + i);
      dbService.authorize("gameCreator" + i, "pass" + i, "creatorSession" + i);
    }

    ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
    CountDownLatch latch = new CountDownLatch(numberOfGames);
    AtomicInteger successCount = new AtomicInteger(0);
    List<Integer> createdGameIds = new CopyOnWriteArrayList<>();

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < numberOfGames; i++) {
      final int gameIndex = i;
      executor.submit(() -> {
        try {
          Integer gameId = dbService.createGame("creatorSession" + gameIndex, 1, 10, 4, 1);
          if (gameId != null) {
            createdGameIds.add(gameId);
            successCount.incrementAndGet();
          }
        } catch (Exception e) {
          // Ignore errors
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await(60, TimeUnit.SECONDS);
    long endTime = System.currentTimeMillis();
    executor.shutdown();

    long totalTime = endTime - startTime;
    double throughput = (successCount.get() * 1000.0) / totalTime;

    printPerformanceMetrics("Concurrent Game Creation", totalTime, successCount.get(), throughput);

    // Создание игр должно поддерживать минимум 100 операций в минуту
    double gamesPerMinute = (successCount.get() * 60000.0) / totalTime;
    System.out.println("Games per minute: " + String.format("%.2f", gamesPerMinute));
    
    assertTrue(successCount.get() >= 100, "At least 100 games should be created");
    assertTrue(throughput >= 1.67, "Throughput should be at least 1.67 ops/sec (100 ops/min)");
    assertTrue(gamesPerMinute >= 100, "Should handle at least 100 game creations per minute");
  }

  @Test
  @Order(5)
  void testBulkQuestionLoading() throws SQLException, DatabaseAccessException {
    // Тест производительности загрузки большого количества вопросов
    dbService.register("questionLoader", "pass");
    dbService.authorize("questionLoader", "pass", "loaderSession");
    Integer gameId = dbService.createGame("loaderSession", 1, 100, 4, 1);

    // Создаем JSON массив с вопросами
    JSONArray questions = new JSONArray();
    for (int i = 1; i <= 100; i++) {
      JSONObject question = new JSONObject();
      question.put("question_number", i);
      question.put("question_text", "Question " + i + "?");
      question.put("right_answer_number", (i % 4) + 1);

      JSONArray answers = new JSONArray();
      for (int j = 1; j <= 4; j++) {
        JSONObject answer = new JSONObject();
        answer.put("index", j);
        answer.put("answer", "Answer " + j + " for question " + i);
        answers.put(answer);
      }
      question.put("available_answers", answers);
      questions.put(question);
    }

    long startTime = System.currentTimeMillis();
    dbService.loadQuestions(gameId, questions);
    long endTime = System.currentTimeMillis();

    long totalTime = endTime - startTime;
    double throughput = (100.0 * 1000.0) / totalTime;

    printPerformanceMetrics("Bulk Question Loading (100 questions)", totalTime, 100, throughput);

    // Загрузка вопросов должна быть достаточно быстрой для поддержки 100+ игр в минуту
    // Каждая игра требует загрузки вопросов, поэтому это критично
    double questionsPerMinute = (100.0 * 60000.0) / totalTime;
    System.out.println("Questions per minute: " + String.format("%.2f", questionsPerMinute));
    
    assertTrue(totalTime < 5000, "Loading 100 questions should take less than 5 seconds");
    assertTrue(throughput >= 20, "Throughput should be at least 20 questions/sec");
    // Если загрузка 100 вопросов занимает меньше минуты, это соответствует 100+ операциям в минуту
    assertTrue(questionsPerMinute >= 100 || totalTime < 60000, 
        "Should be able to load questions for at least 100 games per minute");
  }

  @Test
  @Order(6)
  void testFrequentPointsUpdates() throws InterruptedException, SQLException, DatabaseAccessException {
    // Тест производительности частых обновлений очков
    int numberOfUsers = 20;
    int updatesPerUser = 50;

    // Подготовка: регистрируем пользователей и создаем игру
    dbService.register("pointsGameOwner", "pass");
    dbService.authorize("pointsGameOwner", "pass", "pointsOwnerSession");
    Integer gameId = dbService.createGame("pointsOwnerSession", 1, 10, 20, 1);
    for (int i = 0; i < numberOfUsers; i++) {
      dbService.register("pointsUser" + i, "pass" + i);
      dbService.authorize("pointsUser" + i, "pass" + i, "pointsSession" + i);
      dbService.setCurrentGame("pointsSession" + i, gameId);
    }

    ExecutorService executor = Executors.newFixedThreadPool(20);
    CountDownLatch latch = new CountDownLatch(numberOfUsers * updatesPerUser);
    AtomicInteger successCount = new AtomicInteger(0);

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < numberOfUsers; i++) {
      final int userId = i;
      for (int j = 0; j < updatesPerUser; j++) {
        executor.submit(() -> {
          try {
            dbService.addCurrentGamePoints("pointsSession" + userId, 10);
            dbService.addGlobalPoints("pointsSession" + userId, 5);
            successCount.incrementAndGet();
          } catch (Exception e) {
            // Ignore errors
          } finally {
            latch.countDown();
          }
        });
      }
    }

    latch.await(60, TimeUnit.SECONDS);
    long endTime = System.currentTimeMillis();
    executor.shutdown();

    long totalTime = endTime - startTime;
    double throughput = (successCount.get() * 1000.0) / totalTime;

    printPerformanceMetrics("Frequent Points Updates", totalTime, successCount.get(), throughput);

    // Обновления очков должны быть быстрыми, минимум 100 операций в минуту
    double updatesPerMinute = (successCount.get() * 60000.0) / totalTime;
    System.out.println("Updates per minute: " + String.format("%.2f", updatesPerMinute));
    
    assertTrue(throughput >= 1.67, "Throughput should be at least 1.67 ops/sec (100 ops/min)");
    assertTrue(updatesPerMinute >= 100, "Should handle at least 100 point updates per minute");
  }

  @Test
  @Order(7)
  void testLeaderboardQueriesUnderLoad() throws InterruptedException, SQLException, DatabaseAccessException {
    // Тест производительности запросов лидербордов под нагрузкой
    int numberOfUsers = 30;
    int queriesPerUser = 20;

    // Подготовка: создаем пользователей с очками
    dbService.register("leaderboardGameOwner", "pass");
    dbService.authorize("leaderboardGameOwner", "pass", "lbOwnerSession");
    Integer gameId = dbService.createGame("lbOwnerSession", 1, 10, 30, 1);
    for (int i = 0; i < numberOfUsers; i++) {
      dbService.register("leaderboardUser" + i, "pass" + i);
      dbService.authorize("leaderboardUser" + i, "pass" + i, "lbSession" + i);
      dbService.setCurrentGame("lbSession" + i, gameId);
      dbService.addCurrentGamePoints("lbSession" + i, i * 10);
      dbService.addGlobalPoints("lbSession" + i, i * 5);
    }

    ExecutorService executor = Executors.newFixedThreadPool(20);
    CountDownLatch latch = new CountDownLatch(numberOfUsers * queriesPerUser);
    AtomicInteger successCount = new AtomicInteger(0);

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < numberOfUsers; i++) {
      for (int j = 0; j < queriesPerUser; j++) {
        executor.submit(() -> {
          try {
            // Чередуем запросы игрового и глобального лидерборда
            if (Math.random() > 0.5) {
              dbService.getGameLeaderboards(gameId);
            } else {
              dbService.getGlobalLeaderboards();
            }
            successCount.incrementAndGet();
          } catch (Exception e) {
            // Ignore errors
          } finally {
            latch.countDown();
          }
        });
      }
    }

    latch.await(60, TimeUnit.SECONDS);
    long endTime = System.currentTimeMillis();
    executor.shutdown();

    long totalTime = endTime - startTime;
    double throughput = (successCount.get() * 1000.0) / totalTime;

    printPerformanceMetrics("Leaderboard Queries Under Load", totalTime, successCount.get(), throughput);

    // Запросы лидербордов должны поддерживать минимум 100 запросов в минуту
    double queriesPerMinute = (successCount.get() * 60000.0) / totalTime;
    System.out.println("Queries per minute: " + String.format("%.2f", queriesPerMinute));
    
    assertTrue(throughput >= 1.67, "Throughput should be at least 1.67 ops/sec (100 queries/min)");
    assertTrue(queriesPerMinute >= 100, "Should handle at least 100 leaderboard queries per minute");
  }

  @Test
  @Order(8)
  void testConcurrentGameJoining() throws InterruptedException, SQLException, DatabaseAccessException {
    // Тест производительности одновременного присоединения к игре
    // Проверяем выдержку не менее 100 операций в минуту
    int numberOfGames = 10;
    int usersPerGame = 10; // 100 пользователей всего
    int threadPoolSize = 30;

    // Подготовка: создаем игры и пользователей
    List<Integer> gameIds = new ArrayList<>();
    for (int i = 0; i < numberOfGames; i++) {
      dbService.register("gameOwner" + i, "pass" + i);
      dbService.authorize("gameOwner" + i, "pass" + i, "ownerSession" + i);
      Integer gameId = dbService.createGame("ownerSession" + i, 1, 10, usersPerGame, 1);
      gameIds.add(gameId);
    }

    for (int i = 0; i < numberOfGames * usersPerGame; i++) {
      dbService.register("joiner" + i, "pass" + i);
      dbService.authorize("joiner" + i, "pass" + i, "joinerSession" + i);
    }

    ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
    CountDownLatch latch = new CountDownLatch(numberOfGames * usersPerGame);
    AtomicInteger successCount = new AtomicInteger(0);

    long startTime = System.currentTimeMillis();

    int userIndex = 0;
    for (int gameIndex = 0; gameIndex < numberOfGames; gameIndex++) {
      final Integer gameId = gameIds.get(gameIndex);
      for (int j = 0; j < usersPerGame; j++) {
        final int currentUserIndex = userIndex++;
        executor.submit(() -> {
          try {
            dbService.setCurrentGame("joinerSession" + currentUserIndex, gameId);
            successCount.incrementAndGet();
          } catch (Exception e) {
            // Ignore errors (some may fail due to capacity limits)
          } finally {
            latch.countDown();
          }
        });
      }
    }

    latch.await(60, TimeUnit.SECONDS);
    long endTime = System.currentTimeMillis();
    executor.shutdown();

    long totalTime = endTime - startTime;
    double throughput = (successCount.get() * 1000.0) / totalTime;

    printPerformanceMetrics("Concurrent Game Joining", totalTime, successCount.get(), throughput);

    // Присоединение к играм должно поддерживать минимум 100 операций в минуту
    double joinsPerMinute = (successCount.get() * 60000.0) / totalTime;
    System.out.println("Joins per minute: " + String.format("%.2f", joinsPerMinute));
    
    assertTrue(successCount.get() >= 100, "At least 100 users should join games");
    assertTrue(throughput >= 1.67, "Throughput should be at least 1.67 ops/sec (100 joins/min)");
    assertTrue(joinsPerMinute >= 100, "Should handle at least 100 game joins per minute");
  }

  @Test
  @Order(9)
  void testGetOpenGamesPerformance() throws SQLException, InterruptedException, DatabaseAccessException {
    // Тест производительности получения списка открытых игр
    int numberOfOpenGames = 50;
    int numberOfQueries = 100;

    // Подготовка: создаем много открытых игр
    for (int i = 0; i < numberOfOpenGames; i++) {
      dbService.register("openGameOwner" + i, "pass" + i);
      dbService.authorize("openGameOwner" + i, "pass" + i, "openOwnerSession" + i);
      Integer gameId = dbService.createGame("openOwnerSession" + i, 1, 10, 4, 1);
      dbService.setPrivate(gameId, false);
    }

    ExecutorService executor = Executors.newFixedThreadPool(10);
    CountDownLatch latch = new CountDownLatch(numberOfQueries);
    AtomicInteger successCount = new AtomicInteger(0);

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < numberOfQueries; i++) {
      executor.submit(() -> {
        try {
          JSONArray openGames = dbService.getOpenGames();
          if (openGames != null) {
            successCount.incrementAndGet();
          }
        } catch (Exception e) {
          // Ignore errors
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await(30, TimeUnit.SECONDS);
    long endTime = System.currentTimeMillis();
    executor.shutdown();

    long totalTime = endTime - startTime;
    double throughput = (successCount.get() * 1000.0) / totalTime;

    printPerformanceMetrics("Get Open Games Performance", totalTime, successCount.get(), throughput);

    // Получение списка открытых игр должно поддерживать минимум 100 запросов в минуту
    double queriesPerMinute = (successCount.get() * 60000.0) / totalTime;
    System.out.println("Queries per minute: " + String.format("%.2f", queriesPerMinute));
    
    assertTrue(throughput >= 1.67, "Throughput should be at least 1.67 ops/sec (100 queries/min)");
    assertTrue(queriesPerMinute >= 100, "Should handle at least 100 getOpenGames queries per minute");
  }

  @Test
  @Order(10)
  void testAchievementCheckPerformance() throws SQLException, InterruptedException, DatabaseAccessException {
    // Тест производительности проверки достижений
    int numberOfUsers = 20;
    int checksPerUser = 10;

    // Подготовка: создаем достижения и пользователей
    Achievement achievement = new Achievement();
    achievement.name = "Test Achievement";
    achievement.profilePicNeeded = false;
    achievement.descriptionNeeded = false;
    achievement.gamesNumberNeeded = 0;
    achievement.globalPointsNeeded = 0;
    achievement.globalRatingPlaceNeeded = 0;
    achievement.currentGamePointsNeeded = 0;
    achievement.currentGameRatingNeeded = 0;
    achievement.currentGameLevelDifficultyNeeded = 0;
    dbService.addAchievement(achievement);

    for (int i = 0; i < numberOfUsers; i++) {
      dbService.register("achievementUser" + i, "pass" + i);
      dbService.authorize("achievementUser" + i, "pass" + i, "achSession" + i);
    }

    ExecutorService executor = Executors.newFixedThreadPool(10);
    CountDownLatch latch = new CountDownLatch(numberOfUsers * checksPerUser);
    AtomicInteger successCount = new AtomicInteger(0);

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < numberOfUsers; i++) {
      final int userId = i;
      for (int j = 0; j < checksPerUser; j++) {
        executor.submit(() -> {
          try {
            Achievement checkAchievement = new Achievement();
            checkAchievement.profilePicNeeded = false;
            checkAchievement.descriptionNeeded = false;
            checkAchievement.gamesNumberNeeded = 0;
            checkAchievement.globalPointsNeeded = 0;
            checkAchievement.globalRatingPlaceNeeded = 0;
            checkAchievement.currentGamePointsNeeded = 0;
            checkAchievement.currentGameRatingNeeded = 0;
            checkAchievement.currentGameLevelDifficultyNeeded = 0;
            dbService.checkAchievementAchieved("achSession" + userId, checkAchievement);
            successCount.incrementAndGet();
          } catch (Exception e) {
            // Ignore errors
          } finally {
            latch.countDown();
          }
        });
      }
    }

    latch.await(60, TimeUnit.SECONDS);
    long endTime = System.currentTimeMillis();
    executor.shutdown();

    long totalTime = endTime - startTime;
    double throughput = (successCount.get() * 1000.0) / totalTime;

    printPerformanceMetrics("Achievement Check Performance", totalTime, successCount.get(), throughput);

    // Проверка достижений должна поддерживать минимум 100 проверок в минуту
    double checksPerMinute = (successCount.get() * 60000.0) / totalTime;
    System.out.println("Checks per minute: " + String.format("%.2f", checksPerMinute));
    
    assertTrue(throughput >= 1.67, "Throughput should be at least 1.67 ops/sec (100 checks/min)");
    assertTrue(checksPerMinute >= 100, "Should handle at least 100 achievement checks per minute");
  }

  @Test
  @Order(11)
  void testMixedWorkload() throws SQLException, InterruptedException, DatabaseAccessException {
    // Комплексный тест смешанной нагрузки
    int numberOfOperations = 200;
    int threadPoolSize = 30;

    // Подготовка: создаем базовых пользователей и игры
    for (int i = 0; i < 20; i++) {
      dbService.register("mixedUser" + i, "pass" + i);
      dbService.authorize("mixedUser" + i, "pass" + i, "mixedSession" + i);
    }

    Integer gameId = dbService.createGame("mixedSession0", 1, 10, 15, 1);
    dbService.setPrivate(gameId, false);

    ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
    CountDownLatch latch = new CountDownLatch(numberOfOperations);
    AtomicInteger successCount = new AtomicInteger(0);

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < numberOfOperations; i++) {
      final int operationIndex = i;
      executor.submit(() -> {
        try {
          int operationType = operationIndex % 7;
          switch (operationType) {
            case 0:
              // Регистрация
              dbService.register("tempUser" + operationIndex, "pass");
              break;
            case 1:
              // Проверка сессии
              dbService.getUserId("mixedSession" + (operationIndex % 20));
              break;
            case 2:
              // Обновление очков
              dbService.addGlobalPoints("mixedSession" + (operationIndex % 20), 1);
              break;
            case 3:
              // Получение открытых игр
              dbService.getOpenGames();
              break;
            case 4:
              // Получение лидерборда
              dbService.getGlobalLeaderboards();
              break;
            case 5:
              // Создание игры
              dbService.createGame("mixedSession" + (operationIndex % 20), 1, 5, 4, 1);
              break;
            case 6:
              // Получение очков
              dbService.getGlobalPoints("mixedSession" + (operationIndex % 20));
              break;
          }
          successCount.incrementAndGet();
        } catch (Exception e) {
          // Ignore errors (some operations may fail due to constraints)
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await(120, TimeUnit.SECONDS);
    long endTime = System.currentTimeMillis();
    executor.shutdown();

    long totalTime = endTime - startTime;
    double throughput = (successCount.get() * 1000.0) / totalTime;

    printPerformanceMetrics("Mixed Workload (Realistic Scenario)", totalTime, successCount.get(), throughput);

    // Смешанная нагрузка должна поддерживать минимум 100 операций в минуту
    double opsPerMinute = (successCount.get() * 60000.0) / totalTime;
    System.out.println("Operations per minute: " + String.format("%.2f", opsPerMinute));
    
    assertTrue(successCount.get() >= numberOfOperations * 0.7, 
        "At least 70% of operations should succeed in mixed workload");
    assertTrue(throughput >= 1.67, "Throughput should be at least 1.67 ops/sec (100 ops/min)");
    assertTrue(opsPerMinute >= 100, "Should handle at least 100 operations per minute in mixed workload");
  }
}