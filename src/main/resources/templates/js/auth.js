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
  static getAvatarUrl(picId, customAvatarPath = null) {
    if (customAvatarPath && customAvatarPath.trim()) {
      return customAvatarPath.trim();
    }

    const normalizedPicId = Number(picId);
    if (!Number.isInteger(normalizedPicId) || normalizedPicId <= 0 || normalizedPicId > 8) {
      return '';
    }
    return `/asserts/asserts/avatar${normalizedPicId}.png`;
  }

  // Отображение аватара на элементе
  static displayAvatar(elementId, picId, avatarUrl = null, username = null) {
    const element = document.getElementById(elementId);
    if (!element) return;

    this.displayAvatarElement(element, { picId, avatarUrl, username });
  }

  static displayAvatarElement(element, userData = {}) {
    if (!element) return false;

    const avatarUrl = userData.avatarUrl || this.getAvatarUrl(userData.picId, userData.customAvatarPath);
    if (avatarUrl) {
      element.style.backgroundImage = `url('${avatarUrl}')`;
      element.style.backgroundSize = 'cover';
      element.style.backgroundPosition = 'center';
      element.style.backgroundColor = 'transparent';
      element.style.background = `center / cover no-repeat url('${avatarUrl}')`;
      element.classList.add('has-avatar');
      element.textContent = '';
      element.innerHTML = '';
      return true;
    }

    const fallbackUser = this.getUser();
    const username = userData.username || fallbackUser?.username || '?';
    this.displayAvatarFallback(element, username);
    return false;
  }

  static displayAvatarFallback(element, username) {
    if (!element) return;

    element.classList.remove('has-avatar');
    element.innerHTML = '';
    element.textContent = '';
    element.style.backgroundImage = 'none';
    element.style.background =
      'radial-gradient(circle at 30% 30%, rgba(99, 230, 255, 0.28), transparent 35%), linear-gradient(135deg, #192446 0%, #3d3483 50%, #111b37 100%)';
    element.style.display = 'flex';
    element.style.alignItems = 'center';
    element.style.justifyContent = 'center';
    element.style.color = 'white';
    element.style.fontWeight = 'bold';

    const letterElement = document.createElement('span');
    letterElement.textContent = (username || '?').charAt(0).toUpperCase();
    letterElement.className = 'avatar-letter';
    element.appendChild(letterElement);
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
    this.displayAvatar('user-avatar', userData.picId, userData.avatarUrl || userData.customAvatarPath, userData.username);
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
          user.customAvatarPath = null;
          user.avatarUrl = this.getAvatarUrl(picId);
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

  static async uploadCustomAvatar(file) {
    const session = this.getsession();
    if (!session) {
      throw new Error('Сессия не найдена');
    }
    if (!file) {
      throw new Error('Файл аватарки не выбран');
    }

    const formData = new FormData();
    formData.append('session', session);
    formData.append('avatar', file);

    const response = await fetch('http://localhost:8080/api/users/upload/avatar', {
      method: 'POST',
      body: formData
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || `Ошибка загрузки аватарки: ${response.status}`);
    }

    const avatarData = await response.json();
    const currentUser = this.getUser() || {};
    const updatedUser = {
      ...currentUser,
      ...avatarData,
      picId: 0,
      customAvatarPath: avatarData.customAvatarPath || avatarData.avatarUrl,
      avatarUrl: avatarData.avatarUrl || avatarData.customAvatarPath
    };
    localStorage.setItem('user', JSON.stringify(updatedUser));
    return updatedUser;
  }

  // Получение списка доступных аватаров
  static getAvailableAvatars() {
    return [
      { id: 1, name: 'Аватар 1', url: this.getAvatarUrl(1) },
      { id: 2, name: 'Аватар 2', url: this.getAvatarUrl(2) },
      { id: 3, name: 'Аватар 3', url: this.getAvatarUrl(3) },
      { id: 4, name: 'Аватар 4', url: this.getAvatarUrl(4) },
      { id: 5, name: 'Аватар 5', url: this.getAvatarUrl(5) },
      { id: 6, name: 'Аватар 6', url: this.getAvatarUrl(6) },
      { id: 7, name: 'Аватар 7', url: this.getAvatarUrl(7) },
      { id: 8, name: 'Аватар 8', url: this.getAvatarUrl(8) }
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
