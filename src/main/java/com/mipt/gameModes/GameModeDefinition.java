package com.mipt.gameModes;

public class GameModeDefinition {
  private final GameMode mode;
  private final String title;
  private final String description;
  private final int minParticipants;
  private final int maxParticipants;
  private final int minQuestions;
  private final int maxQuestions;
  private final boolean usesStandardQuizFlow;
  private final boolean usesCoinBank;

  public GameModeDefinition(
      GameMode mode,
      String title,
      String description,
      int minParticipants,
      int maxParticipants,
      int minQuestions,
      int maxQuestions,
      boolean usesStandardQuizFlow,
      boolean usesCoinBank
  ) {
    this.mode = mode;
    this.title = title;
    this.description = description;
    this.minParticipants = minParticipants;
    this.maxParticipants = maxParticipants;
    this.minQuestions = minQuestions;
    this.maxQuestions = maxQuestions;
    this.usesStandardQuizFlow = usesStandardQuizFlow;
    this.usesCoinBank = usesCoinBank;
  }

  public GameMode getMode() {
    return mode;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public int getMinParticipants() {
    return minParticipants;
  }

  public int getMaxParticipants() {
    return maxParticipants;
  }

  public int getMinQuestions() {
    return minQuestions;
  }

  public int getMaxQuestions() {
    return maxQuestions;
  }

  public boolean getUsesStandardQuizFlow() {
    return usesStandardQuizFlow;
  }

  public boolean getUsesCoinBank() {
    return usesCoinBank;
  }
}
