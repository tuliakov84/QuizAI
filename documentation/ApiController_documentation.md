# Документация ApiController

## Общее описание
Класс `ApiController` является основным REST-контроллером приложения Quiz AI Arena, обрабатывающим все HTTP-запросы от клиента. Контроллер обеспечивает аутентификацию, управление играми, вопросами, темами и достижениями.

---

## Методы аутентификации

**function** auth  
**описание:** Аутентифицирует пользователя по логину и паролю, создает сессию  
**принимает:** User user (объект с username и password)  
**возвращает:** ResponseEntity<Object> (User с session, userId, currentGame, description, globalPoints)  
**выбрасывает:**
- "Failed to authenticate user" (404)
- "Database error occurred while authenticating user" (500)
- "Unable to generate unique session token" (500)

**function** register  
**описание:** Регистрирует нового пользователя и выполняет автоматическую аутентификацию  
**принимает:** User user (объект с username и password)  
**возвращает:** ResponseEntity<Object> (аналогично auth)  
**выбрасывает:**
- "Password validation error. Bad password" (400)
- "Username validation error. Bad username" (400)
- "Failed to register user" (404)
- "Database error occurred while registering user" (500)

**function** logout  
**описание:** Завершает сессию пользователя  
**принимает:** User user (объект с session)  
**возвращает:** ResponseEntity<Object> (200 OK)  
**выбрасывает:**
- "Failed to log out of account" (404)
- "Database error occurred while logging out" (500)

---

## Методы профиля пользователя

**function** getProfile  
**описание:** Получает полную информацию о профиле пользователя  
**принимает:** User user (объект с session)  
**возвращает:** ResponseEntity<Object> (User с achievements, gamesPlayed, picId, description, lastActivity, globalPoints)  
**выбрасывает:**
- "Failed to get information about account" (404)
- "Database error occurred while getting information" (500)

**function** changeProfilePic  
**описание:** Изменяет аватарку профиля  
**принимает:** User user (объект с session и picId)  
**возвращает:** ResponseEntity<Object> (200 OK)  
**выбрасывает:**
- "Cant change to null picture" (404)
- "Database error occurred while configuring account" (500)
- "Failed to configure information about user" (404)

**function** changeDescription  
**описание:** Изменяет описание профиля  
**принимает:** User user (объект с session и description)  
**возвращает:** ResponseEntity<Object> (200 OK)  
**выбрасывает:**
- "Description validation error. Bad description" (400)
- "Database error occurred while configuring account" (500)
- "Failed to configure information about user" (404)

**function** changePassword  
**описание:** Изменяет пароль пользователя  
**принимает:** User user (объект с session и password)  
**возвращает:** ResponseEntity<Object> (200 OK)  
**выбрасывает:**
- "Password validation error. Bad password" (400)
- "Database error occurred while configuring account" (500)
- "Failed to configure information about user" (404)

**function** getGames  
**описание:** Получает историю игр пользователя  
**принимает:** User user (объект с session)  
**возвращает:** ResponseEntity<Object> (массив Game с gameId, levelDifficulty, numberOfQuestions, participantsNumber, topicId, gameEndTime)  
**выбрасывает:**
- "Database error occurred while getting information" (500)
- "Failed to get information about user" (404)

---

## Методы управления игрой

**function** joinRoom  
**описание:** Присоединяет пользователя к игровой комнате  
**принимает:** RoomJoinObject data (объект с session и gameId)  
**возвращает:** ResponseEntity<Object> (RoomJoinObject с authorId, levelDifficulty, numberOfQuestions, participantsNumber, topicId, currentParticipantsNumber, isPrivate)  
**выбрасывает:**
- "Failed to join room" (404)
- "Database error occurred while joining room" (500)
- "Failed to join room because it's already started" (409)

**function** leaveRoom  
**описание:** Покидает текущую игровую комнату  
**принимает:** User user (объект с session)  
**возвращает:** ResponseEntity<Object> (200 OK)  
**выбрасывает:**
- "Failed to leave the game" (404)
- "Database error occurred while leaving the game" (500)

**function** getOpenGames  
**описание:** Получает список открытых игр по теме  
**принимает:** Topic topic (объект с topicId)  
**возвращает:** ResponseEntity<Object> (JSON строку со списком игр)  
**выбрасывает:**
- "Failed to get open games" (404)
- "Database error occurred while getting open games" (500)

**function** createGame  
**описание:** Создает новую игровую комнату  
**принимает:** RoomJoinObject data (объект с session, levelDifficulty, numberOfQuestions, participantsNumber, topicId, isPrivate)  
**возвращает:** ResponseEntity<Object> (аналогично joinRoom)  
**выбрасывает:**
- "Failed to create room" (404)
- "Database error occurred while creating game" (500)

**function** changeParticipantsNumber  
**описание:** Изменяет максимальное количество участников в комнате  
**принимает:** RoomJoinObject data (объект с session, gameId, participantsNumber)  
**возвращает:** ResponseEntity<Object> (обновленный RoomJoinObject)  
**выбрасывает:**
- "User is not the author of game" (400)
- "Failed to update room" (404)
- "Database error occurred while updating game" (500)

**function** setPrivate  
**описание:** Устанавливает приватность игры  
**принимает:** Game game (объект с gameId и isPrivate)  
**возвращает:** ResponseEntity<Object> (200 OK)  
**выбрасывает:**
- "Failed to modify privateness option for game" (404)
- "Database error occurred while modifying privateness option" (500)

