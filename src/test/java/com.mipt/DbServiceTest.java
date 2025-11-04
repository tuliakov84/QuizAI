package com.mipt;

import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.Achievement;
import com.mipt.domainModel.Question;
import com.mipt.domainModel.Topic;
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

  // Users TEST UNITS

  @Test
  @Order(1)
  void testRegister_CheckUserExists() throws SQLException {
    assertFalse(dbService.checkUserExists("testUser"));
    assertTrue(dbService.register("testUser", "12345"));
    assertTrue(dbService.checkUserExists("testUser"));

    assertFalse(dbService.register("testUser", "12345678"));
    assertTrue(dbService.register("test2", "1234"));
    dbService.register("test3", "111");
    dbService.register("test4", "1111");
    dbService.authorize("test3", "111", "SESSION3");
    dbService.authorize("test4", "1111", "SESSION4");
  }

  @Test
  @Order(2)
  void testAuthorize_CheckedLoggedIn() throws SQLException {
    assertNull(dbService.getUserId("SESSION"));
    assertFalse(dbService.authorize("notExistingTestUser", "1234", "SESSION"));
    assertNull(dbService.getUserId("SESSION"));
    assertFalse(dbService.authorize("testUser", "12345wrong", "SESSION"));
    assertNull(dbService.getUserId("SESSION"));
    assertTrue(dbService.authorize("testUser", "12345", "SESSION"));
    assertNotNull(dbService.getUserId("SESSION"));
    assertFalse(dbService.authorize("testUser", "12345", "SESSION"));

    assertTrue(dbService.authorize("test2", "1234", "SESSION2"));
  }

  @Test
  @Order(3)
  void testLogOut() throws SQLException {
    assertNotNull(dbService.getUserId("SESSION"));
    dbService.logOut("SESSION");
    dbService.logOut("SESSION2");

    assertNull(dbService.getUserId("SESSION"));
    assertTrue(dbService.authorize("testUser", "12345", "SESSION"));

    assertNull(dbService.getUserId("SESSION2"));
    assertTrue(dbService.authorize("test2", "1234", "SESSION2"));
  }

  @Test
  @Order(4)
  void testChangePassword() throws SQLException {
    dbService.changePassword("SESSION", "12345new");
    dbService.logOut("SESSION");
    assertFalse(dbService.authorize("testUser", "12345", "SESSION"));
    assertTrue(dbService.authorize("testUser", "12345new", "SESSION"));

    dbService.logOut("SESSION2");
    assertFalse(dbService.authorize("test2", "1234", "SESSION"));
    assertTrue(dbService.authorize("test2", "1234", "SESSION2"));
  }

  @Test
  void testChangeProfilePic_GetProfilePic() throws SQLException {
    assertEquals(0, dbService.getProfilePic("NOT_EXISTING_SESSION"));
    assertEquals(0, dbService.getProfilePic("SESSION"));
    dbService.changeProfilePic("SESSION", 1);
    dbService.changeProfilePic("SESSION2", 2);
    assertEquals(1, dbService.getProfilePic("SESSION"));
    assertEquals(2, dbService.getProfilePic("SESSION2"));
  }

  @Test
  void testChangeDescription_GetDescription() throws SQLException {
    assertEquals("", dbService.getDescription("NOT_EXISTING_SESSION"));
    assertEquals("", dbService.getDescription("SESSION"));
    dbService.changeDescription("SESSION", "testDescription");
    assertEquals("testDescription", dbService.getDescription("SESSION"));

    assertEquals("", dbService.getDescription("SESSION2"));
  }

  @Test
  void testSetLastActivity_GetLastActivity() throws SQLException {
    assertNull(dbService.getLastActivity("NOT_EXISTING_SESSION"));
    assertNull(dbService.getLastActivity("SESSION"));
    dbService.setLastActivity("SESSION", Instant.ofEpochSecond(1));
    assertNull(dbService.getLastActivity("SESSION2"));
    dbService.setLastActivity("SESSION2", Instant.ofEpochSecond(2));

    assertEquals(Timestamp.from(Instant.ofEpochSecond(1)), dbService.getLastActivity("SESSION"));
    assertEquals(Timestamp.from(Instant.ofEpochSecond(2)), dbService.getLastActivity("SESSION2"));
  }

  @Test
  @Order(5)
  void testAddGamePlayed_GetGamesPlayed_notInGame() throws SQLException {
    // tests working OUT OF THE GAME
    dbService.addGamePlayed("NOT_EXISTING_SESSION");
    assertNull(dbService.getGamesPlayed("NOT_EXISTING_SESSION"));
    dbService.addGamePlayed("SESSION");
    assertNull(dbService.getGamesPlayed("SESSION")); // NOT IN GAME
  }

  @Test
  @Order(6)
  void testSetCurrentGame_GetCurrentGame() throws SQLException {
    assertNull(dbService.getCurrentGame("NOT_EXISTING_SESSION"));
    assertNull(dbService.getCurrentGame("SESSION"));
    dbService.setCurrentGame("SESSION", 1);

    assertEquals(1, dbService.getCurrentGame("SESSION"));
    assertNull(dbService.getCurrentGame("SESSION2"));
  }

  @Test
  @Order(7)
  void testAddGamePlayed_GetGamesPlayed_inGame() throws SQLException {
    dbService.addGamePlayed("NOT_EXISTING_SESSION");
    assertNull(dbService.getGamesPlayed("NOT_EXISTING_SESSION"));
    dbService.addGamePlayed("SESSION");
    dbService.addGamePlayed("SESSION2");
    Integer[] testArr1 = {1};
    assertArrayEquals(testArr1, dbService.getGamesPlayed("SESSION"));
    assertNull(dbService.getCurrentGame("SESSION2"));
  }

  @Test
  void testAddGlobalPoints_GetGlobalPoints() throws SQLException {
    assertNull(dbService.getGlobalPoints("NOT_EXISTING_SESSION"));
    assertEquals(0, dbService.getGlobalPoints("SESSION"));
    dbService.addGlobalPoints("SESSION", 5);
    dbService.addGlobalPoints("SESSION", 5);
    assertEquals(10, dbService.getGlobalPoints("SESSION"));

    assertEquals(0, dbService.getGlobalPoints("SESSION2"));
  }

  @Test
  void testAddGlobalPoints_GetGlobalPossiblePoints() throws SQLException {
    assertNull(dbService.getGlobalPossiblePoints("NOT_EXISTING_SESSION"));
    assertEquals(0, dbService.getGlobalPossiblePoints("SESSION"));
    dbService.addGlobalPossiblePoints("SESSION", 5);
    dbService.addGlobalPossiblePoints("SESSION", 5);
    assertEquals(10, dbService.getGlobalPossiblePoints("SESSION"));

    assertEquals(0, dbService.getGlobalPossiblePoints("SESSION2"));
  }

  @Test
  void testAddCurrentGamePoints_GetCurrentGamePoints() throws SQLException {
    assertNull(dbService.getCurrentGamePoints("NOT_EXISTING_SESSION"));
    assertEquals(0, dbService.getCurrentGamePoints("SESSION"));
    dbService.addCurrentGamePoints("SESSION", 5);
    dbService.addCurrentGamePoints("SESSION", 5);
    assertEquals(10, dbService.getCurrentGamePoints("SESSION"));

    assertEquals(0, dbService.getCurrentGamePoints("SESSION2"));
  }


  // Game TEST UNITS
  @Test
  @Order(8)
  void testCreateGame_CheckGameExists() throws SQLException {
    assertNull(dbService.createGame("NOT_EXISTING_SESSION", 1, 1, 4, 1));
    assertNull(dbService.createGame("SESSION", 4, 1, 4, 1));
    assertNull(dbService.createGame("SESSION", 1, 1, -1, 1));
    assertTrue(dbService.checkGameExists(1)); // test game with topicId=1 is already initialized in sql
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
  void testSetStatus_GetStatus() throws SQLException {
    assertNull(dbService.getStatus(52));
    assertEquals(0, dbService.getStatus(2));
    dbService.setStatus(2, 2);
    assertEquals(2, dbService.getStatus(2));
  }

  @Test
  void testSetPrivate_GetPrivate() throws SQLException {
    assertNull(dbService.getPrivate(52));
    assertTrue(dbService.getPrivate(2));
    dbService.setPrivate(2, false);
    assertFalse(dbService.getPrivate(2));
  }

  @Test
  void testGetGameStartTime_SetGameStartTime() throws SQLException {
    assertNull(dbService.getGameStartTime(52));
    dbService.setGameStartTime(2, Instant.ofEpochSecond(1));
    assertEquals(Timestamp.from(Instant.ofEpochSecond(1)), dbService.getGameStartTime(2));
  }

  @Test
  void testGetGameEndTime_SetGameEndTime() throws SQLException {
    assertNull(dbService.getGameEndTime(52));
    dbService.setGameEndTime(2, Instant.ofEpochSecond(1));
    assertEquals(Timestamp.from(Instant.ofEpochSecond(1)), dbService.getGameEndTime(2));
  }

  @Test
  @Order(9)
  void testLoadQuestions_NextQuestion_GetCurrentQuestion_GetRightAnswer() throws SQLException {
    assertNull(dbService.getCurrentQuestionNumber(52));
    Integer gameId = dbService.createGame("SESSION3", 1, 10, 4, 1);

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
    dbService.loadQuestions(gameId, jsonObject);
    assertEquals(0, dbService.getCurrentQuestionNumber(gameId));
    Question result1 = dbService.nextQuestion(gameId);
    assertEquals(1, dbService.getCurrentQuestionNumber(gameId));

    if (
        (
        Objects.equals(result1.questionText, "What the Aglet is?") &&
            Objects.equals(result1.answer1, "American audit company") &&
            Objects.equals(result1.answer2, "Programming language") &&
            Objects.equals(result1.answer3, "Lace tip") &&
            Objects.equals(result1.answer4, "idk")
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
        Objects.equals(result2.questionText, "What is the punishment for driving drunk in Russia?") &&
            Objects.equals(result2.answer1, "100 USD") &&
            Objects.equals(result2.answer2, "Deprivation of driving license") &&
            Objects.equals(result2.answer3, "Warning") &&
            Objects.equals(result2.answer4, "None")
        ) && (
            dbService.getRightAnswer(gameId, 2).equals(2)
        )
    ) {
      assertTrue(true);
    } else {
      fail();
    }

    // getRightAnswer
    assertNull(dbService.getRightAnswer(52, 2));
    assertNotEquals(2, dbService.getRightAnswer(gameId, 52));
    assertNotEquals(1, dbService.getRightAnswer(gameId, 2));
    assertEquals(3, dbService.getRightAnswer(gameId, 1));
    assertEquals(2, dbService.getRightAnswer(gameId, 2));

    assertNull(dbService.nextQuestion(gameId));
  }


  @Test
  void testGetGameLeaderboards() throws SQLException {
    dbService.setCurrentGame("SESSION3", 2);
    dbService.setCurrentGame("SESSION4", 2);
    dbService.addCurrentGamePoints("SESSION3", 150);
    dbService.addCurrentGamePoints("SESSION4", 100);
    assertEquals("[[\"test3\",150],[\"test4\",100]]", dbService.getGameLeaderboards(2).toString());
  }


  @AfterAll
  static void testStopGame_DeleteGame_GetGlobalLeaderboards() throws SQLException {
    dbService.createGame("SESSION", 1, 5, 5, 1);
    dbService.setCurrentGame("SESSION", 3);
    dbService.setCurrentGame("SESSION2", 2);

    // stopping game
    dbService.stopGame(52);
    dbService.stopGame(2);

    assertEquals(3, dbService.getStatus(2));
    assertEquals(0, dbService.getStatus(3));
    assertNull(dbService.getCurrentGame("SESSION2"));
    assertEquals(3, dbService.getCurrentGame("SESSION"));
    assertNull(dbService.getRightAnswer(2, 2));

    // deleting game
    dbService.deleteGame(2);
    assertNull(dbService.getPreset(2));
    assertNotNull(dbService.getPreset(3));

    // getting global leaderboards
    dbService.addGlobalPoints("SESSION3", 150);
    dbService.addGlobalPoints("SESSION4", 100);
    dbService.addGlobalPossiblePoints("SESSION3", 150);
    dbService.addGlobalPossiblePoints("SESSION4", 150);

    assertEquals("[[3,\"test3\",150,150],[4,\"test4\",100,150],[1,\"testUser\",10,10],[2,\"test2\",0,0]]",
        dbService.getGlobalLeaderboards().toString());
  }

  // Achievement TEST UNITS

  @Test
  @Order(10)
  void testAddAchievement_GetAchievementById() throws SQLException {
    Achievement achvm = new Achievement();
    achvm.name = "testAchvm";
    achvm.profilePicNeeded = false;
    achvm.descriptionNeeded = true;
    achvm.gamesNumberNeeded = 100;
    achvm.globalPointsNeeded = 150;
    achvm.currentGameLevelDifficultyNeeded = 1;
    Integer achvmId = dbService.addAchievement(achvm);
    Achievement resAchvm = dbService.getAchievementById(achvmId);
    if (
        resAchvm.name.equals(achvm.name) &&
            resAchvm.profilePicNeeded == achvm.profilePicNeeded &&
            resAchvm.descriptionNeeded == achvm.descriptionNeeded &&
            Objects.equals(achvm.gamesNumberNeeded, resAchvm.gamesNumberNeeded) &&
            Objects.equals(achvm.globalPointsNeeded, resAchvm.globalPointsNeeded) &&
            Objects.equals(achvm.currentGameLevelDifficultyNeeded, resAchvm.currentGameLevelDifficultyNeeded)
    ) {
      assertTrue(true);
    } else {
      fail();
    }
  }

  @Test
  void testCheckAchievementAchieved_AttachAchievement_GetAchievementsOf() throws SQLException {
    Achievement test1 = new Achievement();
    test1.profilePicNeeded = true;
    test1.descriptionNeeded = false;
    test1.gamesNumberNeeded = 100;
    test1.globalPointsNeeded = 150;
    Integer[] emptyArr = new Integer[0];
    assertArrayEquals(emptyArr, dbService.checkAchievementAchieved("SESSION3", test1));

    test1.profilePicNeeded = false;
    test1.descriptionNeeded = true;
    Integer[] testArr1 = {2};
    assertArrayEquals(testArr1, dbService.checkAchievementAchieved("SESSION3", test1));

    assertArrayEquals(emptyArr, dbService.getAchievementsOf("SESSION3"));
    dbService.attachAchievement("SESSION3", 2);
    assertArrayEquals(testArr1, dbService.getAchievementsOf("SESSION3"));
    Integer[] testArr2 = {1, 2};
    dbService.attachAchievement("SESSION3", 1);
    assertArrayEquals(testArr2, dbService.getAchievementsOf("SESSION3"));
  }

  @Test
  void testGetAllAchievements() throws SQLException {
    Achievement achvm1 = new Achievement(); // initialized in init.sql
    achvm1.name = "testAchievement";
    Achievement achvm2 = new Achievement(); // initialized in addAchievement test
    achvm2.name = "testAchvm";
    achvm2.profilePicNeeded = false;
    achvm2.descriptionNeeded = true;
    achvm2.gamesNumberNeeded = 100;
    achvm2.globalPointsNeeded = 150;
    achvm2.currentGameLevelDifficultyNeeded = 1;

    Achievement[] res = dbService.getAllAchievements();
    if (
        Objects.equals(res[0].name, achvm1.name) &&
            Objects.equals(res[1].name, achvm2.name) &&
            res[1].profilePicNeeded == achvm2.profilePicNeeded &&
            res[1].descriptionNeeded == achvm2.descriptionNeeded &&
            res[1].gamesNumberNeeded == achvm2.gamesNumberNeeded &&
            res[1].globalPointsNeeded == achvm2.globalPointsNeeded &&
            res[1].currentGameLevelDifficultyNeeded == achvm2.currentGameLevelDifficultyNeeded
    ) {
      assertTrue(true);
    } else {
      fail();
    }
  }

  @AfterAll
  static void testRemoveAchievement() throws SQLException {
    dbService.removeAchievement(2);
    assertNull(dbService.getAchievementById(2));
  }

  // Topic TEST UNITS

  @Test
  @Order(11)
  void testAddTopic_GetTopicById() throws SQLException {
    assertNotNull(dbService.getTopicById(1));
    assertNull(dbService.getTopicById(2));
    Topic topic = new Topic();
    topic.name = "newTopic";
    assertEquals(2, dbService.addTopic(topic));
    Topic res = dbService.getTopicById(2);
    if (!Objects.equals(res.name, topic.name)) {
      fail();
    }
  }

  @Test
  void testGetAllTopics() throws SQLException {
    Topic topic1 = new Topic();
    topic1.name = "testTopic";
    Topic topic2 = new Topic();
    topic2.name = "newTopic";
    Topic[] res = dbService.getAllTopics();
    if (
        Objects.equals(res[0].name, topic1.name) &&
            Objects.equals(res[1].name, topic2.name)
    ) {
      assertTrue(true);
    } else {
      fail();
    }
  }

  @AfterAll
  static void testRemoveTopic() throws SQLException {
    dbService.removeTopic(2);
    assertNull(dbService.getTopicById(2));
  }
}
