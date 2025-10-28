package com.mipt;

import com.mipt.dbAPI.DbService;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.sql.*;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DbServiceTest {

  // INITIALIZING CONTAINER

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
          .withStartupTimeout(Duration.ofSeconds(90));;

  static DbService dbService;

  @BeforeAll
  static void setUp() {
    assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
    String jdbcUrl = postgres.getJdbcUrl();
    String username = postgres.getUsername();
    String password = postgres.getPassword();

    dbService = new DbService(jdbcUrl, username, password);
  }

  // Users TEST UNITS

  @Test
  @Order(1)
  void testRegisterAndCheckUserExists() throws SQLException {
    assertFalse(dbService.checkUserExists("testUser"));
    assertTrue(dbService.register("testUser", "12345"));
    assertTrue(dbService.checkUserExists("testUser"));
    assertFalse(dbService.register("testUser", "12345678"));
  }

  @Test
  @Order(2)
  void testAuthorizeAndCheckedLoggedIn() throws SQLException {
    assertNull(dbService.checkLoggedIn("SESSION"));
    assertFalse(dbService.authorize("notExistingTestUser", "1234", "SESSION"));
    assertNull(dbService.checkLoggedIn("SESSION"));
    assertFalse(dbService.authorize("testUser", "12345wrong", "SESSION"));
    assertNull(dbService.checkLoggedIn("SESSION"));
    assertTrue(dbService.authorize("testUser", "12345", "SESSION"));
    assertNotNull(dbService.checkLoggedIn("SESSION"));
    assertFalse(dbService.authorize("testUser", "12345", "SESSION"));
  }

  @Test
  @Order(3)
  void testLogOut() throws SQLException {
    assertNotNull(dbService.checkLoggedIn("SESSION"));
    dbService.logOut("SESSION");
    assertNull(dbService.checkLoggedIn("SESSION"));
    assertTrue(dbService.authorize("testUser", "12345", "SESSION"));
  }

  @Test
  @Order(4)
  void testChangePassword() throws SQLException {
    dbService.changePassword("SESSION", "12345new");
    dbService.logOut("SESSION");
    assertFalse(dbService.authorize("testUser", "12345", "SESSION"));
    assertTrue(dbService.authorize("testUser", "12345new", "SESSION"));
  }

  @Test
  void testChangeProfilePicAndGetProfilePic() throws SQLException {
    assertEquals(0, dbService.getProfilePic("NOT_EXISTING_SESSION"));
    assertEquals(0, dbService.getProfilePic("SESSION"));
    dbService.changeProfilePic("SESSION", 1);
    assertEquals(1, dbService.getProfilePic("SESSION"));
  }

  @Test
  void testChangeDescriptionAndGetDescription() throws SQLException {
    assertEquals("", dbService.getDescription("NOT_EXISTING_SESSION"));
    assertEquals("", dbService.getDescription("SESSION"));
    dbService.changeDescription("SESSION", "testDescription");
    assertEquals("testDescription", dbService.getDescription("SESSION"));
  }

  @Test
  void testSetLastActivityAndGetLastActivity() throws SQLException {
    assertNull(dbService.getLastActivity("NOT_EXISTING_SESSION"));
    assertNull(dbService.getLastActivity("SESSION"));
    dbService.setLastActivity("SESSION", Instant.ofEpochSecond(1));
    assertEquals(Timestamp.from(Instant.ofEpochSecond(1)), dbService.getLastActivity("SESSION"));
  }

  @Test
  void testSetCurrentGameAndGetCurrentGame() throws SQLException {
    assertNull(dbService.getCurrentGame("NOT_EXISTING_SESSION"));
    assertNull(dbService.getCurrentGame("SESSION"));
    dbService.setCurrentGame("SESSION", 1);
    assertEquals(1, dbService.getCurrentGame("SESSION"));
  }

  void testAddGamePlayedAndGetGamesPlayed() throws SQLException {
    // FEATURE
  }

  @Test
  void testAddGlobalPointsAndGetGlobalPoints() throws SQLException {
    assertNull(dbService.getGlobalPoints("NOT_EXISTING_SESSION"));
    assertEquals(0, dbService.getGlobalPoints("SESSION"));
    dbService.addGlobalPoints("SESSION", 5);
    assertEquals(5, dbService.getGlobalPoints("SESSION"));
  }

  @Test
  void testAddGlobalPointsAndGetGlobalPossiblePoints() throws SQLException {
    assertNull(dbService.getGlobalPossiblePoints("NOT_EXISTING_SESSION"));
    assertEquals(0, dbService.getGlobalPossiblePoints("SESSION"));
    dbService.addGlobalPossiblePoints("SESSION", 5);
    assertEquals(5, dbService.getGlobalPossiblePoints("SESSION"));
  }

  @Test
  void testAddCurrentGamePointsAndGetCurrentGamePoints() throws SQLException {
    assertNull(dbService.getCurrentGamePoints("NOT_EXISTING_SESSION"));
    assertEquals(0, dbService.getCurrentGamePoints("SESSION"));
    dbService.addCurrentGamePoints("SESSION", 5);
    assertEquals(5, dbService.getCurrentGamePoints("SESSION"));
  }


  // Game TEST UNITS

  void testCreateGameAndCheckGameExists() throws SQLException {
    // FEATURE
  }

  void testGetStatus() throws SQLException {
    // FEATURE
  }

  void testSetPrivateAndGetPrivate() throws SQLException {
    // FEATURE
  }

  void testStartGameAndGetGameStartTime() throws SQLException {
    // FEATURE
  }

  void testLoadQuestions() throws SQLException {
    // FEATURE
  }

  void testNextQuestion() throws SQLException {
    // FEATURE
  }

  void testValidateAnswer() throws SQLException {
    // FEATURE
  }

  void testStopGameAndGetGameEndTime() throws SQLException {
    // FEATURE
  }

  void testDeleteGame() throws SQLException {
    // FEATURE
  }

  // Achievement TEST UNITS

  void testAddAchievement() throws SQLException {
    // FEATURE
  }

  void testRemoveAchievement() throws SQLException {
    // FEATURE
  }

  // Topic TEST UNITS

  void testAddTopic() throws SQLException {
    // FEATURE
  }

  void testRemoveTopic() throws SQLException {
    // FEATURE
  }
}