**function** getLobby  
**описание:** Получает информацию о лобби игры  
**принимает:** LobbyObject lobby (объект с gameId)  
**возвращает:** ResponseEntity<Object> (LobbyObject со status и playersUsernames)  
**выбрасывает:**
- "Failed to modify privateness option for game" (404)
- "Database error occurred while modifying privateness option" (500)

**function** startGame  
**описание:** Запускает игру  
**принимает:** Game game (объект с gameId)  
**возвращает:** ResponseEntity<Object> (200 OK)  
**выбрасывает:**
- "Failed to start the game" (404)
- "Database error occurred while stating game" (500)

**function** getQuestion  
**описание:** Получает вопрос по номеру в игре  
**принимает:** Question question (объект с gameId и questionNumber)  
**возвращает:** ResponseEntity<Object> (Question объект)  
**выбрасывает:**
- "Failed to get question" (404)
- "Database error occurred while getting question" (500)

**function** verifyAnswer  
**описание:** Проверяет правильность ответа на вопрос  
**принимает:** Question question (объект с gameId, questionNumber и submittedAnswerNumber)  
**возвращает:** ResponseEntity<Object> (boolean - правильный ответ или нет)  
**выбрасывает:**
- "Failed to get verify" (404)
- "Database error occurred while verifying question" (500)

**function** pauseGame  
**описание:** Приостанавливает игру  
**принимает:** Game game (объект с gameId)  
**возвращает:** ResponseEntity<Object> (200 OK)  
**выбрасывает:**
- "Failed to pause the game" (404)
- "Database error occurred while pausing game" (500)

**function** resumeGame  
**описание:** Возобновляет приостановленную игру  
**принимает:** Game game (объект с gameId)  
**возвращает:** ResponseEntity<Object> (200 OK)  
**выбрасывает:**
- "Failed to resume the game" (404)
- "Database error occurred while pausing game" (500)

**function** stopGame  
**описание:** Останавливает игру  
**принимает:** Game game (объект с gameId)  
**возвращает:** ResponseEntity<Object> (200 OK)  
**выбрасывает:**
- "Failed to stop the game" (404)
- "Database error occurred while stopping game" (500)

**function** deleteGame  
**описание:** Удаляет игру  
**принимает:** Game game (объект с gameId)  
**возвращает:** ResponseEntity<Object> (200 OK)  
**выбрасывает:**
- "Failed to stop the game" (404)
- "Database error occurred while stopping game" (500)

---

## Методы тем

**function** getTopic  
**описание:** Получает тему по ID  
**принимает:** Topic topic (объект с topicId)  
**возвращает:** ResponseEntity<Object> (Topic объект)  
**выбрасывает:**
- "Failed to get the topic" (404)
- "Database error occurred while getting topic" (500)

**function** getAllTopics  
**описание:** Получает все доступные темы  
**принимает:** -  
**возвращает:** ResponseEntity<Object> (список Topic объектов)  
**выбрасывает:**
- "Database error occurred while getting all topics" (500)

---

## Методы лидербордов

**function** getLeaderboardsByGame  
**описание:** Получает таблицу лидеров для конкретной игры  
**принимает:** Game game (объект с gameId)  
**возвращает:** ResponseEntity<Object> (JSON строка с лидербордом)  
**выбрасывает:**
- "Database error occurred while getting the leaderboards" (500)
- "Failed to get the leaderboards" (404)

**function** getGlobalLeaderboards  
**описание:** Получает глобальную таблицу лидеров  
**принимает:** -  
**возвращает:** ResponseEntity<Object> (JSON строка из файла global-leaderboards.json)  
**выбрасывает:**
- "Error reading global leaderboards" (500)

---

## Методы достижений

**function** getAllAchievements  
**описание:** Получает все достижения системы  
**принимает:** -  
**возвращает:** ResponseEntity<Object> (список Achievement объектов)  
**выбрасывает:**
- "Database error occurred while getting all achievements" (500)

**function** getAchievementList  
**описание:** Получает достижения по списку ID  
**принимает:** List<Integer> achievementIds (список ID достижений)  
**возвращает:** ResponseEntity<Object> (список Achievement объектов)  
**выбрасывает:**
- "Failed to get the achievements" (404)
- "Database error occurred while getting achievements" (500)

**function** checkAchieved  
**описание:** Проверяет, какие достижения получены пользователем  
**принимает:** Achieved achieved (объект с session)  
**возвращает:** ResponseEntity<Object> (список Achievement объектов)  
**выбрасывает:**
- "Database error occurred while getting all achievements" (500)
- "Failed to get the achievements" (404)

**function** attachAchievementList  
**описание:** Прикрепляет достижения пользователям  
**принимает:** List<Achieved> achievements (список объектов с session и achievementId)  
**возвращает:** ResponseEntity<Object> (200 OK)  
**выбрасывает:**
- "Failed to attach the achievements" (404)
- "Database error occurred while attaching achievements" (500)

---

## Примечания

1. Все методы используют сессию (session) для идентификации пользователя
2. Валидация входных данных выполняется перед обработкой запросов
3. Ошибки базы данных обрабатываются с возвратом соответствующих HTTP-статусов
4. Контроллер инициализирует таблицу тем при создании через TopicsInit