[file name]: lobby-avatar.js
class LobbyAvatar {
  // Отображение аватара пользователя в лобби
  static displayAvatar(element, picId) {
    if (!element || !picId) return;

    const avatarUrl = AuthService.getAvatarUrl(picId);
    element.style.backgroundImage = `url('${avatarUrl}')`;
    element.style.backgroundSize = 'cover';
    element.style.backgroundPosition = 'center';
    element.style.backgroundColor = 'transparent';
    element.classList.add('has-avatar');
  }

  // Создание элемента аватара
  static createAvatarElement(picId, username) {
    const avatarDiv = document.createElement('div');
    avatarDiv.className = 'player-avatar';

    if (picId) {
      this.displayAvatar(avatarDiv, picId);
    } else {
      avatarDiv.textContent = username.charAt(0).toUpperCase();
      avatarDiv.style.backgroundColor = '#4a90e2';
      avatarDiv.style.display = 'flex';
      avatarDiv.style.alignItems = 'center';
      avatarDiv.style.justifyContent = 'center';
      avatarDiv.style.color = 'white';
      avatarDiv.style.fontWeight = 'bold';
    }

    return avatarDiv;
  }
}