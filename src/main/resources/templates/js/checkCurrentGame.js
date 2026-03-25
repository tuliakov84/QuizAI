/**
 * Проверяет наличие текущей игры у пользователя и перенаправляет
 * - если gameId == null → ничего не делаем
 * - если gameStartTime == null → лобби (roommember.html)
 * - иначе → страница вопроса (question.html) с вычисленным номером вопроса
 */
async function checkCurrentGame() {
    const session = AuthService.getsession();
    if (!session) return;

    try {
        const response = await fetch('http://localhost:8080/api/users/get-current-game', {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + session,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ session: session })
        });

        if (!response.ok) {
            console.error('Ошибка получения текущей игры');
            return;
        }

        const gameData = await response.json();
        const gameId = gameData.gameId;
        const gameStartTime = gameData.gameStartTime;

        if (!gameId) {
            return;
        }

        if (!gameStartTime) {
            window.location.href = `roommember.html?gameId=${gameId}`;
        } else {
            // игра началась – вычисляем номер текущего вопроса
            const startTime = new Date(gameStartTime).getTime();
            const timePerQuestion = 30000; // в мс
            const differance = Date.now() - startTime;
            const questionNumber = Math.floor(differance / timePerQuestion) + 1;

            console.error('differance:', differance);
            console.error('номер вопроса:', questionNumber);
            window.location.href = `question.html?gameId=${gameId}&question=${questionNumber}`;
        }
    } catch (error) {
        console.error('Ошибка при проверке текущей игры:', error);
    }
}

window.checkCurrentGame = checkCurrentGame;