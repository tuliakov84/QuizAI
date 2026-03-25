/**
 * Проверяет, выполнены ли условия для достижений, и прикрепляет новые к пользователю
 * @param {string} session - сессия пользователя
 */
async function checkAndAttachAchievements(session) {
    try {
        // 1. Получаем все достижения
        const allAchievements = await fetchAllAchievements(session);

        // 2. Получаем полученные достижения
        const userProfile = await fetchUserProfile(session);
        const earnedIds = new Set(userProfile.achievements || []);

        // Вычисляем доп поля
        const hasProfilePic = userProfile.picId !== 0;
        const hasDescription = userProfile.description && userProfile.description.trim() !== '';

        // 3. Формируем список для прикрепления
        const toAttach = [];

        for (const ach of allAchievements) {
            if (earnedIds.has(ach.achievementId)) continue;

            let meets = true;

            if (ach.gamesNumberNeeded > 0 && userProfile.gamesPlayedNumber < ach.gamesNumberNeeded) meets = false;
            if (ach.globalPointsNeeded > 0 && userProfile.globalPoints < ach.globalPointsNeeded) meets = false;
            if (ach.profilePicNeeded && !hasProfilePic) meets = false;
            if (ach.descriptionNeeded && !hasDescription) meets = false;
            //todo Достижения, связанные с последней игрой (нет данных)

            if (meets) {
                toAttach.push({
                    session: session,
                    achievementId: ach.achievementId
                });
            }
        }

        // 4. Прикрепляем, если есть
        if (toAttach.length > 0) {
            await attachAchievements(session, toAttach);
            console.log(`Прикреплено ${toAttach.length} новых достижений`);
        }
    } catch (error) {
        console.error('Ошибка при проверке достижений:', error);
    }
}

async function fetchAllAchievements(session) {
    const res = await fetch('http://localhost:8080/api/achievement/get-all', {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + session,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({})
    });
    if (!res.ok) throw new Error('Ошибка загрузки достижений');
    return await res.json();
}

async function fetchUserProfile(session) {
    const res = await fetch('http://localhost:8080/api/users/profile', {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + session,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ session: session })
    });
    if (!res.ok) throw new Error('Ошибка загрузки профиля');
    return await res.json();
}

async function attachAchievements(session, achievementsList) {
    const res = await fetch('http://localhost:8080/api/achievement/attach', {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + session,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(achievementsList)
    });
    if (!res.ok) throw new Error('Ошибка прикрепления достижений');
}

window.checkAndAttachAchievements = checkAndAttachAchievements;