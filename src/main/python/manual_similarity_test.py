"""
Interactive manual check for semantic duplicate filtering.
"""

from __future__ import annotations

from service.question_personalization import (
    CandidateDecision,
    QuestionPersonalizer,
    SentenceTransformerEmbedder,
)


def _read_multiline_block(title: str) -> list[str]:
    print(title)
    print("Введите по одной строке. Пустая строка завершает ввод.")
    lines: list[str] = []
    while True:
        line = input().strip()
        if not line:
            return lines
        lines.append(line)


def _read_player_histories() -> dict[int, list[str]]:
    raw_players_count = input(
        "\nСколько игроков участвует в проверке? "
    ).strip()
    players_count = int(raw_players_count) if raw_players_count else 1

    histories: dict[int, list[str]] = {}
    for player_id in range(1, players_count + 1):
        histories[player_id] = _read_multiline_block(
            f"\nБлок 1.{player_id}. Вопросы из ИСТОРИИ игрока {player_id}:",
        )
    return histories


def _print_decision(decision: CandidateDecision) -> None:
    if decision.reason == "similar_to_player_history":
        source = f"история игроков {decision.conflicting_player_ids}"
    elif decision.reason == "similar_to_quiz_question":
        source = f"текущий квиз: '{decision.conflicting_question}'"
    else:
        source = "N/A"

    print(
        f"[{decision.decision.upper()}] "
        f"score={decision.score:.3f} "
        f"history={decision.max_history_similarity:.3f} "
        f"quiz={decision.max_quiz_similarity:.3f} "
        f"source={source} | {decision.question_text}"
    )


def main() -> None:
    print("===== Ручной тест фильтра QuizAI =====")
    print("Производится проверка")
    print("Истории игроков: вопрос уже знаком кому-то из участников.")
    print("Текущего квиза: вопрос слишком похож на уже зафиксированный вопрос этой викторины.")

    history_by_player = _read_player_histories()
    current_quiz_questions = _read_multiline_block(
        "\nБлок 2. Вопросы, которые уже были ОТОБРАНЫ в текущий квиз ДО этой пачки "
        "(обычно пусто при первой проверке):",
    )
    candidate_questions = _read_multiline_block(
        "\nБлок 3. Новая пачка вопросов-кандидатов от LLM:",
    )

    if not candidate_questions:
        print("Нет кандидатов для проверки.")
        return

    try:
        embedder = SentenceTransformerEmbedder()
    except RuntimeError as exc:
        print("Не удалось загрузить sentence-transformers модель.")
        print(str(exc))
        return

    print("\nСравнение:")
    total_history_questions = sum(len(questions) for questions in history_by_player.values())
    print(f"- Игроков: {len(history_by_player)}")
    print(f"- История игроков: {total_history_questions} шт.")
    print(f"- Уже зафиксировано в текущем квизе до этой пачки: {len(current_quiz_questions)} шт.")
    print(f"- Кандидатов в новой пачке: {len(candidate_questions)} шт.")

    personalizer = QuestionPersonalizer(
        embedder,
        history_similarity_threshold=0.72,
        quiz_similarity_threshold=0.80,
    )

    try:
        player_history = personalizer.encode_history_texts(history_by_player)
        result = personalizer.select_questions(
            [{"question_text": question} for question in candidate_questions],
            player_history,
            target_count=len(candidate_questions),
            existing_quiz_questions=[
                {"question_text": question} for question in current_quiz_questions
            ],
        )
    except RuntimeError as exc:
        print("\nНе удалось обработать введённые вопросы.")
        print(str(exc))
        return

    print("\nВердикты по кандидатам:")
    for decision in result.selected_decisions:
        _print_decision(decision)

    for decision in result.rejected_candidates:
        _print_decision(decision)

    for decision in result.backup_candidates:
        _print_decision(decision)

    print(f"\nНужно догенерировать вопросов: {result.regeneration_needed}")
    print("\nКак читать результат:")
    print("- reason/source = история игроков: кандидат похож на уже знакомый игрокам вопрос.")
    print("- reason/source = текущий квиз: кандидат похож на вопрос, который уже был в квизе или был отобран раньше в этой же пачке.")
    print("- SELECTED: кандидат прошёл оба фильтра.")


if __name__ == "__main__":
    main()
