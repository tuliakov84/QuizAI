<h1>Документация библиотеки DbService</h1>

<h2>Методы пользователя</h2>

**function** checkUserExists  
**описание:** Проверяет существование пользователя  
**принимает:** String username  
**возвращает:** boolean  
**выбрасывает:** -

**function** getUserId  
**описание:** Проверяет авторизацию пользователя по сессии  
**принимает:** String session  
**возвращает:** Integer userId (null, если не найдено)  
**выбрасывает:** -

**function** register  
**описание:** Регистрирует нового пользователя  
**принимает:** String username, String password  
**выбрасывает:** User already exists

**function** authorize  
**описание:** Авторизирует пользователя в системе  
**принимает:** String username, String password, String session  
**выбрасывает:** Session key is not unique and already exists in database, Wrong password, Not found

**function** changePassword  
**описание:** Устанавливает новый пароль для пользователя  
**принимает:** String session, String newPassword  
**выбрасывает:** -

**function** changeProfilePic  
**описание:** Изменяет аватарку профиля  
**принимает:** String session, int picId  
**выбрасывает:** -

**function** getProfilePic  
**описание:** Получает ID аватарки профиля  
**принимает:** String session  
**возвращает:** int picId  
**выбрасывает:** Not found  

**function** getUsername (по userId)  
**описание:** Получает имя пользователя по ID  
**принимает:** int userId  
**возвращает:** String username  
**выбрасывает:** Not found  

**function** getUsername (по session)  
**описание:** Получает имя пользователя по сессии  
**принимает:** String session  
**возвращает:** String username  
**выбрасывает:** Not found  

**function** changeDescription  
**описание:** Изменяет описание профиля  
**принимает:** String session, String description  
**выбрасывает:** -

**function** getDescription  
**описание:** Получает описание профиля  
**принимает:** String session  
**возвращает:** String description  
**выбрасывает:** Not found

**function** setLastActivity  
**описание:** Устанавливает время последней активности  
**принимает:** String session, Instant time  
**выбрасывает:** -

**function** getLastActivity  
**описание:** Получает время последней активности  
**принимает:** String session  
**возвращает:** Timestamp lastActivity  
**выбрасывает:** Not found

**function** setCurrentGame  
**описание:** Устанавливает текущую игру для пользователя  
**принимает:** String session, Integer gameId (чтобы снять с игры, указать null)  
**выбрасывает:** Request is out of bounds, Not found

**function** getCurrentGame  
**описание:** Получает ID текущей игры пользователя  
**принимает:** String session  
**возвращает:** Integer gameId (null, если не в игре)  
**выбрасывает:** Not found  

**function** addGamePlayed  
**описание:** Добавляет сыгранную игру в историю  
**принимает:** String session  
**выбрасывает:** Not found

**function** getGamesPlayed  
**описание:** Получает историю игр пользователя  
**принимает:** String session  
**возвращает:** Integer[] gameIds  
**выбрасывает:** Not found

**function** addGlobalPoints  
**описание:** Добавляет глобальные очки пользователю  
**принимает:** String session, int points  
**выбрасывает:** -

**function** getGlobalPoints  
**описание:** Получает глобальные очки пользователя  
**принимает:** String session  
**возвращает:** Integer globalPoints  
**выбрасывает:** Not found

**function** addGlobalPossiblePoints  
**описание:** Добавляет возможные глобальные очки  
**принимает:** String session, int possiblePoints  
**выбрасывает:** -

**function** getGlobalPossiblePoints  
**описание:** Получает возможные глобальные очки  
**принимает:** String session  
**возвращает:** Integer globalPossiblePoints  
**выбрасывает:** Not found

**function** addCurrentGamePoints  
**описание:** Добавляет очки в текущей игре  
**принимает:** String session, int points  
**выбрасывает:** -

**function** getCurrentGamePoints  
**описание:** Получает очки в текущей игре  
**принимает:** String session  
**возвращает:** Integer currentGamePoints  
**выбрасывает:** Not found

**function** logOut  
**описание:** Выход пользователя из системы  
**принимает:** String session  
**выбрасывает:** -

<h2>Методы игры</h2>

**function** createGame  
**описание:** Создает новую игровую комнату  
**принимает:** String sessionOfAuthor, int levelDifficulty, int numberOfQuestions, int participantsNumber, int topicId  
**возвращает:** Integer gameId  
**выбрасывает:** Not found, Bad params

**function** checkGameExists  
**описание:** Проверяет существование игры  
**принимает:** int gameId  
**возвращает:** boolean  
**выбрасывает:** -

**function** getPreset  
**описание:** Получает настройки игры  
**принимает:** int gameId  
**возвращает:** Integer[] preset (authorId, levelDifficulty, numberOfQuestions, participantsNumber, topicId)  
**выбрасывает:** Not found

**function** setStatus  
**описание:** Устанавливает статус игры  
**принимает:** int gameId, int status  
**выбрасывает:** -

