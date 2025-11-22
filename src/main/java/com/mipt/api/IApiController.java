package com.mipt.api;

import com.mipt.domainModel.*;

import java.time.Instant;
import java.util.List;

public interface IApiController {
  User auth(String username, String password);
  User register(String username, String password);
  void logout(String session);

  User getUser(String session);
  User updateUser(String session, int newPicId, String newDescription, String newUsername);

  List<Game> getLast5GamesPlayed(String session);
  List<Game> getAllGamesPlayed(String session);

  List<Achievement> getAllAchievements(String session);

  Game joinGame(String session, int gameId);
  List<User> getAllParticipants(int gameId);
  List<Topic> getAllTopics();
  List<Game> getListOfOpenGames();

  Game createGame(int difficulty, boolean isPrivate, int participantsNumber,
                  int numberOfQuestions, String topic);
  Game startGame(String sessionOfAuthor);
  Game changeParticipantsNumber(String sessionOfAuthor, int newParticipantsNumber);

  Game startGameAuto(int gameId);
  void endGame(int gameId);

  List<User> getGlobalLeaderboard();
  List<User> getGameLeaderboard(int gameId);

  Question nextQuestion(int gameId);
  User getAnswer(String session, int answerId, Instant timeToAnswer);

}
