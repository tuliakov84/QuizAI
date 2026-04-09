"""
QUIZAI PYTHON DB SERVICE
Функциональная библиотека, направленная на взаимодействие с базой данных
Относится только к сервисам персонализации, написанным на языке Python
Не путать с JAVA DB SERVICE
"""

from __future__ import annotations

from typing import Any, Iterable, Sequence

import psycopg2

import config


class DbService:
    def __init__(self) -> None:
        self.conn = psycopg2.connect(
            dbname=config.DB_NAME,
            user=config.DB_USER,
            password=config.DB_PASSWORD,
            host=config.DB_HOST,
            port=config.DB_PORT,
        )
        self.cur = self.conn.cursor()

    def close(self) -> None:
        if getattr(self, "cur", None) is not None:
            self.cur.close()
        if getattr(self, "conn", None) is not None:
            self.conn.close()

    def __enter__(self) -> "DbService":
        return self

    def __exit__(self, exc_type, exc, tb) -> None:
        self.close()

    @staticmethod
    def _vector_to_literal(vector: Sequence[float]) -> str:
        return "[" + ",".join(f"{float(value):.12g}" for value in vector) + "]"

    @staticmethod
    def _parse_vector(value: Any) -> list[float] | None:
        if value is None:
            return None
        if hasattr(value, "tolist"):
            return [float(item) for item in value.tolist()]
        if isinstance(value, (list, tuple)):
            return [float(item) for item in value]
        if isinstance(value, memoryview):
            value = value.tobytes().decode("utf-8")
        if isinstance(value, bytes):
            value = value.decode("utf-8")
        if isinstance(value, str):
            stripped = value.strip().strip("[]")
            if not stripped:
                return []
            return [float(item.strip()) for item in stripped.split(",")]
        raise TypeError(f"Unsupported vector value type: {type(value)!r}")

    def get_question_texts(self, question_ids: Sequence[int]) -> list[tuple[int, str]]:
        if not question_ids:
            return []
        self.cur.execute(
            """
            SELECT id, question_text
            FROM questions
            WHERE id = ANY(%s)
            ORDER BY id
            """,
            (list(question_ids),),
        )
        return self.cur.fetchall()

    def load_embeddings(self, embeddings_dict: dict[int, Sequence[float]]) -> None:
        for db_id, embedding in embeddings_dict.items():
            self.cur.execute(
                """
                UPDATE questions
                SET embedding = %s::vector
                WHERE id = %s
                """,
                (self._vector_to_literal(embedding), db_id),
            )
        self.conn.commit()

    def get_answered_question_ids(self, user_id: int) -> list[int]:
        self.cur.execute(
            """
            SELECT question_id
            FROM answered_correctly_questions
            WHERE user_id = %s
            ORDER BY id
            """,
            (user_id,),
        )
        return [row[0] for row in self.cur.fetchall()]

    def get_current_game_user_ids(self, game_id: int) -> list[int]:
        self.cur.execute(
            """
            SELECT id
            FROM users
            WHERE current_game_id = %s
            ORDER BY id
            """,
            (game_id,),
        )
        return [row[0] for row in self.cur.fetchall()]

    def get_last_answers(self, user_id: int) -> list[tuple[int, list[float]]]:
        self.cur.execute(
            """
            SELECT q.id, q.embedding::text
            FROM questions q
            INNER JOIN answered_correctly_questions acq
                ON q.id = acq.question_id
            WHERE acq.user_id = %s
              AND q.embedding IS NOT NULL
            ORDER BY acq.id
            """,
            (user_id,),
        )
        return [(row[0], self._parse_vector(row[1])) for row in self.cur.fetchall()]

    def find_similar_question_ids(
        self,
        embedding: Sequence[float],
        excluded_ids: Iterable[int] | None = None,
        limit: int = 5,
    ) -> list[int]:
        excluded_ids = list(excluded_ids or [])
        params: list[Any] = []
        query = """
            SELECT id
            FROM questions
            WHERE embedding IS NOT NULL
        """

        if excluded_ids:
            query += " AND NOT (id = ANY(%s))"
            params.append(excluded_ids)

        query += """
            ORDER BY embedding <=> %s::vector
            LIMIT %s
        """
        params.extend([self._vector_to_literal(embedding), limit])
        self.cur.execute(query, tuple(params))
        return [row[0] for row in self.cur.fetchall()]

    def get_question_by_id(self, question_id: int) -> dict[str, Any] | None:
        self.cur.execute(
            """
            SELECT id, game_id, question_number, question_text,
                   answer1, answer2, answer3, answer4, right_answer_number,
                   embedding::text
            FROM questions
            WHERE id = %s
            """,
            (question_id,),
        )
        row = self.cur.fetchone()
        if row is None:
            return None
        return {
            "id": row[0],
            "game_id": row[1],
            "question_number": row[2],
            "question_text": row[3],
            "answer1": row[4],
            "answer2": row[5],
            "answer3": row[6],
            "answer4": row[7],
            "right_answer_number": row[8],
            "embedding": self._parse_vector(row[9]),
        }

    def get_questions_by_ids(self, question_ids: Sequence[int]) -> list[dict[str, Any]]:
        if not question_ids:
            return []
        self.cur.execute(
            """
            SELECT id, game_id, question_number, question_text,
                   answer1, answer2, answer3, answer4, right_answer_number,
                   embedding::text
            FROM questions
            WHERE id = ANY(%s)
            ORDER BY id
            """,
            (list(question_ids),),
        )
        rows = self.cur.fetchall()
        return [
            {
                "id": row[0],
                "game_id": row[1],
                "question_number": row[2],
                "question_text": row[3],
                "answer1": row[4],
                "answer2": row[5],
                "answer3": row[6],
                "answer4": row[7],
                "right_answer_number": row[8],
                "embedding": self._parse_vector(row[9]),
            }
            for row in rows
        ]

    def get_game_questions(self, game_id: int) -> list[dict[str, Any]]:
        self.cur.execute(
            """
            SELECT id, game_id, question_number, question_text,
                   answer1, answer2, answer3, answer4, right_answer_number,
                   embedding::text
            FROM questions
            WHERE game_id = %s
            ORDER BY question_number, id
            """,
            (game_id,),
        )
        rows = self.cur.fetchall()
        return [
            {
                "id": row[0],
                "game_id": row[1],
                "question_number": row[2],
                "question_text": row[3],
                "answer1": row[4],
                "answer2": row[5],
                "answer3": row[6],
                "answer4": row[7],
                "right_answer_number": row[8],
                "embedding": self._parse_vector(row[9]),
            }
            for row in rows
        ]

    def get_game_question_ids(self, game_id: int) -> list[int]:
        self.cur.execute(
            """
            SELECT id
            FROM questions
            WHERE game_id = %s
            ORDER BY question_number, id
            """,
            (game_id,),
        )
        return [row[0] for row in self.cur.fetchall()]

    def update_question(
        self,
        target_question_id: int,
        candidate: dict[str, Any],
        embedding: Sequence[float] | None = None,
    ) -> None:
        params = [
            candidate["question_text"],
            candidate["answer1"],
            candidate["answer2"],
            candidate["answer3"],
            candidate["answer4"],
            candidate["right_answer_number"],
            self._vector_to_literal(embedding) if embedding is not None else None,
            target_question_id,
        ]
        self.cur.execute(
            """
            UPDATE questions
            SET question_text = %s,
                answer1 = %s,
                answer2 = %s,
                answer3 = %s,
                answer4 = %s,
                right_answer_number = %s,
                embedding = %s::vector
            WHERE id = %s
            """,
            tuple(params),
        )
        self.conn.commit()
