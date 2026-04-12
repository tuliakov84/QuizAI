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
            "Стандартный AI Arena квиз с регулярными вопросами.",
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
            "Квиз с ответами формата Правда/Ложь.",
            2,
            12,
            1,
            50,
            false,
            true
        )
    );
    definitions.put(
        GameMode.DUEL,
        new GameModeDefinition(
            GameMode.DUEL,
            "Дуэль",
            "Матч 1 на 1 со ставками, банком и персонализированными вопросами.",
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
            "Одиночный риск-режим со скрытым коэффициентом выигрыша.",
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
            "Компания игроков, раунды, таблица тем и специальные задания.",
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
