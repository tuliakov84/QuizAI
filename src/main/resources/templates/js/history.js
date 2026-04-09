document.addEventListener('DOMContentLoaded', async function() {
    if (!AuthService.requireAuth()) return;
    await loadGamesHistory();

    // Закрытие модального окна
    const modal = document.getElementById('questionsModal');
    const closeBtn = document.querySelector('.close');
    if (closeBtn) {
        closeBtn.onclick = () => modal.style.display = 'none';
    }
    window.onclick = (event) => {
        if (event.target === modal) modal.style.display = 'none';
    };
});

async function loadGamesHistory() {
    const container = document.getElementById('games-container');
    container.innerHTML = '<div class="loading loading-pulse">Загрузка истории...</div>';

    try {
        const session = AuthService.getsession();
        if (!session) throw new Error('Сессия не найдена');

        const gamesResponse = await fetch('http://localhost:8080/api/users/get-games', {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + session,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ session: session })
        });

        if (!gamesResponse.ok) throw new Error(`Ошибка загрузки игр: ${gamesResponse.status}`);
        const games = await gamesResponse.json();

        if (!games || games.length === 0) {
            container.innerHTML = '<div class="no-games-message">Вы ещё не завершили ни одного квиза</div>';
            return;
        }

        console.log('Полученные игры:', games);
        games.forEach(game => {
            console.log(`Игра ${game.gameId}: сложность = ${game.levelDifficulty} (тип ${typeof game.levelDifficulty})`);
        });

        container.innerHTML = '';
        for (const game of games) {
            const gameElement = await createGameElement(game, session);
            container.appendChild(gameElement);
        }
    } catch (error) {
        console.error('Ошибка загрузки истории:', error);
        container.innerHTML = '<div class="error-message">Не удалось загрузить историю</div>';
    }
}

async function createGameElement(game, session) {
    const gameDiv = document.createElement('div');
    gameDiv.className = 'game';

    // Загружаем название темы
    let topicName = 'Неизвестная тема';
    try {
        const topicResponse = await fetch('http://localhost:8080/api/topic/get', {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + session,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ topicId: game.topicId })
        });
        if (topicResponse.ok) {
            const topic = await topicResponse.json();
            topicName = topic.name || topic.topicName || 'Неизвестная тема';
        }
    } catch (error) {
        console.warn(`Не удалось загрузить тему для игры ${game.gameId}`);
    }

    // Форматируем дату
    let dateStr = 'Дата неизвестна';
    if (game.gameEndTime) {
        const date = new Date(game.gameEndTime);
        dateStr = date.toLocaleString('ru-RU', {
            day: '2-digit', month: '2-digit', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    }

    let difficultyText = '';
    switch (game.levelDifficulty) {
        case "EASY": difficultyText = 'Лёгкая'; break;
        case "MEDIUM": difficultyText = 'Средняя'; break;
        case "HARD": difficultyText = 'Сложная'; break;
        default: difficultyText = 'Неизвестно';
    }

    gameDiv.innerHTML = `
        <div class="text">
            <span class="game-topic">${escapeHtml(topicName)}</span>
            <span class="game-difficulty">${difficultyText}</span>
            <span class="game-date">${escapeHtml(dateStr)}</span>
            <button class="view-questions-btn" data-game-id="${game.gameId}">Вопросы и ответы</button>
        </div>
    `;

    const viewBtn = gameDiv.querySelector('.view-questions-btn');
    viewBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        showQuestionsModal(game.gameId, session);
    });

    return gameDiv;
}

async function showQuestionsModal(gameId, session) {
    const modal = document.getElementById('questionsModal');
    const container = document.getElementById('modal-questions-container');
    container.innerHTML = '<div class="loading loading-pulse">Загрузка вопросов...</div>';
    modal.style.display = 'block';

    try {
        const response = await fetch('http://localhost:8080/api/users/get-history-questions', {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + session,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ session: session, gameId: gameId })
        });

        if (!response.ok) throw new Error(`Ошибка загрузки вопросов: ${response.status}`);
        const questions = await response.json();
        displayQuestionsInModal(questions);
    } catch (error) {
        console.error('Ошибка загрузки вопросов:', error);
        container.innerHTML = '<div class="error-message">Не удалось загрузить вопросы</div>';
    }
}

function displayQuestionsInModal(questions) {
    const container = document.getElementById('modal-questions-container');
    container.innerHTML = '';

    if (!questions || questions.length === 0) {
        container.innerHTML = '<div class="no-questions-message">Нет вопросов для отображения</div>';
        return;
    }

    questions.forEach((q, idx) => {
        // ВНИМАНИЕ: используется поле rightAnswerNumber (не correctAnswerNumber)
        const correctNumber = q.rightAnswerNumber;
        let correctAnswerText = '';
        switch (correctNumber) {
            case 1: correctAnswerText = q.answer1; break;
            case 2: correctAnswerText = q.answer2; break;
            case 3: correctAnswerText = q.answer3; break;
            case 4: correctAnswerText = q.answer4; break;
            default: correctAnswerText = 'Не указан';
        }

        const questionDiv = document.createElement('div');
        questionDiv.className = 'history-question';
        questionDiv.innerHTML = `
            <div class="question-text">${idx+1}. ${escapeHtml(q.questionText)}</div>
            <div class="correct-answer">✅ Правильный ответ: ${escapeHtml(correctAnswerText)}</div>
            <hr>
        `;
        container.appendChild(questionDiv);
    });
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>]/g, function(m) {
        if (m === '&') return '&amp;';
        if (m === '<') return '&lt;';
        if (m === '>') return '&gt;';
        return m;
    });
}