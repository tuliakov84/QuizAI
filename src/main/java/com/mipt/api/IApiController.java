package com.mipt.api;

import com.mipt.domainModel.*;
import com.mipt.utils.AccessException;

import java.time.Instant;
import java.util.List;

public interface IApiController {
  User login(String username, String password);
  User register(String username, String password);
  void logout(String session);

  User getUser(String session);
  User updateUser(String session, int newPicId, String newDescription, String newUsername);

  List<Game> getLast5GamesPlayed(String session);
  List<Game> getAllGamesPlayed(String session);

  void joinGame(String session, int gameId);
  List<Topic> getAllTopics();
  List<Game> getListOfOpenGames();

  Game createGame(int difficulty, boolean isPrivate, int participantsNumber,
                  int numberOfQuestions, String topic);
  void joinGame();
}
