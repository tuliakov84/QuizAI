package com.mipt.gameModes;

import com.mipt.dbAPI.DatabaseAccessException;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class GameModeCatalog {
  private static final Map<GameMode, GameModeDefinition> DEFINITIONS = buildDefinitions();

  private GameModeCatalog() {
  }

  public static GameMode defaultMode() {
    return GameMode.CASUAL;
  }

  public static GameMode normalize(GameMode mode) {
    return mode == null ? defaultMode() : mode;
  }

  public static GameModeDefinition get(GameMode mode) {
    return DEFINITIONS.get(normalize(mode));
  }

  public static List<GameModeDefinition> getAll() {
    return Arrays.stream(GameMode.values())
        .map(DEFINITIONS::get)
        .toList();
  }

  public static boolean usesStandardQuizFlow(GameMode mode) {
    return get(mode).getUsesStandardQuizFlow();
  }

  public static void validateCreateRequest(
      GameMode mode,
      int levelDifficulty,
      int numberOfQuestions,
      int participantsNumber
  ) throws DatabaseAccessException {
    GameModeDefinition definition = get(mode);

    if (!(levelDifficulty >= 1 && levelDifficulty <= 3)) {
      throw new DatabaseAccessException("Bad params");
    }

    if (participantsNumber < definition.getMinParticipants()
        || participantsNumber > definition.getMaxParticipants()) {
      throw new DatabaseAccessException(
          "Participants number is out of bounds for mode " + definition.getMode()
      );
    }

    if (numberOfQuestions < definition.getMinQuestions()
        || numberOfQuestions > definition.getMaxQuestions()) {
      throw new DatabaseAccessException(
          "Questions number is out of bounds for mode " + definition.getMode()
      );
    }
  }

  private static Map<GameMode, GameModeDefinition> buildDefinitions() {
    Map<GameMode, GameModeDefinition> definitions = new EnumMap<>(GameMode.class);
    definitions.put(
        GameMode.CASUAL,
        new GameModeDefinition(
            GameMode.CASUAL,
            "Казуал",
            "Обычный квиз для 2-12 игроков: тестовые вопросы, очки рейтинга и +10 коинов за верный ответ до лимита 300 коинов в день.",
            "2-12 человек проходят регулярный квиз, состоящий из тестовых вопросов. "
                + "За правильный ответ игрок получает 10 коинов. "
                + "Максимально таким способом можно заработать 300 коинов в день.",
            2,
            12,
            1,
            50,
            true,
            true
        )
    );
    definitions.put(
        GameMode.TRUE_FALSE,
        new GameModeDefinition(
            GameMode.TRUE_FALSE,
            "Правда или ложь",
            "Быстрый квиз для 2-12 игроков: на каждый вопрос нужно выбрать «Правда» или «Ложь», +5 коинов за верный ответ до лимита 150 в день.",
            "2-12 человек проходят регулярный квиз, состоящий из вопросов, требующих ответа Правда/Ложь.\n"
                + "За правильный ответ игрок получает 5 коинов.\n"
                + "Максимально таким способом можно заработать 150 коинов в день.",
            2,
            12,
            1,
            50,
            true,
            true
        )
    );
    definitions.put(
        GameMode.DUEL,
        new GameModeDefinition(
            GameMode.DUEL,
            "Дуэль",
            "Матч 1 на 1 на коины: анте, рейзы, фолд и вопросы, подобранные под историю двух соперников.",
            "2 человека соревнуются, играя на коины. Задача победителя — ответить на такой вопрос, на который не ответит оппонент.",
            2,
            2,
            1,
            50,
            false,
            true
        )
    );
    definitions.put(
        GameMode.RANDOM,
        new GameModeDefinition(
            GameMode.RANDOM,
            "Рандом",
            "Одиночный риск-режим: ставка, один случайный вопрос и скрытый коэффициент выигрыша.",
            "Одиночный риск-режим: игрок делает ставку, отвечает на один случайный вопрос и получает выигрыш с учетом скрытого коэффициента.",
            1,
            1,
            1,
            50,
            false,
            true
        )
    );
    definitions.put(
        GameMode.SVOYAK,
        new GameModeDefinition(
            GameMode.SVOYAK,
            "Свояк",
            "Игра для компании: таблица тем, цены вопросов, 3 обычных раунда, финал и общий банк коинов.",
            "Игра для компании с таблицей тем и стоимостью вопросов: 3 обычных раунда, финал и общий банк коинов.",
            2,
            12,
            1,
            100,
            false,
            true
        )
    );
    return definitions;
  }
}
