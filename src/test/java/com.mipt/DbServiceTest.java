package com.mipt;

import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.Achievement;
import com.mipt.domainModel.Question;
import com.mipt.domainModel.Topic;
import org.json.JSONArray;
import org.json.JSONException;
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
    // setting up database
    assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
    String jdbcUrl = postgres.getJdbcUrl();
    String username = postgres.getUsername();
    String password = postgres.getPassword();

    dbService = new DbService(jdbcUrl, username, password);
  }

  @BeforeEach
  void setInitRecords() throws SQLException, DatabaseAccessException {
    // creating users
    assertFalse(dbService.checkUserExists("test1"));
    assertDoesNotThrow(() -> dbService.register("test1", "12345"));
    assertTrue(dbService.checkUserExists("test1"));
    assertThrows(DatabaseAccessException.class, () -> dbService.register("test1", "12345678"));

    assertFalse(dbService.checkUserExists("test2"));
    assertDoesNotThrow(() -> dbService.register("test2", "1234"));
    assertTrue(dbService.checkUserExists("test2"));
    assertThrows(DatabaseAccessException.class, () -> dbService.register("test2", "1234567"));

    // authorizing users
    assertNull(dbService.getUserId("SESSION"));
    assertThrows(DatabaseAccessException.class, () -> dbService.authenticate("notExistingTestUser", "1234", "SESSION"));
    assertNull(dbService.getUserId("SESSION"));
    assertThrows(DatabaseAccessException.class, () -> dbService.authenticate("test1", "12345wrong", "SESSION"));
    assertNull(dbService.getUserId("SESSION"));

    assertDoesNotThrow(() -> dbService.authenticate("test1", "12345", "SESSION"));
    assertNotNull(dbService.getUserId("SESSION"));
    assertThrows(DatabaseAccessException.class, () -> dbService.authenticate("test1", "12345", "SESSION"));

    assertThrows(DatabaseAccessException.class, () -> dbService.authenticate("test2", "12345wrong", "SESSION"));
    assertDoesNotThrow(() -> dbService.authenticate("test2", "1234", "SESSION2"));

    assertEquals("test1", dbService.getUsername(dbService.getUserId("SESSION")));
    assertEquals("test2", dbService.getUsername("SESSION2"));
  }

  @AfterEach
  void eraseRecords() throws SQLException, DatabaseAccessException {
    // deleting all data
    dbService.eraseAllData("DELETE_ALL_RECORDS_IN_DATABASE");
  }

  //
  // Users TEST UNITS
  //

  @Test
  void testLogOut() throws SQLException, DatabaseAccessException {
    assertNotNull(dbService.getUserId("SESSION"));
    assertNotNull(dbService.getUserId("SESSION2"));

    assertThrows(DatabaseAccessException.class, () -> dbService.logOut("NOT_EXISTING_SESSION"));
    dbService.logOut("SESSION");
    dbService.logOut("SESSION2");

    assertNull(dbService.getUserId("SESSION"));
    assertDoesNotThrow(() -> dbService.authenticate("test1", "12345", "SESSION"));

    assertNull(dbService.getUserId("SESSION2"));
    assertDoesNotThrow(() -> dbService.authenticate("test2", "1234", "SESSION2"));
  }

  @Test
  void testChangePassword() throws SQLException, DatabaseAccessException {
    assertThrows(DatabaseAccessException.class, () -> dbService.changePassword("NOT_EXISTING_SESSION", "1"));

    dbService.changePassword("SESSION", "12345new");
    dbService.changePassword("SESSION2", "1234new");

    dbService.logOut("SESSION");
    assertThrows(DatabaseAccessException.class, () -> dbService.authenticate("test1", "12345", "SESSION"));
    assertDoesNotThrow(() -> dbService.authenticate("test1", "12345new", "SESSION"));

    dbService.logOut("SESSION2");
    assertThrows(DatabaseAccessException.class, () -> dbService.authenticate("test2", "1234", "SESSION2"));
    assertDoesNotThrow(() -> dbService.authenticate("test2", "1234new", "SESSION2"));
  }

  @Test
  void testChangeProfilePic_GetProfilePic() throws SQLException, DatabaseAccessException {
    assertThrows(DatabaseAccessException.class, () -> dbService.changeProfilePic("NOT_EXISTING_SESSION", 1));

    assertThrows(DatabaseAccessException.class, () -> dbService.getProfilePic("NOT_EXISTING_SESSION"));
    assertEquals(0, dbService.getProfilePic("SESSION"));
    dbService.changeProfilePic("SESSION", 1);
    dbService.changeProfilePic("SESSION2", 2);
    assertEquals(1, dbService.getProfilePic("SESSION"));
    assertEquals(2, dbService.getProfilePic("SESSION2"));
  }

  @Test
  void testChangeDescription_GetDescription() throws SQLException, DatabaseAccessException {
    assertThrows(DatabaseAccessException.class, () -> dbService.changeDescription("NOT_EXISTING_SESSION", "123"));

    assertThrows(DatabaseAccessException.class, () -> dbService.getDescription("NOT_EXISTING_SESSION"));
    assertEquals("", dbService.getDescription("SESSION"));
    dbService.changeDescription("SESSION", "testDescription");
    assertEquals("testDescription", dbService.getDescription("SESSION"));

    assertEquals("", dbService.getDescription("SESSION2"));
  }

  @Test
  void testSetLastActivity_GetLastActivity() throws SQLException, DatabaseAccessException {
    assertThrows(DatabaseAccessException.class, () -> dbService.setLastActivity("NOT_EXISTING_SESSION", Instant.ofEpochSecond(1)));

    assertThrows(DatabaseAccessException.class, () -> dbService.getLastActivity("NOT_EXISTING_SESSION"));
    assertNull(dbService.getLastActivity("SESSION"));
    dbService.setLastActivity("SESSION", Instant.ofEpochSecond(1));
    assertNull(dbService.getLastActivity("SESSION2"));
    dbService.setLastActivity("SESSION2", Instant.ofEpochSecond(2));

    assertEquals(Timestamp.from(Instant.ofEpochSecond(1)), dbService.getLastActivity("SESSION"));
    assertEquals(Timestamp.from(Instant.ofEpochSecond(2)), dbService.getLastActivity("SESSION2"));
  }

  @Test
  void testSetCurrentGame_GetCurrentGame_GetCurrentParticipantsNumber() throws SQLException, DatabaseAccessException {
    // creating game
    Integer gameId = dbService.createGame("SESSION", 1, 5, 4, 1);

    // testing getCurrentGame for not existing sessions
    assertThrows(DatabaseAccessException.class, () -> dbService.setCurrentGame("NOT_EXISTING_SESSION", gameId));
    assertThrows(DatabaseAccessException.class, () -> dbService.getCurrentGame("NOT_EXISTING_SESSION"));

    // creating 3 users additionally to reach the bound
    dbService.register("user3", "1");
    dbService.register("user4", "1");
    dbService.register("user5", "1");
    dbService.authenticate("user3", "1", "SESSION3");
    dbService.authenticate("user4", "1", "SESSION4");
    dbService.authenticate("user5", "1", "SESSION5");

    // joining the game and checking current participants
    assertNull(dbService.getCurrentGame("SESSION"));
    assertDoesNotThrow(() -> dbService.setCurrentGame("SESSION", gameId));
    assertEquals(gameId, dbService.getCurrentGame("SESSION"));
    assertEquals(1, dbService.getCurrentParticipantsNumber(gameId));
    assertNull(dbService.getCurrentGame("SESSION2"));
    assertDoesNotThrow(() -> dbService.setCurrentGame("SESSION2", gameId));
    assertEquals(gameId, dbService.getCurrentGame("SESSION2"));
    assertEquals(2, dbService.getCurrentParticipantsNumber(gameId));
    assertNull(dbService.getCurrentGame("SESSION3"));
    assertDoesNotThrow(() -> dbService.setCurrentGame("SESSION3", gameId));
    assertEquals(gameId, dbService.getCurrentGame("SESSION3"));
    assertEquals(3, dbService.getCurrentParticipantsNumber(gameId));
    assertNull(dbService.getCurrentGame("SESSION4"));
    assertDoesNotThrow(() -> dbService.setCurrentGame("SESSION4", gameId));
    assertEquals(gameId, dbService.getCurrentGame("SESSION4"));
    assertEquals(4, dbService.getCurrentParticipantsNumber(gameId));

    // failed joining the game for test5, out of bounds error throwing
    assertNull(dbService.getCurrentGame("SESSION5"));
    assertThrows(DatabaseAccessException.class, () -> dbService.setCurrentGame("SESSION5", gameId));
    assertNull(dbService.getCurrentGame("SESSION5"));
    assertEquals(4, dbService.getCurrentParticipantsNumber(gameId));
  }

  @Test
  void testAddGamePlayed_GetGamesPlayed() throws SQLException, DatabaseAccessException {
    // test working OUT OF THE GAME
    assertThrows(DatabaseAccessException.class, () -> dbService.addGamePlayed("SESSION"));
    Integer[] emptyArr = new Integer[0];
    assertArrayEquals(emptyArr, dbService.getGamesPlayed("SESSION")); // NOT IN GAME

    // test working IN GAME

    // creating game
    Integer gameId = dbService.createGame("SESSION", 1, 5, 4, 1);

    // testing not existing sessions
    assertThrows(DatabaseAccessException.class, () -> dbService.addGamePlayed("NOT_EXISTING_SESSION"));
    assertThrows(DatabaseAccessException.class, () -> dbService.getGamesPlayed("NOT_EXISTING_SESSION"));

    // joining the game
    assertNull(dbService.getCurrentGame("SESSION"));
    assertDoesNotThrow(() -> dbService.setCurrentGame("SESSION", gameId));
    assertEquals(gameId, dbService.getCurrentGame("SESSION"));
    assertNull(dbService.getCurrentGame("SESSION2"));
    assertDoesNotThrow(() -> dbService.setCurrentGame("SESSION2", gameId));
    assertEquals(gameId, dbService.getCurrentGame("SESSION2"));

    // adding games played
    dbService.addGamePlayed("SESSION");
    dbService.addGamePlayed("SESSION2");

    // checking for games played
    Integer[] testArr = {gameId};
    assertArrayEquals(testArr, dbService.getGamesPlayed("SESSION"));
    assertArrayEquals(testArr, dbService.getGamesPlayed("SESSION2"));
  }

  @Test
  void testAddGlobalPoints_GetGlobalPoints() throws SQLException, DatabaseAccessException {
    // creating game
    Integer gameId = dbService.createGame("SESSION", 1, 5, 4, 1);

    // joining the game
    assertNull(dbService.getCurrentGame("SESSION"));
    assertDoesNotThrow(() -> dbService.setCurrentGame("SESSION", gameId));
    assertEquals(gameId, dbService.getCurrentGame("SESSION"));
    assertNull(dbService.getCurrentGame("SESSION2"));
    assertDoesNotThrow(() -> dbService.setCurrentGame("SESSION2", gameId));
    assertEquals(gameId, dbService.getCurrentGame("SESSION2"));

    // testing not existing sessions
    assertThrows(DatabaseAccessException.class, () -> dbService.addGlobalPoints("NOT_EXISTING_SESSION", 9999));
    assertThrows(DatabaseAccessException.class, () -> dbService.getGlobalPoints("NOT_EXISTING_SESSION"));

    // adding global points
    assertEquals(0, dbService.getGlobalPoints("SESSION"));
    dbService.addGlobalPoints("SESSION", 5);
    dbService.addGlobalPoints("SESSION", 5);
    assertEquals(10, dbService.getGlobalPoints("SESSION"));

    assertEquals(0, dbService.getGlobalPoints("SESSION2"));
  }

  @Test
  void testAddGlobalPossiblePoints_GetGlobalPossiblePoints() throws SQLException, DatabaseAccessException {
    // creating game
    Integer gameId = dbService.createGame("SESSION", 1, 5, 4, 1);

    // joining the game
    assertNull(dbService.getCurrentGame("SESSION"));
    assertDoesNotThrow(() -> dbService.setCurrentGame("SESSION", gameId));
    assertEquals(gameId, dbService.getCurrentGame("SESSION"));
    assertNull(dbService.getCurrentGame("SESSION2"));
    assertDoesNotThrow(() -> dbService.setCurrentGame("SESSION2", gameId));
    assertEquals(gameId, dbService.getCurrentGame("SESSION2"));

    // testing not existing sessions
    assertThrows(DatabaseAccessException.class, () -> dbService.addGlobalPossiblePoints("NOT_EXISTING_SESSION", 9999));
    assertThrows(DatabaseAccessException.class, () -> dbService.getGlobalPossiblePoints("NOT_EXISTING_SESSION"));

    // adding global possible points
    assertEquals(0, dbService.getGlobalPossiblePoints("SESSION"));
    dbService.addGlobalPossiblePoints("SESSION", 5);
    dbService.addGlobalPossiblePoints("SESSION", 5);
    assertEquals(10, dbService.getGlobalPossiblePoints("SESSION"));

    assertEquals(0, dbService.getGlobalPossiblePoints("SESSION2"));
  }

  @Test
  void testAddCurrentGamePoints_GetCurrentGamePoints() throws SQLException, DatabaseAccessException {
    // creating game
    Integer gameId = dbService.createGame("SESSION", 1, 5, 4, 1);

    // joining the game
    assertNull(dbService.getCurrentGame("SESSION"));
    assertDoesNotThrow(() -> dbService.setCurrentGame("SESSION", gameId));
    assertEquals(gameId, dbService.getCurrentGame("SESSION"));
    assertNull(dbService.getCurrentGame("SESSION2"));
    assertDoesNotThrow(() -> dbService.setCurrentGame("SESSION2", gameId));
    assertEquals(gameId, dbService.getCurrentGame("SESSION2"));

    // testing not existing sessions
    assertThrows(DatabaseAccessException.class, () -> dbService.addCurrentGamePoints("NOT_EXISTING_SESSION", 9999));
    assertThrows(DatabaseAccessException.class, () -> dbService.getCurrentGamePoints("NOT_EXISTING_SESSION"));

    // adding global possible points
    assertEquals(0, dbService.getCurrentGamePoints("SESSION"));
    dbService.addCurrentGamePoints("SESSION", 5);
    dbService.addCurrentGamePoints("SESSION", 5);
    assertEquals(10, dbService.getCurrentGamePoints("SESSION"));

    assertEquals(0, dbService.getCurrentGamePoints("SESSION2"));
  }

  //
  // Game TEST UNITS
  //

  @Test
  @Order(1)
  void testCreateGame_CheckGameExists_setParticipantsNumber() throws SQLException, DatabaseAccessException {
    // testing not existing sessions
    assertThrows(DatabaseAccessException.class, () -> dbService.createGame("NOT_EXISTING_SESSION", 1, 1, 4, 1));

    // testing errors throwing "out of bounds"
    assertThrows(DatabaseAccessException.class, () -> dbService.createGame("SESSION", 4, 1, 4, 1));
    assertThrows(DatabaseAccessException.class, () -> dbService.createGame("SESSION", 1, 1, -1, 1));

    // testing game exists method
    assertTrue(dbService.checkGameExists(1)); // test game with topicId=1 is already initialized in sql
    assertFalse(dbService.checkGameExists(2)); // we can say it's gameId=2 because the test goes firstly
    assertEquals(2, dbService.createGame("SESSION", 1, 1, 4, 1)); // checking gameId=2
    assertTrue(dbService.checkGameExists(2));

    // testing setParticipantsNumber method
    dbService.changeParticipantsNumber(2, 6);
    assertEquals(6, dbService.getPreset(2)[3]);
    assertEquals(1, dbService.getPreset(1)[3]);
  }

  @Test
  void testSetPrivate_GetPrivate_GetOpenGames() throws SQLException, DatabaseAccessException, JSONException {
    // creating games
    Integer gameId1 = dbService.createGame("SESSION", 1, 5, 4, 1);
    Integer gameId2 = dbService.createGame("SESSION2", 3, 15, 5, 1);

    // set, get private test
    // testing not existing games
    assertThrows(DatabaseAccessException.class, () -> dbService.setPrivate(145, false));
    assertThrows(DatabaseAccessException.class, () -> dbService.getPrivate(145));

    // testing
    assertTrue(dbService.getPrivate(gameId1));
    dbService.setPrivate(gameId1, false);
    assertFalse(dbService.getPrivate(gameId1)); // first game is open
    assertTrue(dbService.getPrivate(gameId2)); // second game is closed

    JSONArray result = dbService.getOpenGames();
    assertEquals(1, result.length());
    JSONArray first = result.getJSONArray(0);

    assertEquals(gameId1, first.getInt(0)); // id
    assertEquals(1, first.getInt(1)); // topic
    assertEquals(dbService.getCurrentParticipantsNumber(gameId1), first.getInt(2)); // current participants number
    assertEquals(dbService.getPreset(gameId1)[3], first.getInt(3)); // max participants number
  }

  @Test
  void testGetPreset() throws SQLException, DatabaseAccessException {
    // creating game
    Integer gameId = dbService.createGame("SESSION", 1, 5, 4, 1);

    // testing not existing game
    assertThrows(DatabaseAccessException.class, () -> dbService.getPreset(145));

    // testing
    Integer[] testArr = {dbService.getUserId("SESSION"), 1, 5, 4, 1};
    assertArrayEquals(testArr, dbService.getPreset(gameId));
  }

  @Test
  void testSetStatus_GetStatus() throws SQLException, DatabaseAccessException {
    // creating game
    Integer gameId = dbService.createGame("SESSION", 1, 5, 4, 1);

    // testing not existing game
    assertThrows(DatabaseAccessException.class, () -> dbService.setStatus(145, 2));
    assertThrows(DatabaseAccessException.class, () -> dbService.getStatus(145));

    // testing
    assertEquals(0, dbService.getStatus(gameId));
    dbService.setStatus(gameId, 2);
    assertEquals(2, dbService.getStatus(gameId));
  }

  @Test
  void testGetGameStartTime_SetGameStartTime() throws SQLException, DatabaseAccessException {
    // creating game
    Integer gameId = dbService.createGame("SESSION", 1, 5, 4, 1);

    // testing not existing game
    assertThrows(DatabaseAccessException.class, () -> dbService.setGameStartTime(145, Instant.ofEpochSecond(1)));
    assertThrows(DatabaseAccessException.class, () -> dbService.getGameStartTime(145));

    // testing
    dbService.setGameStartTime(gameId, Instant.ofEpochSecond(1));
    assertEquals(Timestamp.from(Instant.ofEpochSecond(1)), dbService.getGameStartTime(gameId));
  }

  @Test
  void testGetGameEndTime_SetGameEndTime() throws SQLException, DatabaseAccessException {
    // creating game
    Integer gameId = dbService.createGame("SESSION", 1, 5, 4, 1);

    // testing not existing game
    assertThrows(DatabaseAccessException.class, () -> dbService.setGameEndTime(145, Instant.ofEpochSecond(1)));
    assertThrows(DatabaseAccessException.class, () -> dbService.getGameEndTime(145));

    // testing
    dbService.setGameEndTime(gameId, Instant.ofEpochSecond(1));
    assertEquals(Timestamp.from(Instant.ofEpochSecond(1)), dbService.getGameEndTime(gameId));
  }

  @Test
  void testLoadQuestions_NextQuestion_GetCurrentQuestion_GetRightAnswer() throws SQLException, DatabaseAccessException, JSONException {
    // creating game
    Integer gameId = dbService.createGame("SESSION", 1, 10, 4, 1);
    dbService.setStatus(gameId, 2);

    // testing not existing game getCurrentQuestionNumber and nextQuestion
    assertThrows(DatabaseAccessException.class, () -> dbService.getCurrentQuestionNumber(145));
    assertThrows(DatabaseAccessException.class, () -> dbService.nextQuestion(145));

    // load questions
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

    // performing loadQuestions and testing not existing game
    JSONArray jsonObject = new JSONArray(jsonString);
    assertThrows(DatabaseAccessException.class, () -> dbService.loadQuestions(145, jsonObject));
    dbService.loadQuestions(gameId, jsonObject);

    // testing getRightAnswer
    // testing not existing game
    assertThrows(DatabaseAccessException.class, () -> dbService.getRightAnswer(145, 2));

    // testing stopped game getRightAnswer
    Integer stoppedGameId = dbService.createGame("SESSION2", 1, 10, 4, 1);
    dbService.loadQuestions(stoppedGameId, jsonObject);
    dbService.setStatus(stoppedGameId, 3);
    assertThrows(DatabaseAccessException.class, () -> dbService.getRightAnswer(stoppedGameId, 2));

    // testing running game getRightAnswer
    assertEquals(3, dbService.getRightAnswer(gameId, 1));
    assertEquals(2, dbService.getRightAnswer(gameId, 2));

    // testing nextQuestion
    assertEquals(0, dbService.getCurrentQuestionNumber(gameId));
    Question result1 = dbService.nextQuestion(gameId);
    assertEquals(1, dbService.getCurrentQuestionNumber(gameId));

    if (
        (
        Objects.equals(result1.getQuestionText(), "What the Aglet is?") &&
            Objects.equals(result1.getAnswer1(), "American audit company") &&
            Objects.equals(result1.getAnswer2(), "Programming language") &&
            Objects.equals(result1.getAnswer3(), "Lace tip") &&
            Objects.equals(result1.getAnswer4(), "idk")
        ) && (
            dbService.getRightAnswer(gameId, 1).equals(3)
        )
    ) {
      assertTrue(true);
    } else {
      fail();
    }

    Question result2 = dbService.nextQuestion(gameId);
    assertEquals(2, dbService.getCurrentQuestionNumber(gameId));

    if (
        (
        Objects.equals(result2.getQuestionText(), "What is the punishment for driving drunk in Russia?") &&
            Objects.equals(result2.getAnswer1(), "100 USD") &&
            Objects.equals(result2.getAnswer2(), "Deprivation of driving license") &&
            Objects.equals(result2.getAnswer3(), "Warning") &&
            Objects.equals(result2.getAnswer4(), "None")
        ) && (
            dbService.getRightAnswer(gameId, 2).equals(2)
        )
    ) {
      assertTrue(true);
    } else {
      fail();
    }

    // testing the bound of loaded questions
    assertThrows(DatabaseAccessException.class, () -> dbService.nextQuestion(gameId));
  }


  @Test
  void testGetGameLeaderboards_GetCurrentGameParticipantsNumber() throws SQLException, DatabaseAccessException {
    // creating game
    Integer gameId = dbService.createGame("SESSION", 1, 10, 4, 1);

    // testing not existing game
    assertThrows(DatabaseAccessException.class, () -> dbService.getGameLeaderboards(145));
    assertThrows(DatabaseAccessException.class, () -> dbService.getCurrentParticipantsNumber(145));

    // testing
    dbService.setCurrentGame("SESSION", gameId);
    assertEquals(1, dbService.getCurrentParticipantsNumber(gameId));
    dbService.setCurrentGame("SESSION2", gameId);
    assertEquals(2, dbService.getCurrentParticipantsNumber(gameId));

    dbService.addCurrentGamePoints("SESSION", 100);
    dbService.addCurrentGamePoints("SESSION2", 150);
    assertEquals("[[\"test2\",150],[\"test1\",100]]", dbService.getGameLeaderboards(gameId).toString());
  }

  @Test
  void testStopGame_DeleteGame_GetGlobalLeaderboards() throws SQLException, DatabaseAccessException, JSONException {
    // creating games
    Integer gameId1 = dbService.createGame("SESSION", 1, 5, 5, 1);
    Integer gameId2 = dbService.createGame("SESSION", 1, 5, 5, 1);

    // testing not existing games
    assertThrows(DatabaseAccessException.class, () -> dbService.stopGame(145));
    assertThrows(DatabaseAccessException.class, () -> dbService.deleteGame(145));

    // joining the games
    dbService.setCurrentGame("SESSION", gameId1);
    dbService.setCurrentGame("SESSION2", gameId2);

    // stopping game 1
    assertThrows(DatabaseAccessException.class, () -> dbService.stopGame(145));
    assertDoesNotThrow(() -> dbService.stopGame(gameId1));

    assertEquals(3, dbService.getStatus(gameId1));
    assertEquals(0, dbService.getStatus(gameId2));
    assertNull(dbService.getCurrentGame("SESSION")); // the game is stopped so current game is null
    assertEquals(gameId2, dbService.getCurrentGame("SESSION2"));

    // getting global leaderboards
    dbService.addGlobalPoints("SESSION", 150);
    dbService.addGlobalPoints("SESSION2", 100);
    dbService.addGlobalPossiblePoints("SESSION", 150);
    dbService.addGlobalPossiblePoints("SESSION2", 170);

    JSONArray resultLeaderboard = dbService.getGlobalLeaderboards();
    assertEquals(2, resultLeaderboard.length());
    JSONArray first = resultLeaderboard.getJSONArray(0);
    JSONArray second = resultLeaderboard.getJSONArray(1);

    // checking first
    assertEquals(dbService.getUserId("SESSION"), first.getInt(0));
    assertEquals(dbService.getUsername("SESSION"), first.getString(1));
    assertEquals(150, first.getInt(2));
    assertEquals(150, first.getInt(3));

    // checking second
    assertEquals(dbService.getUserId("SESSION2"), second.getInt(0));
    assertEquals(dbService.getUsername("SESSION2"), second.getString(1));
    assertEquals(100, second.getInt(2));
    assertEquals(170, second.getInt(3));

    // deleting the game
    assertTrue(dbService.checkGameExists(gameId1));
    dbService.deleteGame(gameId1);
    assertFalse(dbService.checkGameExists(gameId1));
    assertTrue(dbService.checkGameExists(gameId2));
  }

  //
  // Achievement TEST UNITS
  //

  Achievement createTestAchievement() {
    Achievement achvm = new Achievement();
    achvm.setName("testAchvm");
    achvm.setProfilePicNeeded(false);
    achvm.setDescriptionNeeded(true);
    achvm.setGamesNumberNeeded(100);
    achvm.setGlobalPointsNeeded(150);
    achvm.setCurrentGameLevelDifficultyNeeded(1);
    return achvm;
  }

  @Test
  void testAddAchievement_GetAchievementById() throws SQLException, DatabaseAccessException {
    // creating achievement
    Achievement achvm = createTestAchievement();
    Integer achvmId = dbService.addAchievement(achvm);
    Achievement resAchvm = dbService.getAchievementById(achvmId);

    // checking fields
    assertEquals(resAchvm.getName(), achvm.getName());
    assertEquals(resAchvm.isProfilePicNeeded(), achvm.isProfilePicNeeded());
    assertEquals(resAchvm.isDescriptionNeeded(), achvm.isDescriptionNeeded());
    assertEquals(resAchvm.getGamesNumberNeeded(), achvm.getGamesNumberNeeded());
    assertEquals(resAchvm.getGlobalPointsNeeded(), achvm.getGlobalPointsNeeded());
    assertEquals(resAchvm.getCurrentGameLevelDifficultyNeeded(), achvm.getCurrentGameLevelDifficultyNeeded());
  }

  @Test
  void testCheckAchievementAchieved_AttachAchievement_GetAchievementsOf() throws SQLException, DatabaseAccessException {
    // creating achievement
    Achievement achvm = createTestAchievement();
    Integer achvmId1 = dbService.addAchievement(achvm);


    // creating "achieved" object
    Achievement test1 = new Achievement();
    test1.setProfilePicNeeded(true);
    test1.setDescriptionNeeded(false);
    test1.setGamesNumberNeeded(100);
    test1.setGlobalPointsNeeded(150);
    Integer[] emptyArr = new Integer[0];

    // testing checkAchievementAchieved
    assertArrayEquals(emptyArr, dbService.checkAchievementAchieved("SESSION", test1));
    // reformatting "achieved" object
    test1.setProfilePicNeeded(false);
    test1.setDescriptionNeeded(true);
    Integer[] testArr1 = {achvmId1};
    assertArrayEquals(testArr1, dbService.checkAchievementAchieved("SESSION", test1));

    // testing getAchievementsOf and AttachAchievement
    assertArrayEquals(emptyArr, dbService.getAchievementsOf("SESSION"));
    dbService.attachAchievement("SESSION", achvmId1);
    assertArrayEquals(testArr1, dbService.getAchievementsOf("SESSION")); // attached only achvm1 achievement
    Integer achvmId2 = dbService.addAchievement(achvm); // creating auxiliary achievement to check multiple options
    Integer[] testArr2 = {achvmId1, achvmId2};
    dbService.attachAchievement("SESSION", achvmId2); // here we attach the test achievement
    assertArrayEquals(testArr2, dbService.getAchievementsOf("SESSION")); // here must be init and test achievements

    // testing not existing sessions
    assertThrows(DatabaseAccessException.class, () -> dbService.getAchievementsOf("NOT_EXISTING_SESSION"));

    // testing other users don't have these achievements
    assertArrayEquals(emptyArr, dbService.getAchievementsOf("SESSION2"));
    Integer[] testArr3 = {achvmId2};
    dbService.attachAchievement("SESSION2", achvmId2); // attaching to the other user
    assertArrayEquals(testArr3, dbService.getAchievementsOf("SESSION2"));
  }

  @Test
  void testGetAllAchievements() throws SQLException, DatabaseAccessException {
    // creating achievement
    Achievement achvm = createTestAchievement();
    dbService.addAchievement(achvm);

    Achievement achvm1 = new Achievement(); // created above
    achvm1.setName("testAchvm");
    achvm1.setProfilePicNeeded(false);
    achvm1.setDescriptionNeeded(true);
    achvm1.setGamesNumberNeeded(100);
    achvm1.setGlobalPointsNeeded(150);
    achvm1.setCurrentGameLevelDifficultyNeeded(1);

    Achievement[] res = dbService.getAllAchievements();
    assertEquals(1, res.length);
    assertEquals(res[0].getName(), achvm1.getName());
    assertEquals(res[0].isProfilePicNeeded(), achvm1.isProfilePicNeeded());
    assertEquals(res[0].isDescriptionNeeded(), achvm1.isDescriptionNeeded());
    assertEquals(res[0].getGamesNumberNeeded(), achvm1.getGamesNumberNeeded());
    assertEquals(res[0].getGlobalPointsNeeded(), achvm1.getGlobalPointsNeeded());
    assertEquals(res[0].getCurrentGameLevelDifficultyNeeded(), achvm1.getCurrentGameLevelDifficultyNeeded());
  }

  @Test
  void testRemoveAchievement() throws SQLException, DatabaseAccessException {
    // creating achievement
    Achievement achvm = createTestAchievement();
    Integer achvmId = dbService.addAchievement(achvm);

    dbService.removeAchievement(achvmId);
    // testing it not exists anymore
    assertThrows(DatabaseAccessException.class, () -> dbService.getAchievementById(achvmId));
  }

  //
  // Topics TEST UNITS
  //

  Topic createTestTopic(String name) {
    Topic topic = new Topic();
    topic.setName(name);
    return topic;
  }

  @Test
  void testAddTopic_GetTopicById() throws SQLException, DatabaseAccessException {
    // creating topics
    Topic topic1 = createTestTopic("testTopic1");
    Topic topic2 = createTestTopic("testTopic2");
    Integer topicId1 = dbService.addTopic(topic1);
    Integer topicId2 = dbService.addTopic(topic2);

    // testing getTopicById
    Topic res1 = dbService.getTopicById(topicId1);
    if (!Objects.equals(res1.getName(), topic1.getName())) {
      fail("Topic1 dont matches data in DB");
    }
    Topic res2 = dbService.getTopicById(topicId2);
    if (!Objects.equals(res2.getName(), topic2.getName())) {
      fail("Topic2 dont matches data in DB");
    }
  }

  @Test
  void testGetAllTopics() throws SQLException, DatabaseAccessException {
    // creating topics
    Topic topic1 = createTestTopic("testTopic1");
    Topic topic2 = createTestTopic("testTopic2");
    dbService.addTopic(topic1);
    dbService.addTopic(topic2);

    // testing getAllTopics
    Topic[] res = dbService.getAllTopics();
    if (
        Objects.equals(res[0].getName(), "testTopic") && // initialized in DB
            Objects.equals(res[1].getName(), topic1.getName()) &&
            Objects.equals(res[2].getName(), topic2.getName())
    ) {
      assertTrue(true);
    } else {
      fail("Data dont match");
    }
  }

  @Test
  void testRemoveTopic() throws SQLException, DatabaseAccessException {
    // creating topics
    Topic topic1 = createTestTopic("testTopic1");
    Topic topic2 = createTestTopic("testTopic2");
    Integer topicId1 = dbService.addTopic(topic1);
    Integer topicId2 = dbService.addTopic(topic2);

    // testing removeTopic
    dbService.removeTopic(topicId1);
    assertThrows(DatabaseAccessException.class, () -> dbService.getTopicById(topicId1));

    // checking topic2 left
    assertNotNull(dbService.getTopicById(topicId2));
  }
}
