/**
 *
 * QUIZAI JAVA DB SERVICE
 * Функциональная библиотека, направленная на взаимодействие с базой данных
 * Относится только к сервисам, написанным на языке Java
 * Не путать с PYTHON DB SERVICE
 *
 */



package com.mipt.dbAPI;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.mipt.bank.CoinTransactionType;
import com.mipt.dbAPI.jpa.entity.AchievementEntity;
import com.mipt.dbAPI.jpa.entity.AnsweredCorrectlyQuestionEntity;
import com.mipt.dbAPI.jpa.entity.CoinTransactionEntity;
import com.mipt.dbAPI.jpa.entity.GameEntity;
import com.mipt.dbAPI.jpa.entity.GameHistoryEntity;
import com.mipt.dbAPI.jpa.entity.QuestionEntity;
import com.mipt.dbAPI.jpa.entity.TopicEntity;
import com.mipt.dbAPI.jpa.entity.UserAchievementEntity;
import com.mipt.dbAPI.jpa.entity.UserEntity;
import com.mipt.dbAPI.jpa.repository.AchievementRepository;
import com.mipt.dbAPI.jpa.repository.AnsweredCorrectlyQuestionRepository;
import com.mipt.dbAPI.jpa.repository.CoinTransactionRepository;
import com.mipt.dbAPI.jpa.repository.GameHistoryRepository;
import com.mipt.dbAPI.jpa.repository.GameRepository;
import com.mipt.dbAPI.jpa.repository.QuestionRepository;
import com.mipt.dbAPI.jpa.repository.TopicRepository;
import com.mipt.dbAPI.jpa.repository.UserAchievementRepository;
import com.mipt.dbAPI.jpa.repository.UserRepository;
import com.mipt.domainModel.Achievement;
import com.mipt.domainModel.CurrentGameObject;
import com.mipt.domainModel.Question;
import com.mipt.domainModel.Topic;
import com.mipt.gameModes.GameMode;
import com.mipt.gameModes.GameModeCatalog;
import com.mipt.utils.QuestionPayloadFinalNormalizer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class DbService {
  // API of QUIZ AI ARENA database

  private static final String DELETE_ALL_KEYWORD = "DELETE_ALL_RECORDS_IN_DATABASE";
  private static final int DEFAULT_STARTING_COIN_BALANCE = 100;
  private static final int CASUAL_COIN_REWARD = 10;
  private static final int CASUAL_DAILY_COIN_LIMIT = 300;
  private static final int PREMIUM_CASUAL_DAILY_COIN_LIMIT = CASUAL_DAILY_COIN_LIMIT * 2;
  private static final int TRUE_FALSE_COIN_REWARD = 5;
  private static final int TRUE_FALSE_DAILY_COIN_LIMIT = 150;
  private static final int PREMIUM_TRUE_FALSE_DAILY_COIN_LIMIT = TRUE_FALSE_DAILY_COIN_LIMIT * 2;
  @Value("${app.auth.email-enabled:true}")
  private boolean emailAuthEnabled = true;

  private final UserRepository userRepository;
  private final GameRepository gameRepository;
  private final QuestionRepository questionRepository;
  private final GameHistoryRepository gameHistoryRepository;
  private final AchievementRepository achievementRepository;
  private final UserAchievementRepository userAchievementRepository;
  private final AnsweredCorrectlyQuestionRepository answeredCorrectlyQuestionRepository;
  private final TopicRepository topicRepository;
  private final CoinTransactionRepository coinTransactionRepository;
  private final ConfigurableApplicationContext localContext;

  @Autowired
  public DbService(
      UserRepository userRepository,
      GameRepository gameRepository,
      QuestionRepository questionRepository,
      GameHistoryRepository gameHistoryRepository,
      AchievementRepository achievementRepository,
      UserAchievementRepository userAchievementRepository,
      AnsweredCorrectlyQuestionRepository answeredCorrectlyQuestionRepository,
      TopicRepository topicRepository,
      CoinTransactionRepository coinTransactionRepository
  ) {
    this(
        userRepository,
        gameRepository,
        questionRepository,
        gameHistoryRepository,
        achievementRepository,
        userAchievementRepository,
        answeredCorrectlyQuestionRepository,
        topicRepository,
        coinTransactionRepository,
        null
    );
  }

  public DbService(String url, String user, String password) {
    this(bootstrapRepositories(url, user, password));
  }

  private DbService(RepositoryBundle bundle) {
    this(
        bundle.userRepository,
        bundle.gameRepository,
        bundle.questionRepository,
        bundle.gameHistoryRepository,
        bundle.achievementRepository,
        bundle.userAchievementRepository,
        bundle.answeredCorrectlyQuestionRepository,
        bundle.topicRepository,
        bundle.coinTransactionRepository,
        bundle.context
    );
  }

  private DbService(
      UserRepository userRepository,
      GameRepository gameRepository,
      QuestionRepository questionRepository,
      GameHistoryRepository gameHistoryRepository,
      AchievementRepository achievementRepository,
      UserAchievementRepository userAchievementRepository,
      AnsweredCorrectlyQuestionRepository answeredCorrectlyQuestionRepository,
      TopicRepository topicRepository,
      CoinTransactionRepository coinTransactionRepository,
      ConfigurableApplicationContext localContext
  ) {
    this.userRepository = userRepository;
    this.gameRepository = gameRepository;
    this.questionRepository = questionRepository;
    this.gameHistoryRepository = gameHistoryRepository;
    this.achievementRepository = achievementRepository;
    this.userAchievementRepository = userAchievementRepository;
    this.answeredCorrectlyQuestionRepository = answeredCorrectlyQuestionRepository;
    this.topicRepository = topicRepository;
    this.coinTransactionRepository = coinTransactionRepository;
    this.localContext = localContext;
  }

  private static RepositoryBundle bootstrapRepositories(String url, String user, String password) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean(DatabaseProperties.class, () -> {
      DatabaseProperties databaseProperties = new DatabaseProperties();
      databaseProperties.setUrl(url);
      databaseProperties.setUser(user);
      databaseProperties.setPassword(password);
      return databaseProperties;
    });
    context.register(DbServiceConfig.class);
    context.refresh();

    return new RepositoryBundle(
        context,
        context.getBean(UserRepository.class),
        context.getBean(GameRepository.class),
        context.getBean(QuestionRepository.class),
        context.getBean(GameHistoryRepository.class),
        context.getBean(AchievementRepository.class),
        context.getBean(UserAchievementRepository.class),
        context.getBean(AnsweredCorrectlyQuestionRepository.class),
        context.getBean(TopicRepository.class),
        context.getBean(CoinTransactionRepository.class)
    );
  }

  private Optional<UserEntity> findBySessionExact(String session) {
    if (session == null) {
      return Optional.empty();
    }
    return userRepository.findBySession(session);
  }

  private UserEntity getUserBySessionOrThrow(String session) throws DatabaseAccessException {
    return findBySessionExact(session).orElseThrow(DatabaseAccessException::new);
  }

  private UserEntity getUserByIdOrThrow(int userId) throws DatabaseAccessException {
    return userRepository.findById(userId).orElseThrow(DatabaseAccessException::new);
  }

  private GameEntity getGameOrThrow(int gameId) throws DatabaseAccessException {
    return gameRepository.findById(gameId).orElseThrow(DatabaseAccessException::new);
  }

  private TopicEntity getTopicOrThrow(int topicId) throws DatabaseAccessException {
    return topicRepository.findById(topicId).orElseThrow(DatabaseAccessException::new);
  }


  private static Achievement mapAchievement(AchievementEntity achievementEntity) {
    Achievement achievement = new Achievement();
    achievement.setAchievementId(achievementEntity.getId());
    achievement.setName(achievementEntity.getName());
    achievement.setProfilePicNeeded(boolOrFalse(achievementEntity.getProfilePicNeeded()));
    achievement.setDescriptionNeeded(boolOrFalse(achievementEntity.getDescriptionNeeded()));
    achievement.setGamesNumberNeeded(intOrZero(achievementEntity.getGamesNumberNeeded()));
    achievement.setGlobalPointsNeeded(intOrZero(achievementEntity.getGlobalPointsNeeded()));
    achievement.setGlobalRatingPlaceNeeded(intOrZero(achievementEntity.getGlobalRatingPlaceNeeded()));
    achievement.setCurrentGamePointsNeeded(intOrZero(achievementEntity.getCurrentGamePointsNeeded()));
    achievement.setCurrentGameRatingNeeded(intOrZero(achievementEntity.getCurrentGameRatingNeeded()));
    achievement.setCurrentGameLevelDifficultyNeeded(intOrZero(achievementEntity.getCurrentGameLevelDifficultyNeeded()));
    return achievement;
  }

  private static Topic mapTopic(TopicEntity topicEntity) {
    Topic topic = new Topic();
    topic.setTopicId(topicEntity.getId());
    topic.setName(topicEntity.getName());
    return topic;
  }

  private static Integer[] toIntegerArray(List<Integer> source) {
    return source.toArray(new Integer[0]);
  }

  private static int intOrZero(Integer value) {
    return value == null ? 0 : value;
  }

  private static int getPremiumPlanPrice(int days) throws DatabaseAccessException {
    return switch (days) {
      case 1 -> 1500;
      case 7 -> 5000;
      case 30 -> 15000;
      default -> throw new DatabaseAccessException("Unknown premium plan");
    };
  }

  private static boolean isPremiumActive(UserEntity userEntity) {
    Timestamp premiumUntil = userEntity.getPremiumUntil();
    return premiumUntil != null && premiumUntil.toInstant().isAfter(Instant.now());
  }

  private static int getCasualDailyCoinLimit(UserEntity userEntity) {
    return isPremiumActive(userEntity) ? PREMIUM_CASUAL_DAILY_COIN_LIMIT : CASUAL_DAILY_COIN_LIMIT;
  }

  private static int getTrueFalseDailyCoinLimit(UserEntity userEntity) {
    return isPremiumActive(userEntity) ? PREMIUM_TRUE_FALSE_DAILY_COIN_LIMIT : TRUE_FALSE_DAILY_COIN_LIMIT;
  }

  private static boolean boolOrFalse(Boolean value) {
    return Boolean.TRUE.equals(value);
  }

  private static boolean requiresPremiumAvatar(String customAvatarPath) {
    if (customAvatarPath == null) {
      return false;
    }

    String normalizedPath = customAvatarPath.toLowerCase(Locale.ROOT);
    return normalizedPath.endsWith(".gif") || normalizedPath.endsWith(".webp");
  }

  private static String resolveAvatarUrl(UserEntity userEntity) {
    String customAvatarPath = userEntity.getCustomAvatarPath();
    if (customAvatarPath != null && !customAvatarPath.isBlank()) {
      return customAvatarPath;
    }

    Integer picId = userEntity.getPicId();
    if (picId != null && picId > 0) {
      return "/asserts/asserts/avatar" + picId + ".png";
    }

    return null;
  }

  private void expirePremiumBenefitsIfNeeded(UserEntity userEntity) {
    Timestamp premiumUntil = userEntity.getPremiumUntil();
    if (premiumUntil == null || premiumUntil.toInstant().isAfter(Instant.now())) {
      return;
    }

    String customAvatarPath = userEntity.getCustomAvatarPath();
    if (requiresPremiumAvatar(customAvatarPath)) {
      userEntity.setCustomAvatarPath(null);
      userEntity.setPicId(0);
    }
    userEntity.setPremiumUntil(null);
    userRepository.save(userEntity);
  }

  // Users TABLE REFERRED METHODS
  //

  public Integer getUserId(String session) throws SQLException {
    return findBySessionExact(session).map(UserEntity::getId).orElse(null);
  }

  public boolean checkUserExists(String username) throws SQLException {
    if (username == null) {
      return false;
    }
    return userRepository.existsByUsername(username);
  }

  public boolean checkEmailExists(String email) throws SQLException {
    if (email == null || email.isBlank()) {
      return false;
    }
    return userRepository.existsByEmail(normalizeEmail(email));
  }

  public void register(String username, String password) throws SQLException, DatabaseAccessException {
    register(username, password, null);
  }

  public void register(String username, String password, String email) throws SQLException, DatabaseAccessException {
    if (checkUserExists(username)) {
      throw new DatabaseAccessException("User already exists");
    }
    if (emailAuthEnabled && email != null && !email.isBlank() && checkEmailExists(email)) {
      throw new DatabaseAccessException("Email already exists");
    }

    String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
    registerHashedPassword(username, hashedPassword, email);
  }

  public void registerHashedPassword(String username, String passwordHash, String email)
      throws SQLException, DatabaseAccessException {
    if (checkUserExists(username)) {
      throw new DatabaseAccessException("User already exists");
    }
    String emailToStore = emailAuthEnabled ? email : null;
    if (emailToStore != null && !emailToStore.isBlank() && checkEmailExists(emailToStore)) {
      throw new DatabaseAccessException("Email already exists");
    }

    UserEntity userEntity = new UserEntity();
    userEntity.setUsername(username);
    userEntity.setPassword(passwordHash);
    userEntity.setEmail(normalizeEmail(emailToStore));
    userEntity.setPicId(0);
    userEntity.setDescription("");
    userEntity.setGamesPlayedNumber(0);
    userEntity.setGlobalPoints(0);
    userEntity.setGlobalPossiblePoints(0);
    userEntity.setCurrentGamePoints(0);
    userEntity.setCoinBalance(DEFAULT_STARTING_COIN_BALANCE);
    userRepository.save(userEntity);
    recordCoinTransaction(
        userEntity,
        null,
        DEFAULT_STARTING_COIN_BALANCE,
        DEFAULT_STARTING_COIN_BALANCE,
        CoinTransactionType.INITIAL_GRANT,
        "Initial account grant"
    );
  }

  public void authenticate(String username, String password, String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = (emailAuthEnabled
        ? userRepository.findByUsernameOrEmail(username)
        : userRepository.findByUsername(username)
    ).orElseThrow(DatabaseAccessException::new);

    BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), userEntity.getPassword());
    if (!result.verified) {
      throw new DatabaseAccessException("Wrong password");
    }

    if (session != null && findBySessionExact(session).isPresent()) {
      throw new DatabaseAccessException("Session key is not unique and already exists in database");
    }

    userEntity.setSession(session);
    userRepository.save(userEntity);
  }

  public void changePassword(String session, String newPassword) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    String hashedPassword = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
    userEntity.setPassword(hashedPassword);
    userRepository.save(userEntity);
  }

  public void changePasswordHashByEmail(String email, String passwordHash) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = userRepository.findByEmail(normalizeEmail(email)).orElseThrow(DatabaseAccessException::new);
    userEntity.setPassword(passwordHash);
    userRepository.save(userEntity);
  }

  public void changeProfilePic(String session, int picId) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    userEntity.setPicId(picId);
    userEntity.setCustomAvatarPath(null);
    userRepository.save(userEntity);
  }

  public int getProfilePic(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    return intOrZero(userEntity.getPicId());
  }

  public void changeCustomAvatarPath(String session, String customAvatarPath)
      throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    userEntity.setCustomAvatarPath(customAvatarPath);
    userEntity.setPicId(0);
    userRepository.save(userEntity);
  }

  public String getCustomAvatarPath(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    return userEntity.getCustomAvatarPath();
  }

  public String getUsername(int userId) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = userRepository.findById(userId).orElseThrow(DatabaseAccessException::new);
    return userEntity.getUsername();
  }

  public String getUsername(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    return userEntity.getUsername();
  }

  public String getEmail(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    return userEntity.getEmail();
  }

  public String getEmailByUsername(String username) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = userRepository.findByUsername(username).orElseThrow(DatabaseAccessException::new);
    return userEntity.getEmail();
  }

  public boolean userExistsByEmail(String email) throws SQLException {
    return checkEmailExists(email);
  }

  public void changeDescription(String session, String description) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    userEntity.setDescription(description);
    userRepository.save(userEntity);
  }

  public String getDescription(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    return userEntity.getDescription();
  }

  public void setLastActivity(String session, Instant time) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    userEntity.setLastActivity(Timestamp.from(time));
    userRepository.save(userEntity);
  }

  public Timestamp getLastActivity(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    return userEntity.getLastActivity();
  }

  public void setCurrentGame(String session, Integer gameId) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);

    if (gameId == null) {
      userEntity.setCurrentGame(null);
      userRepository.save(userEntity);
      userEntity.setCurrentGamePoints(0);
      return;
    }

    GameEntity gameEntity = gameRepository.findByIdAndStatus(gameId, 0)
        .orElseThrow(() -> new DatabaseAccessException("Game not found"));

    Integer participantsNumber = gameEntity.getParticipantsNumber();
    if (getCurrentParticipantsNumber(gameId).equals(participantsNumber)) {
      throw new DatabaseAccessException("Request is out of bounds");
    }

    userEntity.setCurrentGame(gameEntity);
    userRepository.save(userEntity);
  }

  public Integer getCurrentGame(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    GameEntity currentGame = userEntity.getCurrentGame();
    return currentGame == null ? null : currentGame.getId();
  }

  public Integer getCoinBalance(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    return intOrZero(userEntity.getCoinBalance());
  }

  public Integer getCoinBalanceByUserId(int userId) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserByIdOrThrow(userId);
    return intOrZero(userEntity.getCoinBalance());
  }

  public Timestamp getPremiumUntil(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    expirePremiumBenefitsIfNeeded(userEntity);
    return userEntity.getPremiumUntil();
  }

  public boolean isPremiumActive(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    expirePremiumBenefitsIfNeeded(userEntity);
    return isPremiumActive(userEntity);
  }

  public boolean isPremiumActiveForUser(UserEntity userEntity) {
    expirePremiumBenefitsIfNeeded(userEntity);
    return isPremiumActive(userEntity);
  }

  public int getCasualDailyCoinLimit(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    expirePremiumBenefitsIfNeeded(userEntity);
    return getCasualDailyCoinLimit(userEntity);
  }

  public void changeCoinBalance(
      String session,
      int amountDelta,
      CoinTransactionType transactionType,
      String reason,
      Integer gameId
  ) throws SQLException, DatabaseAccessException {
    if (amountDelta == 0) {
      return;
    }

    UserEntity userEntity = getUserBySessionOrThrow(session);
    int currentBalance = intOrZero(userEntity.getCoinBalance());
    int updatedBalance = currentBalance + amountDelta;
    if (updatedBalance < 0) {
      throw new DatabaseAccessException("Insufficient coin balance");
    }

    GameEntity gameEntity = gameId == null ? null : getGameOrThrow(gameId);
    userEntity.setCoinBalance(updatedBalance);
    userRepository.save(userEntity);
    recordCoinTransaction(userEntity, gameEntity, amountDelta, updatedBalance, transactionType, reason);
  }

  public void changeCoinBalanceByUserId(
      int userId,
      int amountDelta,
      CoinTransactionType transactionType,
      String reason,
      Integer gameId
  ) throws SQLException, DatabaseAccessException {
    if (amountDelta == 0) {
      return;
    }

    UserEntity userEntity = getUserByIdOrThrow(userId);
    int currentBalance = intOrZero(userEntity.getCoinBalance());
    int updatedBalance = currentBalance + amountDelta;
    if (updatedBalance < 0) {
      throw new DatabaseAccessException("Insufficient coin balance");
    }

    GameEntity gameEntity = gameId == null ? null : getGameOrThrow(gameId);
    userEntity.setCoinBalance(updatedBalance);
    userRepository.save(userEntity);
    recordCoinTransaction(userEntity, gameEntity, amountDelta, updatedBalance, transactionType, reason);
  }

  public int addCasualQuizReward(String session, int gameId) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    expirePremiumBenefitsIfNeeded(userEntity);
    return addQuizRewardWithinDailyLimit(
        session,
        gameId,
        userEntity,
        CASUAL_COIN_REWARD,
        getCasualDailyCoinLimit(userEntity),
        CoinTransactionType.QUIZ_REWARD,
        "Casual quiz correct answer reward"
    );
  }

  public int addTrueFalseQuizReward(String session, int gameId) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    expirePremiumBenefitsIfNeeded(userEntity);
    return addQuizRewardWithinDailyLimit(
        session,
        gameId,
        userEntity,
        TRUE_FALSE_COIN_REWARD,
        getTrueFalseDailyCoinLimit(userEntity),
        CoinTransactionType.TRUE_FALSE_REWARD,
        "True/False quiz correct answer reward"
    );
  }

  private int addQuizRewardWithinDailyLimit(
      String session,
      int gameId,
      UserEntity userEntity,
      int rewardPerAnswer,
      int dailyLimit,
      CoinTransactionType transactionType,
      String reason
  ) throws SQLException, DatabaseAccessException {
    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    Timestamp dayStart = Timestamp.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
    Timestamp nextDayStart = Timestamp.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    int earnedToday = intOrZero(coinTransactionRepository.sumAmountDeltaByUserAndTypeBetween(
        userEntity.getId(),
        transactionType,
        dayStart,
        nextDayStart
    ));
    int reward = Math.min(rewardPerAnswer, Math.max(0, dailyLimit - earnedToday));
    if (reward <= 0) {
      return 0;
    }

    changeCoinBalance(session, reward, transactionType, reason, gameId);
    return reward;
  }

  public Timestamp purchasePremium(String session, int days) throws SQLException, DatabaseAccessException {
    int price = getPremiumPlanPrice(days);
    UserEntity userEntity = getUserBySessionOrThrow(session);
    expirePremiumBenefitsIfNeeded(userEntity);

    int currentBalance = intOrZero(userEntity.getCoinBalance());
    int updatedBalance = currentBalance - price;
    if (updatedBalance < 0) {
      throw new DatabaseAccessException("Insufficient coin balance");
    }

    Instant now = Instant.now();
    Instant startsAt = isPremiumActive(userEntity)
        ? userEntity.getPremiumUntil().toInstant()
        : now;
    Timestamp premiumUntil = Timestamp.from(startsAt.plus(Duration.ofDays(days)));

    userEntity.setCoinBalance(updatedBalance);
    userEntity.setPremiumUntil(premiumUntil);
    userRepository.save(userEntity);
    recordCoinTransaction(
        userEntity,
        null,
        -price,
        updatedBalance,
        CoinTransactionType.PREMIUM_PURCHASE,
        "QUIZ PREMIUM " + days + " day plan"
    );
    return premiumUntil;
  }

  public void changePremiumUntil(String session, Timestamp premiumUntil) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    userEntity.setPremiumUntil(premiumUntil);
    userRepository.save(userEntity);
  }

  public void expirePremiumBenefits(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    expirePremiumBenefitsIfNeeded(userEntity);
  }

  public void expireExpiredPremiumBenefits() {
    for (UserEntity userEntity : userRepository.findByPremiumUntilIsNotNull()) {
      expirePremiumBenefitsIfNeeded(userEntity);
    }
  }

  public void addGamePlayed(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    GameEntity gameEntity = userEntity.getCurrentGame();
    if (gameEntity == null) {
      throw new DatabaseAccessException("Game not found");
    }

    userEntity.setGamesPlayedNumber(userEntity.getGamesPlayedNumber() + 1);
    userRepository.save(userEntity);

    GameHistoryEntity gameHistoryEntity = new GameHistoryEntity();
    gameHistoryEntity.setGame(gameEntity);
    gameHistoryEntity.setUser(userEntity);
    gameHistoryRepository.save(gameHistoryEntity);
  }

  public Integer[] getGamesPlayed(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    List<GameHistoryEntity> historyEntities = gameHistoryRepository.findByUser_IdOrderByIdAsc(userEntity.getId());
    List<Integer> gameIds = new ArrayList<>();
    for (GameHistoryEntity historyEntity : historyEntities) {
      gameIds.add(historyEntity.getGame().getId());
    }
    return toIntegerArray(gameIds);
  }

  public boolean addCorrectAnswer(String session, int questionId) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    QuestionEntity questionEntity = questionRepository.findById(questionId).orElseThrow(DatabaseAccessException::new);
    if (answeredCorrectlyQuestionRepository.existsByUser_IdAndQuestion_Id(userEntity.getId(), questionId)) {
      return false;
    }

    AnsweredCorrectlyQuestionEntity answeredCorrectlyQuestionEntity = new AnsweredCorrectlyQuestionEntity();
    answeredCorrectlyQuestionEntity.setUser(userEntity);
    answeredCorrectlyQuestionEntity.setQuestion(questionEntity);
    answeredCorrectlyQuestionRepository.save(answeredCorrectlyQuestionEntity);
    return true;
  }

  public Question[] getCorrectAnswers(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    List<AnsweredCorrectlyQuestionEntity> answeredCorrectlyQuestionEntities =
        answeredCorrectlyQuestionRepository.findByUser_IdOrderByIdAsc(userEntity.getId());
    List<Question> questions = new ArrayList<>();

    for (AnsweredCorrectlyQuestionEntity answeredCorrectlyQuestionEntity : answeredCorrectlyQuestionEntities) {
      QuestionEntity questionEntity = answeredCorrectlyQuestionEntity.getQuestion();
      Question question = new Question();
      question.setQuestionId(questionEntity.getId());
      question.setGameId(questionEntity.getGame() == null ? null : questionEntity.getGame().getId());
      question.setQuestionNumber(questionEntity.getQuestionNumber());
      question.setQuestionText(questionEntity.getQuestionText());
      question.setAnswer1(questionEntity.getAnswer1());
      question.setAnswer2(questionEntity.getAnswer2());
      question.setAnswer3(questionEntity.getAnswer3());
      question.setAnswer4(questionEntity.getAnswer4());
      question.setRightAnswerNumber(questionEntity.getRightAnswerNumber());
      questions.add(question);
    }

    return questions.toArray(new Question[0]);
  }


  public Question[] getQuizHistoryQuestions(String session, int gameId) throws SQLException, DatabaseAccessException {
    getUserBySessionOrThrow(session);
    getGameOrThrow(gameId);

    Integer[] gamesPlayed = getGamesPlayed(session);
    boolean gameFoundInHistory = false;
    for (Integer playedGameId : gamesPlayed) {
      if (playedGameId != null && playedGameId == gameId) {
        gameFoundInHistory = true;
        break;
      }
    }

    if (!gameFoundInHistory) {
      throw new DatabaseAccessException("Game is not in user history");
    }

    List<QuestionEntity> questionEntities = questionRepository.findByGame_IdOrderByQuestionNumberAscIdAsc(gameId);
    List<Question> questions = new ArrayList<>();

    for (QuestionEntity questionEntity : questionEntities) {
      Question question = new Question();
      question.setQuestionId(questionEntity.getId());
      question.setGameId(gameId);
      question.setQuestionNumber(questionEntity.getQuestionNumber());
      question.setQuestionText(questionEntity.getQuestionText());
      question.setAnswer1(questionEntity.getAnswer1());
      question.setAnswer2(questionEntity.getAnswer2());
      question.setAnswer3(questionEntity.getAnswer3());
      question.setAnswer4(questionEntity.getAnswer4());
      question.setRightAnswerNumber(questionEntity.getRightAnswerNumber());
      questions.add(question);
    }

    return questions.toArray(new Question[0]);
  }


  public CurrentGameObject getCurrentGameObjectBySession(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    GameEntity gameEntity = userEntity.getCurrentGame();
    CurrentGameObject currentGameObject = new CurrentGameObject();

    if (gameEntity == null) {
      return currentGameObject;
    }

    GameEntity game = getGameOrThrow(gameEntity.getId());
    currentGameObject.setGameId(game.getId());
    currentGameObject.setGameMode(GameModeCatalog.normalize(game.getGameMode()));
    if (game.getGameStartTime() != null) {
      currentGameObject.setGameStartTime(game.getGameStartTime().toInstant());
    }
    return currentGameObject;
  }


  public void addGlobalPoints(String session, int points) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    userEntity.setGlobalPoints(userEntity.getGlobalPoints() + points);
    userRepository.save(userEntity);
  }

  public Integer getGlobalPoints(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    return userEntity.getGlobalPoints();
  }

  public Integer getGamesPlayedNumber(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    return intOrZero(userEntity.getGamesPlayedNumber());
  }

  public void addGlobalPossiblePoints(String session, int possiblePoints) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    userEntity.setGlobalPossiblePoints(userEntity.getGlobalPossiblePoints() + possiblePoints);
    userRepository.save(userEntity);
  }

  public Integer getGlobalPossiblePoints(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    return userEntity.getGlobalPossiblePoints();
  }

  public void addCurrentGamePoints(String session, int points) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    userEntity.setCurrentGamePoints(userEntity.getCurrentGamePoints() + points);
    userRepository.save(userEntity);
  }

  public Integer getCurrentGamePoints(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    return userEntity.getCurrentGamePoints();
  }

  public void leaveGame(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    if (userEntity.getCurrentGame() == null) {
      throw new DatabaseAccessException();
    }

    userEntity.setCurrentGame(null);
    userRepository.save(userEntity);
  }

  public void logOut(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    userEntity.setSession(null);
    userRepository.save(userEntity);
  }

  private String normalizeEmail(String email) {
    if (email == null || email.isBlank()) {
      return null;
    }
    return email.trim().toLowerCase(Locale.ROOT);
  }

  //
  // Games TABLE REFERRED METHODS
  //

  public Integer createGame(String sessionOfAuthor, int levelDifficulty, int numberOfQuestions, int participantsNumber, int topicId)
      throws SQLException, DatabaseAccessException {
    return createGame(
        sessionOfAuthor,
        levelDifficulty,
        numberOfQuestions,
        participantsNumber,
        topicId,
        true,
        GameModeCatalog.defaultMode()
    );
  }

  public Integer createGame(String sessionOfAuthor, int levelDifficulty, int numberOfQuestions, int participantsNumber, int topicId, boolean isPrivate)
      throws SQLException, DatabaseAccessException {
    return createGame(
        sessionOfAuthor,
        levelDifficulty,
        numberOfQuestions,
        participantsNumber,
        topicId,
        isPrivate,
        GameModeCatalog.defaultMode()
    );
  }

  public Integer createGame(
      String sessionOfAuthor,
      int levelDifficulty,
      int numberOfQuestions,
      int participantsNumber,
      int topicId,
      boolean isPrivate,
      GameMode gameMode
  ) throws SQLException, DatabaseAccessException {
    UserEntity author = getUserBySessionOrThrow(sessionOfAuthor);
    GameMode normalizedMode = GameModeCatalog.normalize(gameMode);
    GameModeCatalog.validateCreateRequest(normalizedMode, levelDifficulty, numberOfQuestions, participantsNumber);

    TopicEntity topicEntity = getTopicOrThrow(topicId);

    GameEntity gameEntity = new GameEntity();
    gameEntity.setStatus(0);
    gameEntity.setAuthor(author);
    gameEntity.setCreatedAt(Timestamp.from(Instant.now()));
    gameEntity.setPrivate(isPrivate);
    gameEntity.setLevelDifficulty(levelDifficulty);
    gameEntity.setNumberOfQuestions(numberOfQuestions);
    gameEntity.setParticipantsNumber(participantsNumber);
    gameEntity.setTopic(topicEntity);
    gameEntity.setQuestionsValidated(false);
    gameEntity.setGameMode(normalizedMode);

    return gameRepository.save(gameEntity).getId();
  }

  public Boolean checkGameExists(int gameId) throws SQLException {
    return gameRepository.existsById(gameId);
  }

  public Integer[] getPreset(int gameId) throws SQLException, DatabaseAccessException {
    GameEntity gameEntity = getGameOrThrow(gameId);

    Integer[] preset = new Integer[5];
    preset[0] = gameEntity.getAuthor() == null ? 0 : gameEntity.getAuthor().getId();
    preset[1] = intOrZero(gameEntity.getLevelDifficulty());
    preset[2] = intOrZero(gameEntity.getNumberOfQuestions());
    preset[3] = gameEntity.getParticipantsNumber();
    preset[4] = gameEntity.getTopic() == null ? 0 : gameEntity.getTopic().getId();
    return preset;
  }

  public GameMode getGameMode(int gameId) throws SQLException, DatabaseAccessException {
    GameEntity gameEntity = getGameOrThrow(gameId);
    return GameModeCatalog.normalize(gameEntity.getGameMode());
  }

  public void setStatus(int gameId, int status) throws SQLException, DatabaseAccessException {
    GameEntity gameEntity = getGameOrThrow(gameId);
    gameEntity.setStatus(status);
    gameRepository.save(gameEntity);
  }

  public Integer getStatus(int gameId) throws SQLException, DatabaseAccessException {
    GameEntity gameEntity = getGameOrThrow(gameId);
    return gameEntity.getStatus();
  }

  public void setGameStartTime(int gameId, Instant gameStartTime) throws SQLException, DatabaseAccessException {
    GameEntity gameEntity = getGameOrThrow(gameId);
    gameEntity.setGameStartTime(Timestamp.from(gameStartTime));
    gameRepository.save(gameEntity);
  }

  public Timestamp getGameStartTime(int gameId) throws SQLException, DatabaseAccessException {
    GameEntity gameEntity = getGameOrThrow(gameId);
    return gameEntity.getGameStartTime();
  }

  public void setPrivate(int gameId, boolean isPrivate) throws SQLException, DatabaseAccessException {
    GameEntity gameEntity = getGameOrThrow(gameId);
    gameEntity.setPrivate(isPrivate);
    gameRepository.save(gameEntity);
  }

  public Boolean getPrivate(int gameId) throws SQLException, DatabaseAccessException {
    GameEntity gameEntity = getGameOrThrow(gameId);
    return Boolean.TRUE.equals(gameEntity.getIsPrivate());
  }

  public void setGameEndTime(int gameId, Instant gameEndTime) throws SQLException, DatabaseAccessException {
    GameEntity gameEntity = getGameOrThrow(gameId);
    gameEntity.setGameEndTime(Timestamp.from(gameEndTime));
    gameRepository.save(gameEntity);
  }

  public Timestamp getGameEndTime(int gameId) throws SQLException, DatabaseAccessException {
    GameEntity gameEntity = getGameOrThrow(gameId);
    return gameEntity.getGameEndTime();
  }

  public void stopGame(int gameId) throws SQLException, DatabaseAccessException {
    GameEntity gameEntity = getGameOrThrow(gameId);

    gameEntity.setStatus(3);
    gameEntity.setGameEndTime(Timestamp.from(Instant.now()));
    gameRepository.save(gameEntity);

    List<UserEntity> participants = userRepository.findByCurrentGame_Id(gameId);
    for (UserEntity userEntity : participants) {
      userEntity.setGamesPlayedNumber(intOrZero(userEntity.getGamesPlayedNumber()) + 1);
      userEntity.setCurrentGame(null);
      userEntity.setCurrentGamePoints(0);

      GameHistoryEntity gameHistoryEntity = new GameHistoryEntity();
      gameHistoryEntity.setGame(gameEntity);
      gameHistoryEntity.setUser(userEntity);
      gameHistoryRepository.save(gameHistoryEntity);
    }
    userRepository.saveAll(participants);
  }

  public void deleteGame(int gameId) throws SQLException, DatabaseAccessException {
    getGameOrThrow(gameId);

    List<UserEntity> participants = userRepository.findByCurrentGame_Id(gameId);
    for (UserEntity userEntity : participants) {
      userEntity.setCurrentGame(null);
      userEntity.setCurrentGamePoints(0);
    }
    userRepository.saveAll(participants);

    gameRepository.deleteById(gameId);
  }

  public Boolean isGameReady(int gameId) throws DatabaseAccessException, SQLException {
    GameEntity gameEntity = getGameOrThrow(gameId);
    if (!GameModeCatalog.usesStandardQuizFlow(gameEntity.getGameMode())) {
      return true;
    }
    long currentNumber = questionRepository.countByGame_Id(gameId);
    int neededNumber = intOrZero(gameEntity.getNumberOfQuestions());
    return neededNumber == currentNumber && Boolean.TRUE.equals(gameEntity.getQuestionsValidated());
  }

  public long countQuestionsInGame(int gameId) throws DatabaseAccessException, SQLException {
    getGameOrThrow(gameId);
    return questionRepository.countByGame_Id(gameId);
  }

  public void setQuestionsValidated(int gameId, boolean validated) throws SQLException, DatabaseAccessException {
    GameEntity gameEntity = getGameOrThrow(gameId);
    gameEntity.setQuestionsValidated(validated);
    gameRepository.save(gameEntity);
  }

  public Question getQuestion(int gameId, int questionNumber) throws SQLException, DatabaseAccessException {
    if (!checkGameExists(gameId)) {
      throw new DatabaseAccessException();
    }

    if (!GameModeCatalog.usesStandardQuizFlow(getGameMode(gameId))) {
      throw new DatabaseAccessException("Current mode does not use the standard question feed");
    }

    QuestionEntity questionEntity = questionRepository.findByGame_IdAndQuestionNumber(gameId, questionNumber)
        .orElseThrow(() -> new DatabaseAccessException("Question not exists"));

    Question question = new Question();
    question.setQuestionId(questionEntity.getId());
    question.setGameId(gameId);
    question.setQuestionNumber(questionEntity.getQuestionNumber());
    question.setQuestionText(questionEntity.getQuestionText());
    question.setAnswer1(questionEntity.getAnswer1());
    question.setAnswer2(questionEntity.getAnswer2());
    question.setAnswer3(questionEntity.getAnswer3());
    question.setAnswer4(questionEntity.getAnswer4());
    return question;
  }

  public Question getQuestionForAnyMode(int gameId, int questionNumber) throws SQLException, DatabaseAccessException {
    if (!checkGameExists(gameId)) {
      throw new DatabaseAccessException();
    }

    QuestionEntity questionEntity = questionRepository.findByGame_IdAndQuestionNumber(gameId, questionNumber)
        .orElseThrow(() -> new DatabaseAccessException("Question not exists"));

    Question question = new Question();
    question.setQuestionId(questionEntity.getId());
    question.setGameId(gameId);
    question.setQuestionNumber(questionEntity.getQuestionNumber());
    question.setQuestionText(questionEntity.getQuestionText());
    question.setAnswer1(questionEntity.getAnswer1());
    question.setAnswer2(questionEntity.getAnswer2());
    question.setAnswer3(questionEntity.getAnswer3());
    question.setAnswer4(questionEntity.getAnswer4());
    question.setRightAnswerNumber(questionEntity.getRightAnswerNumber());
    return question;
  }

  public Integer getRightAnswer(int gameId, int questionNumber) throws SQLException, DatabaseAccessException {
    if (!checkGameExists(gameId)) {
      throw new DatabaseAccessException();
    }

    if (!GameModeCatalog.usesStandardQuizFlow(getGameMode(gameId))) {
      throw new DatabaseAccessException("Current mode does not use the standard answer validator");
    }

    Integer gameStatus = getStatus(gameId);
    if (gameStatus != 2) {
      throw new DatabaseAccessException("Game is not active");
    }

    QuestionEntity questionEntity = questionRepository.findByGame_IdAndQuestionNumber(gameId, questionNumber)
        .orElseThrow(DatabaseAccessException::new);
    return questionEntity.getRightAnswerNumber();
  }

  public Integer getStoredRightAnswer(int gameId, int questionNumber) throws SQLException, DatabaseAccessException {
    if (!checkGameExists(gameId)) {
      throw new DatabaseAccessException();
    }

    QuestionEntity questionEntity = questionRepository.findByGame_IdAndQuestionNumber(gameId, questionNumber)
        .orElseThrow(DatabaseAccessException::new);
    return questionEntity.getRightAnswerNumber();
  }

  public List<Integer> loadQuestions(int gameId, JSONArray jsonArr) throws SQLException, DatabaseAccessException {
    System.err.println("[DEBUG] loadQuestions started. gameId=" + gameId + ", arrayLength=" + jsonArr.length());
    GameEntity gameEntity = getGameOrThrow(gameId);
    final int answerAmount = 4;
    List<Integer> questionIds = new ArrayList<>();
    Set<Integer> payloadQuestionNumbers = new HashSet<>();

    for (int i = 0; i < jsonArr.length(); i++) {
      try {
        JSONObject itemObject = jsonArr.optJSONObject(i);
        if (itemObject == null) {
          throw new DatabaseAccessException("Question at index " + i + " is not a JSON object");
        }
        System.err.println("[DEBUG] Processing question " + i + ": " + itemObject.toString());

        if (!itemObject.has("question_number")) {
          throw new DatabaseAccessException("Missing question_number at index " + i);
        }
        int questionNumber = itemObject.getInt("question_number");
        if (!payloadQuestionNumbers.add(questionNumber)) {
          throw new DatabaseAccessException("Duplicate question_number in payload: " + questionNumber);
        }

        QuestionEntity questionEntity = questionRepository
            .findByGame_IdAndQuestionNumber(gameId, questionNumber)
            .orElseGet(() -> {
              System.err.println("[DEBUG] Creating new question entity for number: " + questionNumber);
              QuestionEntity created = new QuestionEntity();
              created.setGame(gameEntity);
              created.setQuestionNumber(questionNumber);
              return created;
            });

        System.err.println("[DEBUG] Applying payload to question entity...");
        applyQuestionPayload(questionEntity, itemObject, answerAmount);
        System.err.println("[DEBUG] Saving question to repository...");
        questionRepository.save(questionEntity);
        System.err.println("[DEBUG] Question saved with id: " + questionEntity.getId());
        questionIds.add(questionEntity.getId());
      } catch (DatabaseAccessException e) {
        throw e;
      } catch (Exception e) {
        System.err.println("[ERROR] Exception processing question at index " + i + ": " + e.getMessage());
        throw new DatabaseAccessException("Failed to process question at index " + i + ": " + e.getMessage(), e);
      }
    }

    System.err.println("[DEBUG] Setting questions_validated=true for gameId=" + gameId);
    setQuestionsValidated(gameId, true);
    System.err.println("[DEBUG] loadQuestions completed successfully. Persisted " + questionIds.size() + " questions");
    return questionIds;
  }

  public void replaceQuestionsByIds(int gameId, List<Integer> questionIdsToReplace, JSONArray generatedQuestions)
      throws SQLException, DatabaseAccessException {
    if (questionIdsToReplace == null || questionIdsToReplace.isEmpty() || generatedQuestions == null || generatedQuestions.isEmpty()) {
      return;
    }
    if (questionIdsToReplace.size() != generatedQuestions.length()) {
      throw new DatabaseAccessException("Replacement size mismatch");
    }

    Map<Integer, QuestionEntity> questionsById = new HashMap<>();
    for (QuestionEntity questionEntity : questionRepository.findByGame_IdAndIdIn(gameId, questionIdsToReplace)) {
      questionsById.put(questionEntity.getId(), questionEntity);
    }
    if (questionsById.size() != questionIdsToReplace.size()) {
      throw new DatabaseAccessException("Some question ids are not found in game " + gameId);
    }

    for (int i = 0; i < questionIdsToReplace.size(); i++) {
      try {
        int questionId = questionIdsToReplace.get(i);
        JSONObject itemObject = generatedQuestions.optJSONObject(i);
        if (itemObject == null) {
          throw new DatabaseAccessException("Question at index " + i + " is not a JSON object");
        }
        QuestionEntity target = questionsById.get(questionId);
        if (target == null) {
          throw new DatabaseAccessException("Question id " + questionId + " does not belong to game " + gameId);
        }

        applyQuestionPayload(target, itemObject, 4);
        questionRepository.save(target);
      } catch (DatabaseAccessException e) {
        throw e;
      } catch (Exception e) {
        throw new DatabaseAccessException("Failed to replace question at index " + i + ": " + e.getMessage(), e);
      }
    }
  }

  public List<Integer> getQuestionNumbersByIds(int gameId, List<Integer> questionIds)
      throws SQLException, DatabaseAccessException {
    if (questionIds == null || questionIds.isEmpty()) {
      return List.of();
    }

    Map<Integer, Integer> numbersById = new HashMap<>();
    for (QuestionEntity questionEntity : questionRepository.findByGame_IdAndIdIn(gameId, questionIds)) {
      numbersById.put(questionEntity.getId(), questionEntity.getQuestionNumber());
    }
    if (numbersById.size() != questionIds.size()) {
      throw new DatabaseAccessException("Some question ids are not found in game " + gameId);
    }

    List<Integer> result = new ArrayList<>();
    for (Integer questionId : questionIds) {
      result.add(numbersById.get(questionId));
    }
    return result;
  }

  public List<String> getQuestionTextsByGameIdExceptNumbers(int gameId, List<Integer> excludedQuestionNumbers)
      throws SQLException, DatabaseAccessException {
    getGameOrThrow(gameId);
    Set<Integer> excluded = excludedQuestionNumbers == null
        ? Set.of()
        : new HashSet<>(excludedQuestionNumbers);

    List<String> texts = new ArrayList<>();
    for (QuestionEntity questionEntity : questionRepository.findByGame_IdOrderByQuestionNumberAscIdAsc(gameId)) {
      if (excluded.contains(questionEntity.getQuestionNumber())) {
        continue;
      }
      String text = questionEntity.getQuestionText();
      if (text != null && !text.isBlank()) {
        texts.add(text.trim());
      }
    }
    return texts;
  }

  private static void applyQuestionPayload(QuestionEntity questionEntity, JSONObject itemObject, int answerAmount)
      throws DatabaseAccessException {
    JSONObject normalizedItemObject = QuestionPayloadFinalNormalizer.normalizeQuestion(itemObject);
    questionEntity.setQuestionText(normalizedItemObject.getString("question_text"));
    questionEntity.setRightAnswerNumber(normalizedItemObject.getInt("right_answer_number"));

    JSONArray availableAnswers = normalizedItemObject.getJSONArray("available_answers");
    if (availableAnswers.length() < answerAmount) {
      throw new DatabaseAccessException("Not enough answers for question payload");
    }
    for (int j = 0; j < answerAmount; j++) {
      JSONObject answerObject = availableAnswers.getJSONObject(j);
      int answerIndex = answerObject.getInt("index");
      String answer = answerObject.getString("answer");

      switch (answerIndex) {
        case 1 -> questionEntity.setAnswer1(answer);
        case 2 -> questionEntity.setAnswer2(answer);
        case 3 -> questionEntity.setAnswer3(answer);
        case 4 -> questionEntity.setAnswer4(answer);
        default -> throw new DatabaseAccessException("Bad answer index");
      }
    }
  }

  public String[] getParticipantUsernames(int gameId) throws SQLException, DatabaseAccessException {
    if (!checkGameExists(gameId)) {
      throw new DatabaseAccessException();
    }

    List<UserEntity> participants = userRepository.findByCurrentGame_IdOrderByIdAsc(gameId);
    List<String> usernames = new ArrayList<>();
    for (UserEntity participant : participants) {
      usernames.add(participant.getUsername());
    }
    return usernames.toArray(new String[0]);
  }

  public List<UserEntity> getParticipants(int gameId) throws SQLException, DatabaseAccessException {
    if (!checkGameExists(gameId)) {
      throw new DatabaseAccessException();
    }
    return userRepository.findByCurrentGame_IdOrderByIdAsc(gameId);
  }

  public JSONArray getGameLeaderboards(int gameId) throws SQLException, DatabaseAccessException {
    if (!checkGameExists(gameId)) {
      throw new DatabaseAccessException();
    }

    List<UserEntity> users = userRepository.findByCurrentGame_IdOrderByCurrentGamePointsDesc(gameId);
    JSONArray result = new JSONArray();
    for (UserEntity userEntity : users) {
      result.put(new JSONArray().put(userEntity.getUsername()).put(userEntity.getCurrentGamePoints()));
    }
    return result;
  }

  public JSONArray getGlobalLeaderboards() throws SQLException {
    List<UserEntity> users = userRepository.findTop100ByOrderByGlobalPointsDescGlobalPossiblePointsAsc();
    JSONArray result = new JSONArray();

    for (UserEntity userEntity : users) {
      expirePremiumBenefitsIfNeeded(userEntity);
      JSONArray row = new JSONArray();
      row.put(userEntity.getId());
      row.put(userEntity.getUsername());
      row.put(userEntity.getGlobalPoints());
      row.put(userEntity.getGlobalPossiblePoints());
      row.put(isPremiumActive(userEntity));
      row.put(intOrZero(userEntity.getPicId()));
      row.put(resolveAvatarUrl(userEntity));
      result.put(row);
    }

    return result;
  }

  public JSONArray getCoinLeaderboards() throws SQLException {
    List<UserEntity> users = userRepository.findTop50ByOrderByCoinBalanceDescUsernameAsc();
    JSONArray result = new JSONArray();

    for (UserEntity userEntity : users) {
      expirePremiumBenefitsIfNeeded(userEntity);
      JSONArray row = new JSONArray();
      row.put(userEntity.getId());
      row.put(userEntity.getUsername());
      row.put(intOrZero(userEntity.getCoinBalance()));
      row.put(isPremiumActive(userEntity));
      row.put(intOrZero(userEntity.getPicId()));
      row.put(resolveAvatarUrl(userEntity));
      result.put(row);
    }

    return result;
  }

  public Integer getCurrentParticipantsNumber(int gameId) throws SQLException, DatabaseAccessException {
    if (!checkGameExists(gameId)) {
      throw new DatabaseAccessException();
    }

    return (int) userRepository.countParticipantsInGame(gameId);
  }

  public void changeParticipantsNumber(int gameId, int participantsNumber) throws SQLException, DatabaseAccessException {
    GameEntity gameEntity = getGameOrThrow(gameId);
    gameEntity.setParticipantsNumber(participantsNumber);
    gameRepository.save(gameEntity);
  }

  public JSONArray getOpenGames() throws SQLException, DatabaseAccessException {
    List<GameEntity> games = gameRepository.findByIsPrivateFalseAndStatusOrderByIdAsc(0);
    JSONArray result = new JSONArray();

    for (GameEntity gameEntity : games) {
      JSONArray row = new JSONArray();
      int gameId = gameEntity.getId();
      row.put(gameId);
      row.put(gameEntity.getTopic() == null ? 0 : gameEntity.getTopic().getId());
      row.put(getCurrentParticipantsNumber(gameId));
      row.put(gameEntity.getParticipantsNumber());
      row.put(GameModeCatalog.normalize(gameEntity.getGameMode()).name());
      result.put(row);
    }

    return result;
  }

  public JSONArray getOpenGames(int topicId) throws SQLException, DatabaseAccessException {
    List<GameEntity> games = gameRepository.findByIsPrivateFalseAndStatusAndTopic_IdOrderByIdAsc(0, topicId);
    JSONArray result = new JSONArray();

    for (GameEntity gameEntity : games) {
      JSONArray row = new JSONArray();
      int gameId = gameEntity.getId();
      row.put(gameId);
      row.put(getCurrentParticipantsNumber(gameId));
      row.put(gameEntity.getParticipantsNumber());
      row.put(GameModeCatalog.normalize(gameEntity.getGameMode()).name());
      result.put(row);
    }

    return result;
  }

  //
  // Achievements TABLE REFERRED METHODS
  //

  public Integer addAchievement(Achievement achievement) throws SQLException {
    AchievementEntity achievementEntity = new AchievementEntity();
    achievementEntity.setName(achievement.getName());
    achievementEntity.setProfilePicNeeded(achievement.getProfilePicNeeded());
    achievementEntity.setDescriptionNeeded(achievement.getDescriptionNeeded());
    achievementEntity.setGamesNumberNeeded(intOrZero(achievement.getGamesNumberNeeded()));
    achievementEntity.setGlobalPointsNeeded(intOrZero(achievement.getGlobalPointsNeeded()));
    achievementEntity.setGlobalRatingPlaceNeeded(intOrZero(achievement.getGlobalRatingPlaceNeeded()));
    achievementEntity.setCurrentGamePointsNeeded(intOrZero(achievement.getCurrentGamePointsNeeded()));
    achievementEntity.setCurrentGameRatingNeeded(intOrZero(achievement.getCurrentGameRatingNeeded()));
    achievementEntity.setCurrentGameLevelDifficultyNeeded(intOrZero(achievement.getCurrentGameLevelDifficultyNeeded()));
    return achievementRepository.save(achievementEntity).getId();
  }

  public void attachAchievement(String session, int achievementId) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    AchievementEntity achievementEntity = achievementRepository.findById(achievementId).orElseThrow(DatabaseAccessException::new);

    UserAchievementEntity userAchievementEntity = new UserAchievementEntity();
    userAchievementEntity.setUser(userEntity);
    userAchievementEntity.setAchievement(achievementEntity);
    userAchievementRepository.save(userAchievementEntity);
  }

  public Integer[] checkAchievementAchieved(String session, Achievement achieved) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);

    List<Integer> achievementIds = achievementRepository.findQualifiedUnattachedAchievementIds(
        userEntity.getId(),
        achieved.getProfilePicNeeded(),
        achieved.getDescriptionNeeded(),
        intOrZero(achieved.getGamesNumberNeeded()),
        intOrZero(achieved.getGlobalPointsNeeded()),
        intOrZero(achieved.getGlobalRatingPlaceNeeded()),
        intOrZero(achieved.getCurrentGamePointsNeeded()),
        intOrZero(achieved.getCurrentGameRatingNeeded()),
        intOrZero(achieved.getCurrentGameLevelDifficultyNeeded())
    );

    return toIntegerArray(achievementIds);
  }

  public Integer[] getAchievementsOf(String session) throws SQLException, DatabaseAccessException {
    UserEntity userEntity = getUserBySessionOrThrow(session);
    List<UserAchievementEntity> attached = userAchievementRepository.findByUser_IdOrderByAchievement_Id(userEntity.getId());

    List<Integer> achievementIds = new ArrayList<>();
    for (UserAchievementEntity userAchievementEntity : attached) {
      achievementIds.add(userAchievementEntity.getAchievement().getId());
    }

    return toIntegerArray(achievementIds);
  }

  public Achievement getAchievementById(int achievementId) throws SQLException, DatabaseAccessException {
    AchievementEntity achievementEntity = achievementRepository.findById(achievementId).orElseThrow(DatabaseAccessException::new);
    return mapAchievement(achievementEntity);
  }

  public Achievement[] getAllAchievements() throws SQLException {
    List<AchievementEntity> achievementEntities = achievementRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    List<Achievement> achievements = new ArrayList<>();

    for (AchievementEntity achievementEntity : achievementEntities) {
      achievements.add(mapAchievement(achievementEntity));
    }

    return achievements.toArray(new Achievement[0]);
  }

  public void removeAchievement(int achievementId) throws SQLException {
    achievementRepository.deleteById(achievementId);
  }

  //
  // Topics TABLE REFERRED METHODS
  //

  public Integer addTopic(Topic topic) throws SQLException {
    TopicEntity topicEntity = new TopicEntity();
    topicEntity.setName(topic.getName());
    return topicRepository.save(topicEntity).getId();
  }

  public Topic getTopicById(int id) throws SQLException, DatabaseAccessException {
    TopicEntity topicEntity = topicRepository.findById(id).orElseThrow(DatabaseAccessException::new);
    return mapTopic(topicEntity);
  }

  public Topic[] getAllTopics() throws SQLException {
    List<TopicEntity> topicEntities = topicRepository.findAllByOrderByIdAsc();
    List<Topic> topics = new ArrayList<>();

    for (TopicEntity topicEntity : topicEntities) {
      topics.add(mapTopic(topicEntity));
    }

    return topics.toArray(new Topic[0]);
  }

  public void removeTopic(int topicId) throws SQLException {
    topicRepository.deleteById(topicId);
  }

  //
  // AUXILIARY METHODS FOR TESTING
  //

  public void eraseAllData(String keyword) throws SQLException {
    if (!DELETE_ALL_KEYWORD.equals(keyword)) {
      return;
    }

    userAchievementRepository.deleteAll();
    coinTransactionRepository.deleteAll();
    answeredCorrectlyQuestionRepository.deleteAll();
    gameHistoryRepository.deleteAll();
    questionRepository.deleteAll();
    userRepository.deleteAll();
    gameRepository.deleteAll();
    achievementRepository.deleteAll();

    GameEntity initialGame = new GameEntity();
    initialGame.setStatus(0);
    initialGame.setParticipantsNumber(1);
    initialGame.setPrivate(null);
    initialGame.setQuestionsValidated(false);
    initialGame.setGameMode(GameModeCatalog.defaultMode());
    gameRepository.save(initialGame);
  }

  private void recordCoinTransaction(
      UserEntity userEntity,
      GameEntity gameEntity,
      int amountDelta,
      int balanceAfter,
      CoinTransactionType transactionType,
      String reason
  ) {
    CoinTransactionEntity transactionEntity = new CoinTransactionEntity();
    transactionEntity.setUser(userEntity);
    transactionEntity.setGame(gameEntity);
    transactionEntity.setAmountDelta(amountDelta);
    transactionEntity.setBalanceAfter(balanceAfter);
    transactionEntity.setTransactionType(transactionType);
    transactionEntity.setReason(reason);
    transactionEntity.setCreatedAt(Timestamp.from(Instant.now()));
    coinTransactionRepository.save(transactionEntity);
  }

  private record RepositoryBundle(
      ConfigurableApplicationContext context,
      UserRepository userRepository,
      GameRepository gameRepository,
      QuestionRepository questionRepository,
      GameHistoryRepository gameHistoryRepository,
      AchievementRepository achievementRepository,
      UserAchievementRepository userAchievementRepository,
      AnsweredCorrectlyQuestionRepository answeredCorrectlyQuestionRepository,
      TopicRepository topicRepository,
      CoinTransactionRepository coinTransactionRepository
  ) {
  }
}