**function** getStatus  
**описание:** Получает статус игры  
**принимает:** int gameId  
**возвращает:** Integer status  
**выбрасывает:** Not found  

**function** setGameStartTime  
**описание:** Устанавливает время начала игры  
**принимает:** int gameId, Instant gameStartTime  
**выбрасывает:** -

**function** getGameStartTime  
**описание:** Получает время начала игры  
**принимает:** int gameId  
**возвращает:** Timestamp gameStartTime  
**выбрасывает:** Not found

**function** setPrivate  
**описание:** Устанавливает приватность игры  
**принимает:** int gameId, boolean isPrivate  
**выбрасывает:** -

**function** getPrivate  
**описание:** Получает статус приватности игры  
**принимает:** int gameId  
**возвращает:** Boolean isPrivate  
**выбрасывает:** Not found

**function** setGameEndTime  
**описание:** Устанавливает время окончания игры  
**принимает:** int gameId, Instant gameEndTime  
**выбрасывает:** -

**function** getGameEndTime  
**описание:** Получает время окончания игры  
**принимает:** int gameId  
**возвращает:** Timestamp gameEndTime  
**выбрасывает:** Not found

**function** stopGame  
**описание:** Останавливает игру  
**принимает:** int gameId  
**выбрасывает:** Not found

**function** getCurrentQuestionNumber  
**описание:** Получает текущий номер вопроса в игре  
**принимает:** int gameId  
**возвращает:** Integer currentQuestionNumber  
**выбрасывает:** Not found

**function** nextQuestion  
**описание:** Возвращает следующий вопрос в игре  
**принимает:** int gameId  
**возвращает:** Question questionObj (null, если вопросов больше нет)  
**выбрасывает:** Not found

**function** getRightAnswer  
**описание:** Получает правильный ответ на вопрос  
**принимает:** int gameId, int questionNumber  
**возвращает:** Integer rightAnswerNumber  
**выбрасывает:** Game is not active, Not found

**function** loadQuestions  
**описание:** Загружает вопросы в игру из JSON (пример показан в ml-answer-example.json)  
**принимает:** int gameId, JSONArray jsonArr  
**выбрасывает:** -

**function** getGameLeaderboards  
**описание:** Получает таблицу лидеров текущей игры  
**принимает:** int gameId  
**возвращает:** JSONArray leaderboards  
**выбрасывает:** Not found

**function** getGlobalLeaderboards  
**описание:** Получает глобальную таблицу лидеров  
**принимает:** -  
**возвращает:** JSONArray leaderboards  
**выбрасывает:** -

**function** getCurrentParticipantsNumber  
**описание:** Получает текущее количество участников в игре  
**принимает:** int gameId  
**возвращает:** Integer participantsNumber  
**выбрасывает:** Not found

**function** getOpenGames  
**описание:** Получает список открытых игр  
**принимает:** -  
**возвращает:** JSONArray openGames  
**выбрасывает:** -

<h2>Методы достижений</h2>

**function** addAchievement  
**описание:** Создает новое достижение в БД  
**принимает:** Achievement achievement  
**возвращает:** Integer achievementId  
**выбрасывает:** -

**function** attachAchievement  
**описание:** Прикрепляет достижение пользователю  
**принимает:** String session, int achievementId  
**выбрасывает:** Not found

**function** checkAchievementAchieved  
**описание:** Проверяет выполненные достижения на основе статистики, изложенной в объекте achieved  
**принимает:** String session, Achievement achieved  
**возвращает:** Integer[] achievementIds  
**выбрасывает:** Not found

**function** getAchievementsOf  
**описание:** Получает достижения пользователя  
**принимает:** String session  
**возвращает:** Integer[] achievementIds  
**выбрасывает:** Not found

**function** getAchievementById  
**описание:** Получает достижение по ID  
**принимает:** int achievementId  
**возвращает:** Achievement achievementObj  
**выбрасывает:** Not found

**function** getAllAchievements  
**описание:** Получает все достижения из БД  
**принимает:** -  
**возвращает:** Achievement[] achievements  
**выбрасывает:** -

**function** removeAchievement  
**описание:** Удаляет достижение из БД  
**принимает:** int achievementId  
**выбрасывает:** -

**function** addTopic  
**описание:** Создает новую тему в БД  
**принимает:** Topic topic  
**возвращает:** Integer topicId  
**выбрасывает:** -

**function** getTopicById  
**описание:** Получает тему по ID  
**принимает:** int topicId  
**возвращает:** Topic topic  
**выбрасывает:** Not found

**function** getAllTopics  
**описание:** Получает все темы из БД  
**принимает:** -  
**возвращает:** Topic[] topics  
**выбрасывает:** -

**function** removeTopic  
**описание:** Удаляет тему из БД  
**принимает:** int topicId  
**выбрасывает:** -

<h3>Вспомогательные функции</h3>

**function** eraseAllData  
**описание:** Очищает все данные в базе (для тестирования)  
**принимает:** String keyword  
**выбрасывает:** -
