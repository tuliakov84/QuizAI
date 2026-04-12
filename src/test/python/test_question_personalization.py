import sys
import unittest
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[3]
SERVICE_ROOT = PROJECT_ROOT / "src" / "main" / "python" / "service"
sys.path.insert(0, str(SERVICE_ROOT))

from question_personalization import QuestionPersonalizer  # noqa: E402


class StubEmbedder:
    def encode(self, texts):
        return [[float(index + 1), 1.0] for index, _ in enumerate(texts)]


class QuestionPersonalizationTest(unittest.TestCase):
    def test_rejects_questions_with_chinese_symbols_for_regeneration(self):
        personalizer = QuestionPersonalizer(StubEmbedder())
        candidates = [
            {
                "id": 101,
                "question_text": "Какой город обозначается 北京?",
                "available_answers": [
                    {"index": 1, "answer": "Пекин"},
                    {"index": 2, "answer": "Шанхай"},
                    {"index": 3, "answer": "Нанкин"},
                    {"index": 4, "answer": "Тяньцзинь"},
                ],
                "right_answer_number": 1,
            },
            {
                "id": 102,
                "question_text": "Какая столица у Японии?",
                "available_answers": [
                    {"index": 1, "answer": "Токио"},
                    {"index": 2, "answer": "Осака"},
                    {"index": 3, "answer": "Киото"},
                    {"index": 4, "answer": "Саппоро"},
                ],
                "right_answer_number": 1,
            },
        ]

        result = personalizer.select_questions(candidates, target_count=2)

        self.assertEqual(1, len(result.selected_questions))
        self.assertEqual(1, result.regeneration_needed)
        self.assertEqual(1, len(result.rejected_candidates))
        self.assertEqual("contains_chinese_text", result.rejected_candidates[0].reason)
        self.assertEqual(101, result.rejected_candidates[0].source_question_id)
        self.assertEqual("Какая столица у Японии?", result.selected_questions[0]["question_text"])


if __name__ == "__main__":
    unittest.main()
