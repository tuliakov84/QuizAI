Инструкция по установке FastAPI, Pydantic, Requests через pip и Ollama через Homebrew (Linux Ubuntu Server Edition)

Системные требования
    Ubuntu Server 20.04 / 22.04 / 24.04 LTS
	Доступ к интернету
	Права суперпользователя (sudo)

Установка Homebrew
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

Добавление переменных окружения:
    echo 'eval "$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)"' >> ~/.bashrc
    eval "$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)"

Проверка:
    brew --version

Установка Ollama через Homebrew
    brew install ollama

Запуск службы:
    sudo systemctl enable ollama
    sudo systemctl start ollama

Проверка работы:
    ollama --version
    ollama list

Загрузка модели:
    ollama pull qwen2.5


Инструкция по установке FastAPI, Pydantic, Requests через pip и Ollama через Homebrew (Windows 11)

Системные требования
	Windows 11 (64-bit) 21H2 / 22H2 / 23H2 / новее
	PowerShell 7+ (желательно)
	Доступ к интернету


Установка Homebrew
    Установка Git Bash
        winget install Git.Git
    Установка Homebrew
        В Git Bash:
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    Добавить в PATH (Git Bash):
        echo 'eval "$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)"' >> ~/.bashrc
        eval "$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)"
    Проверка:
        brew --version

Установка Ollama через Homebrew
    brew install ollama

Запуск Ollama вручную:
    ollama serve

Проверка:
    ollama --version

Загрузка модели:
    ollama pull llama3.1
