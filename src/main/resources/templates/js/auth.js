


class AuthService {


  // Проверка авторизации
  static isAuthenticated() {
    const session = localStorage.getItem('session');
    const user = localStorage.getItem('user');
    return !!(session && user);
  }

  // Получение данных пользователя
  static getUser() {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  }

  // Получение токена
  static getsession() {
    return localStorage.getItem('session');
  }

  // Проверка и перенаправление если не авторизован
  static requireAuth(redirectUrl = 'index.html') {
    if (!this.isAuthenticated()) {
      window.location.href = redirectUrl;
      return false;
    }
    return true;
  }

  // Загрузка данных профиля с сервера
  static async loadUserProfile() {
    try {
      const session = this.getsession();
      const response = await fetch('http://localhost:8080/api/users/profile', {
        method: 'GET',
        headers: {
          'Authorization': 'Bearer ' + session
        }
      });
      if (response.ok) {
        const userData = await response.json();
        localStorage.setItem('user', JSON.stringify(userData));
        return userData;
      }
      return null;
    } catch (error) {
      console.error('Ошибка загрузки профиля:', error);
      return null;
    }
  }

  // Отображение данных пользователя на странице
  static displayUserProfile(userData) {
    // Имя пользователя
    if (document.getElementById('user-username') && userData.username) {
      document.getElementById('user-username').textContent = userData.username;
    }

    // Статус
    if (document.getElementById('user-status') && userData.description) {
      document.getElementById('user-status').textContent =
        userData.description;
    } else {
      document.getElementById('user-status').textContent =
        "Статус не установлен";
    }

    // Рейтинг
    if (document.getElementById('user-rating') && userData.globalPoints !== undefined) {
      document.getElementById('user-rating').textContent = userData.globalPoints;
    }

    // Аватар
    // Если есть URL аватара, устанавливаем как фон
    // ДОРАБОТАТЬ
    if (document.getElementById('user-avatar') && userData.avatarUrl) {
      const avatar = document.getElementById('user-avatar');
      avatar.style.backgroundImage = `url('${userData.avatarUrl}')`;
      avatar.style.backgroundSize = 'cover';
      avatar.style.backgroundPosition = 'center';
    }
  }

  static async updateUserProfile(userData) {
    try {
      const session = this.getsession();
      const response = await fetch('http://localhost:8080/api/users/profile', {
        method: 'PUT',
        headers: {
          'Authorization': 'Bearer ' + session,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(userData)
      });

      if (response.ok) {
        const updatedUserData = await response.json();
        localStorage.setItem('user', JSON.stringify(updatedUserData));
        return updatedUserData;
      }
      return null;
    } catch (error) {
      console.error('Ошибка обновления профиля:', error);
      return null;
    }
  }


  static async getGlobalLeaderboard() {
    try {
      const session = this.getsession();
      const response = await fetch('http://localhost:8080/api/leaderboard/global', {
        method: 'GET',
        headers: {
          'Authorization': 'Bearer ' + session,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        return await response.json();
      }
      return null;
    } catch (error) {
      console.error('Ошибка загрузки рейтинга:', error);
      return null;
    }
  }
}