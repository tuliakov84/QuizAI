package com.mipt.api;

import com.mipt.domainModel.*;

import java.time.Instant;
import java.util.List;

public interface IApiController {
  //User authentification
  User register(String userName, String password, String session);
  User login(String userName, String password, String session);
  void logout(String session);

  //Show UserData
  String getUsername(String session);
  int getProfilePicId(String session);
  int getCurrentGlobalPoints(String session);
  int getGlobalPossiblePoints(String session);
  List<Achievement> getAchievements(String session);
  List<Game> getGamesPlayed(String session);


  //Editing profile
  User updateUserProfilePic(String session, int picId);
  User updateUsername(String session, String newUserName);
  User updatePassword(String session, String oldPassword, String newPassword);
  User updateDescription(String session, String newDescription);

  //Host operations
  Game createGame(String hostSession, int topicId, Game.LevelDifficulty level,
                  boolean isPrivate, int maxAmounOfPlayers, Instant timeToAnswer);
  Game updateMaxAmountOfPlayersInGame(String hostSession, int newMaxAmountOfPlayers);
  Question startGame(String hostSession);
  void deleteGame(String hostSession);
  void pauseGame(String hostSession);
  void unpauseGame(String hostSession);

  //Nonhost operations
  List<Game> getListOfWaitingGames();
  User joinGame(String session, int gameId);
  User leftGame(String session);
  List<User> getListOfPlayersInCurrentGame(int gameId);

  //Game service
  Question getNextQuestion(int questionId, int gameId);
  void getAnswer(String session, int answerNumber, Instant timeToAnswer);
  int getCurrentGamePoints(String session);
  void endGame(int gameId);//should it be in ApiController?

  //Show leaderboard
  List<User> getBestPlayers();
}
