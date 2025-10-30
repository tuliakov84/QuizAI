package com.mipt;

import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.Question;
import org.json.JSONArray;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

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
    assertNull(dbService.getUserId("SESSION"));
    assertFalse(dbService.authorize("notExistingTestUser", "1234", "SESSION"));
    assertNull(dbService.getUserId("SESSION"));
    assertFalse(dbService.authorize("testUser", "12345wrong", "SESSION"));
    assertNull(dbService.getUserId("SESSION"));
    assertTrue(dbService.authorize("testUser", "12345", "SESSION"));
    assertNotNull(dbService.getUserId("SESSION"));
    assertFalse(dbService.authorize("testUser", "12345", "SESSION"));
  }

  @Test
  @Order(3)
  void testLogOut() throws SQLException {
    assertNotNull(dbService.getUserId("SESSION"));
    dbService.logOut("SESSION");
    assertNull(dbService.getUserId("SESSION"));
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
  @Order(5)
  void testAddGamePlayedAndGetGamesPlayed_notInGame() throws SQLException {
    dbService.addGamePlayed("NOT_EXISTING_SESSION");
    assertNull(dbService.getGamesPlayed("NOT_EXISTING_SESSION"));
    dbService.addGamePlayed("SESSION");
    assertNull(dbService.getGamesPlayed("SESSION"));
  }

  @Test
  @Order(6)
  void testSetCurrentGameAndGetCurrentGame() throws SQLException {
    assertNull(dbService.getCurrentGame("NOT_EXISTING_SESSION"));
    assertNull(dbService.getCurrentGame("SESSION"));
    dbService.setCurrentGame("SESSION", 1);
    assertEquals(1, dbService.getCurrentGame("SESSION"));
  }

  @Test
  @Order(7)
  void testAddGamePlayedAndGetGamesPlayed_inGame() throws SQLException {
    dbService.addGamePlayed("NOT_EXISTING_SESSION");
    assertNull(dbService.getGamesPlayed("NOT_EXISTING_SESSION"));
    dbService.addGamePlayed("SESSION");
    Integer[] testArr = {1};
    assertArrayEquals(testArr, dbService.getGamesPlayed("SESSION"));
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
  @Test
  @Order(8)
  void testCreateGameAndCheckGameExists() throws SQLException {
    assertNull(dbService.createGame("NOT_EXISTING_SESSION", 1, 1, 4, 1));
    assertNull(dbService.createGame("SESSION", 4, 1, 4, 1));
    assertNull(dbService.createGame("SESSION", 1, 1, -1, 1));
    assertTrue(dbService.checkGameExists(1)); // test game with id=1 is already initialized in sql
    assertFalse(dbService.checkGameExists(2));
    assertEquals(2, dbService.createGame("SESSION", 1, 1, 4, 1));
    assertTrue(dbService.checkGameExists(2));
  }

  @Test
  void testGetPreset() throws SQLException {
    Integer[] testArr = {1, 1, 1, 4, 1};
    assertArrayEquals(testArr, dbService.getPreset(2));
  }

  @Test
  void testSetStatusAndGetStatus() throws SQLException {
    assertNull(dbService.getStatus(52));
    assertEquals(0, dbService.getStatus(2));
    dbService.setStatus(2, 2);
    assertEquals(2, dbService.getStatus(2));
  }

  @Test
  void testSetPrivateAndGetPrivate() throws SQLException {
    // FEATURE
    assertNull(dbService.getPrivate(52));
    assertTrue(dbService.getPrivate(2));
    dbService.setPrivate(2, false);
    assertFalse(dbService.getPrivate(2));
  }

  @Test
  void testGetGameStartTimeAndSetGameStartTime() throws SQLException {
    // FEATURE
    assertNull(dbService.getGameStartTime(52));
    dbService.setGameStartTime(2, Instant.ofEpochSecond(1));
    assertEquals(Timestamp.from(Instant.ofEpochSecond(1)), dbService.getGameStartTime(2));
  }

  @Test
  void testGetGameEndTimeAndSetGameEndTime() throws SQLException {
    assertNull(dbService.getGameEndTime(52));
    dbService.setGameEndTime(2, Instant.ofEpochSecond(1));
    assertEquals(Timestamp.from(Instant.ofEpochSecond(1)), dbService.getGameEndTime(2));
  }

  @Test
  void testLoadQuestionsAndNextQuestion() throws SQLException {
    String jsonString = "[\n" +
        "    {\n" +
        "        \"question_number\": 1,\n" +
        "        \"question_text\": \"What the Aglet is?\",\n" +
        "        \"available_answers\": \n" +
        "        [\n" +
        "            {\n" +
        "                \"index\": 1,\n" +
        "                \"answer\": \"American audit company\"\n" +
        "            },\n" +
        "            {\n" +
        "                \"index\": 2,\n" +
        "                \"answer\": \"Programming language\"\n" +
        "            },\n" +
        "            {\n" +
        "                \"index\": 3,\n" +
        "                \"answer\": \"Lace tip\"\n" +
        "            },\n" +
        "            {\n" +
        "                \"index\": 4,\n" +
        "                \"answer\": \"idk\"\n" +
        "            }\n" +
        "        ],\n" +
        "        \"right_answer_number\": 3\n" +
        "    },\n" +
        "    {\n" +
        "        \"question_number\": 2,\n" +
        "        \"question_text\": \"What is the punishment for driving drunk in Russia?\",\n" +
        "        \"available_answers\": \n" +
        "        [\n" +
        "            {\n" +
        "                \"index\": 1,\n" +
        "                \"answer\": \"100 USD\"\n" +
        "            },\n" +
        "            {\n" +
        "                \"index\": 2,\n" +
        "                \"answer\": \"Deprivation of driving license\"\n" +
        "            },\n" +
        "            {\n" +
        "                \"index\": 3,\n" +
        "                \"answer\": \"Warning\"\n" +
        "            },\n" +
        "            {\n" +
        "                \"index\": 4,\n" +
        "                \"answer\": \"None\"\n" +
        "            }\n" +
        "        ],\n" +
        "        \"right_answer_number\": 2\n" +
        "    }\n" +
        "]\n";

    JSONArray jsonObject = new JSONArray(jsonString);
    dbService.loadQuestions(2, jsonObject);

    Question result1 = dbService.nextQuestion(2);
    if (
        Objects.equals(result1.questionText, "What the Aglet is?") &&
            Objects.equals(result1.answer1, "American audit company") &&
            Objects.equals(result1.answer2, "Programming language") &&
            Objects.equals(result1.answer3, "Lace tip") &&
            Objects.equals(result1.answer4, "idk")
    ) {
      assertTrue(true);
    } else {
      fail();
    }

    Question result2 = dbService.nextQuestion(2);
    if (
        Objects.equals(result2.questionText, "What is the punishment for driving drunk in Russia?") &&
            Objects.equals(result2.answer1, "100 USD") &&
            Objects.equals(result2.answer2, "Deprivation of driving license") &&
            Objects.equals(result2.answer3, "Warning") &&
            Objects.equals(result2.answer4, "None")
    ) {
      assertTrue(true);
    } else {
      fail();
    }

  }

  void testValidateAnswer() throws SQLException {
    // FEATURE
  }

  void testStopGame() throws SQLException {
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