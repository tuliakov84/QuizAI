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

  // Получение URL аватара
  static getAvatarUrl(picId) {
    if (!picId || picId <= 0 || picId > 8) {
      // Возвращаем дефолтный аватар или пустую строку
      return '';
    }
    return `/assets.avatars/avatar${picId}.png`;
  }

  // Отображение аватара на элементе
  static displayAvatar(elementId, picId) {
    const element = document.getElementById(elementId);
    if (!element) return;

    const avatarUrl = this.getAvatarUrl(picId);
    if (avatarUrl) {
      console.log('Setting avatar URL:', avatarUrl); // Для отладки
      element.style.backgroundImage = `url('${avatarUrl}')`;
      element.style.backgroundSize = 'cover';
      element.style.backgroundPosition = 'center';
      element.style.backgroundColor = 'transparent';
      element.classList.add('has-avatar');

      // Очищаем текст, если он был
      element.textContent = '';
    } else {
      // Если нет аватара, показываем первую букву имени
      const user = this.getUser();
      if (user && user.username) {
        element.textContent = user.username.charAt(0).toUpperCase();
        element.style.backgroundColor = '#4a90e2';
        element.style.display = 'flex';
        element.style.alignItems = 'center';
        element.style.justifyContent = 'center';
        element.style.color = 'white';
        element.style.fontWeight = 'bold';
        element.style.backgroundImage = 'none'; // Убираем фон
      }
    }
  }

  // Загрузка профиля пользователя
  static async loadUserProfile() {
    try {
      const session = this.getsession();
      const user = this.getUser();

      const requestData = {
        session: session,
        username: user?.username || ''
      };

      const response = await fetch('http://localhost:8080/api/users/profile', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestData)
      });

      if (response.ok) {
        const userData = await response.json();
        localStorage.setItem('user', JSON.stringify(userData));
        return userData;
      } else {
        console.error('Ошибка загрузки профиля:', response.status);
        return null;
      }
    } catch (error) {
      console.error('Ошибка загрузки профиля:', error);
      return null;
    }
  }

  // Отображение данных профиля на странице
  static displayUserProfile(userData) {
    // Имя пользователя
    if (document.getElementById('user-username') && userData.username) {
      document.getElementById('user-username').textContent = userData.username;
    }

    // Статус
    if (document.getElementById('user-status') && userData.description) {
      document.getElementById('user-status').textContent = userData.description;
    } else if (document.getElementById('user-status')) {
      document.getElementById('user-status').textContent = "Статус не установлен";
    }

    // Рейтинг
    if (document.getElementById('user-rating') && userData.globalPoints !== undefined) {
      document.getElementById('user-rating').textContent = userData.globalPoints;
    }

    // Аватар
    if (userData.picId) {
      this.displayAvatar('user-avatar', userData.picId);
    }
  }

  // Обновление аватара
  static async updateAvatar(picId) {
    try {
      const session = this.getsession();
      const response = await fetch('http://localhost:8080/api/users/set/profile_pic', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          session: session,
          picId: picId
        })
      });

      if (response.ok) {
        // Обновляем локальные данные пользователя
        const user = this.getUser();
        if (user) {
          user.picId = picId;
          localStorage.setItem('user', JSON.stringify(user));
        }
        return true;
      }
      return false;
    } catch (error) {
      console.error('Ошибка обновления аватара:', error);
      return false;
    }
  }

  // Получение списка доступных аватаров
  static getAvailableAvatars() {
    return [
      { id: 1, name: 'Аватар 1', url: '/templates/assets.avatars/avatar1.png' },
      { id: 2, name: 'Аватар 2', url: '/templates/assets.avatars/avatar2.png' },
      { id: 3, name: 'Аватар 3', url: '/templates/assets.avatars/avatar3.png' },
      { id: 4, name: 'Аватар 4', url: '/templates/assets.avatars/avatar4.png' },
      { id: 5, name: 'Аватар 5', url: '/templates/assets.avatars/avatar5.png' },
      { id: 6, name: 'Аватар 6', url: '/templates/assets.avatars/avatar6.png' },
      { id: 7, name: 'Аватар 7', url: '/templates/assets.avatars/avatar7.png' },
      { id: 8, name: 'Аватар 8', url: '/templates/assets.avatars/avatar8.png' }
    ];
  }

  // Обновление профиля пользователя
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

  // Получение глобального рейтинга
  static async getGlobalLeaderboard() {
    try {
      const session = this.getsession();
      const response = await fetch('http://localhost:8080/api/leaderboard/get/global', {
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
