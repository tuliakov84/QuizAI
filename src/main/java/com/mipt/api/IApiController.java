package com.mipt.api;

import com.mipt.domainModel.*;
import com.mipt.utils.AccessException;

import java.time.Instant;
import java.util.List;

public interface IApiController {
  //User authentification
  String register(String userName, String password);
  String login(String userName, String password) throws AccessException;
  void logOut(String session);

  //Show UserData
  String getUsername(String session);
  int getProfilePicId(String session);
  int getCurrentGlobalPoints(String session);
  int getGlobalPossiblePoints(String session);
  String getDescription(String session);
  List<Achievement> getAchievements(String session);
  List<Game> getGamesPlayed(String session);


  //Editing profile
  User changeUserProfilePic(String session, int picId);
  User changeUsername(String session, String newUserName);
  User changePassword(String session, String oldPassword, String newPassword);
  User changeDescription(String session, String newDescription);

  //Host operations
  Game createGame(String hostSession, int topicId, Game.LevelDifficulty level,
                  boolean isPrivate, int maxAmountOfPlayers);
  Game changeMaxAmountOfPlayersInGame(String hostSession, int newMaxAmountOfPlayers);
  Question startGame(String hostSession);
  void deleteGame(String hostSession);
  void pauseGame(String hostSession);
  void resumeGame(String hostSession);

  //Nonhost operations
  List<Game> getListOfOpenGames();
  User joinGame(String session, int gameId);
  User leftGame(String session);
  List<User> getListOfPlayersInCurrentGame(int gameId);

  //Game service
  Question getNextQuestion(int questionId, int gameId);
  void getAnswer(String session, int answerNumber, Instant wastedTime);
  int getCurrentGamePoints(String session);
  void endGame(int gameId);//should it be in ApiController?

  //Show leaderboard
  List<User> getGlobalLeaderboard();
}
