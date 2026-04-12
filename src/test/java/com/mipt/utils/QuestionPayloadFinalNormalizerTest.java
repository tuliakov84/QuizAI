package com.mipt.utils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionPayloadFinalNormalizerTest {

  @Test
  void normalizeQuestionTransliteratesMixedScriptWords() throws Exception {
    JSONObject normalized = QuestionPayloadFinalNormalizer.normalizeQuestion(new JSONObject("""
        {
          "question_number": 1,
          "question_text": "Как называется элемент оганеsson?",
          "available_answers": [
            {"index": 1, "answer": "Оганеsson"},
            {"index": 2, "answer": "Гелий"},
            {"index": 3, "answer": "Неон"},
            {"index": 4, "answer": "Аргон"}
          ],
          "right_answer_number": 1
        }
        """));

    assertEquals("Как называется элемент оганессон?", normalized.getString("question_text"));
    assertEquals(
        "Оганессон",
        normalized.getJSONArray("available_answers").getJSONObject(0).getString("answer")
    );
  }

  @Test
  void containsChineseTextDetectsQuestionAndAnswers() throws Exception {
    JSONObject questionWithChinese = new JSONObject()
        .put("question_text", "Столица 中国?")
        .put("available_answers", new JSONArray()
            .put(new JSONObject().put("index", 1).put("answer", "Пекин"))
            .put(new JSONObject().put("index", 2).put("answer", "Шанхай"))
            .put(new JSONObject().put("index", 3).put("answer", "Гуанчжоу"))
            .put(new JSONObject().put("index", 4).put("answer", "Шэньчжэнь")));
    JSONObject answerWithChinese = new JSONObject()
        .put("question_text", "Какая столица у Китая?")
        .put("available_answers", new JSONArray()
            .put(new JSONObject().put("index", 1).put("answer", "北京"))
            .put(new JSONObject().put("index", 2).put("answer", "Шанхай"))
            .put(new JSONObject().put("index", 3).put("answer", "Гуанчжоу"))
            .put(new JSONObject().put("index", 4).put("answer", "Шэньчжэнь")));
    JSONObject cleanQuestion = new JSONObject()
        .put("question_text", "Какая столица у Китая?")
        .put("available_answers", new JSONArray()
            .put(new JSONObject().put("index", 1).put("answer", "Пекин"))
            .put(new JSONObject().put("index", 2).put("answer", "Шанхай"))
            .put(new JSONObject().put("index", 3).put("answer", "Гуанчжоу"))
            .put(new JSONObject().put("index", 4).put("answer", "Шэньчжэнь")));

    assertTrue(QuestionPayloadFinalNormalizer.containsChineseText(questionWithChinese));
    assertTrue(QuestionPayloadFinalNormalizer.containsChineseText(answerWithChinese));
    assertFalse(QuestionPayloadFinalNormalizer.containsChineseText(cleanQuestion));
  }
}
