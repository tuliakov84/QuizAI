package com.mipt.api;

import com.mipt.domainModel.*;

import java.util.List;

public interface IApiController {

  User register(String username, String password);
  String login(String username, String password);
  void logout(String session);

  User getUserBySession(String session);
  void updateProfilePic(String session, int picId);
  void updateDescription(String session, String description);

  Game createGame(String session, int topicId, Game.LevelDifficulty level, boolean isPrivate);
  List<Game> listWaitingGames();
  void joinGame(String session, int gameId);
  void startGame(String session, int gameId);
  void pauseGame(String session, int gameId);
  void endGame(String session, int gameId);

  Question getNextQuestion(String session, int gameId);
  boolean answerQuestion(String session, int gameId, int answerNumber);

  List<Achievement> getUserAchievements(String session);
  int getUserCurrentRating (String session);
}
