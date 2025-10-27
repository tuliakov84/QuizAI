package com.mipt;

import com.mipt.dbAPI.DbService;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;


public class DbServiceTest {
  // DbService.DbUsers TEST UNITS

  @Test
  void testRegisterAndCheckUserExists() throws SQLException {
    assertFalse(DbService.DbUser.checkUserExists("testUser"));
    assertTrue(DbService.DbUser.register("testUser", "12345"));
    assertTrue(DbService.DbUser.checkUserExists("testUser"));
    assertFalse(DbService.DbUser.register("testUser", "12345678"));
  }

  @Test
  void testAuthorizeAndCheckedLoggedIn() throws SQLException {
    assertNull(DbService.DbUser.checkLoggedIn("SESSION"));
    assertFalse(DbService.DbUser.authorize("notExistingTestUser", "1234", "SESSION"));
    assertNull(DbService.DbUser.checkLoggedIn("SESSION"));
    assertFalse(DbService.DbUser.authorize("testUser", "12345wrong", "SESSION"));
    assertNull(DbService.DbUser.checkLoggedIn("SESSION"));
    assertTrue(DbService.DbUser.authorize("testUser", "12345", "SESSION"));
    assertNotNull(DbService.DbUser.checkLoggedIn("SESSION"));
    assertFalse(DbService.DbUser.authorize("testUser", "12345", "SESSION"));
  }

  @Test
  void testLogOut() throws SQLException {
    assertNotNull(DbService.DbUser.checkLoggedIn("SESSION"));
    DbService.DbUser.logOut("SESSION");
    assertNull(DbService.DbUser.checkLoggedIn("SESSION"));
    assertTrue(DbService.DbUser.authorize("testUser", "12345", "SESSION"));
  }

  @Test
  void testChangePassword() throws SQLException {
    DbService.DbUser.changePassword("SESSION", "12345new");
    DbService.DbUser.logOut("SESSION");
    assertFalse(DbService.DbUser.authorize("testUser", "12345", "SESSION"));
    assertTrue(DbService.DbUser.authorize("testUser", "12345new", "SESSION"));
  }

  @Test
  void testChangeProfilePicAndGetProfilePic() throws SQLException {
    assertEquals(0, DbService.DbUser.getProfilePic("NOT_EXISTING_SESSION"));
    assertEquals(0, DbService.DbUser.getProfilePic("SESSION"));
    DbService.DbUser.changeProfilePic("SESSION", 1);
    assertEquals(1, DbService.DbUser.getProfilePic("SESSION"));
  }

  @Test
  void testChangeDescriptionAndGetDescription() throws SQLException {
    assertEquals("", DbService.DbUser.getDescription("NOT_EXISTING_SESSION"));
    assertEquals("", DbService.DbUser.getDescription("SESSION"));
    DbService.DbUser.changeDescription("SESSION", "testDescription");
    assertEquals("testDescription", DbService.DbUser.getDescription("SESSION"));
  }

  @Test
  void testSetLastActivityAndGetLastActivity() throws SQLException {
    assertNull(DbService.DbUser.getLastActivity("NOT_EXISTING_SESSION"));
    assertNull(DbService.DbUser.getLastActivity("SESSION"));
    DbService.DbUser.setLastActivity("SESSION", Instant.ofEpochSecond(1));
    assertEquals(Timestamp.from(Instant.ofEpochSecond(1)), DbService.DbUser.getLastActivity("SESSION"));
  }

  @Test
  void testSetCurrentGameAndGetCurrentGame() throws SQLException {
    assertNull(DbService.DbUser.getCurrentGame("NOT_EXISTING_SESSION"));
    assertNull(DbService.DbUser.getCurrentGame("SESSION"));
    DbService.DbUser.setCurrentGame("SESSION", 1);
    assertEquals(1, DbService.DbUser.getCurrentGame("SESSION"));
  }

  void testAddGamePlayedAndGetGamesPlayed() throws SQLException {
    // FEATURE
  }

  @Test
  void testAddGlobalPointsAndGetGlobalPoints() throws SQLException {
    assertNull(DbService.DbUser.getGlobalPoints("NOT_EXISTING_SESSION"));
    assertEquals(0, DbService.DbUser.getGlobalPoints("SESSION"));
    DbService.DbUser.addGlobalPoints("SESSION", 5);
    assertEquals(5, DbService.DbUser.getGlobalPoints("SESSION"));
  }

  @Test
  void testAddGlobalPointsAndGetGlobalPossiblePoints() throws SQLException {
    assertNull(DbService.DbUser.getGlobalPossiblePoints("NOT_EXISTING_SESSION"));
    assertEquals(0, DbService.DbUser.getGlobalPossiblePoints("SESSION"));
    DbService.DbUser.addGlobalPossiblePoints("SESSION", 5);
    assertEquals(5, DbService.DbUser.getGlobalPossiblePoints("SESSION"));
  }

  @Test
  void testAddCurrentGamePointsAndGetCurrentGamePoints() throws SQLException {
    assertNull(DbService.DbUser.getCurrentGamePoints("NOT_EXISTING_SESSION"));
    assertEquals(0, DbService.DbUser.getCurrentGamePoints("SESSION"));
    DbService.DbUser.addCurrentGamePoints("SESSION", 5);
    assertEquals(5, DbService.DbUser.getCurrentGamePoints("SESSION"));
  }


  // DbService.DbGame TEST UNITS

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

  // DbService.DbAchievement TEST UNITS

  void testAddAchievement() throws SQLException {
    // FEATURE
  }

  void testRemoveAchievement() throws SQLException {
    // FEATURE
  }

  // DbService.DbTopic TEST UNITS

  void testAddTopic() throws SQLException {
    // FEATURE
  }

  void testRemoveTopic() throws SQLException {
    // FEATURE
  }
}
