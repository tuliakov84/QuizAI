package com.mipt.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QuestionPayloadFinalNormalizer {

  private static final Pattern WORD_PATTERN = Pattern.compile("\\p{L}+");
  private static final List<Map.Entry<String, String>> MULTI_CHAR_REPLACEMENTS = List.of(
      Map.entry("shch", "щ"),
      Map.entry("yo", "ё"),
      Map.entry("zh", "ж"),
      Map.entry("kh", "х"),
      Map.entry("ts", "ц"),
      Map.entry("ch", "ч"),
      Map.entry("sh", "ш"),
      Map.entry("yu", "ю"),
      Map.entry("ya", "я"),
      Map.entry("ye", "е"),
      Map.entry("jo", "ё"),
      Map.entry("ph", "ф"),
      Map.entry("qu", "кв"),
      Map.entry("ck", "к")
  );
  private static final Map<Character, String> SINGLE_CHAR_REPLACEMENTS = Map.ofEntries(
      Map.entry('a', "а"),
      Map.entry('b', "б"),
      Map.entry('d', "д"),
      Map.entry('e', "е"),
      Map.entry('f', "ф"),
      Map.entry('g', "г"),
      Map.entry('h', "х"),
      Map.entry('i', "и"),
      Map.entry('j', "дж"),
      Map.entry('k', "к"),
      Map.entry('l', "л"),
      Map.entry('m', "м"),
      Map.entry('n', "н"),
      Map.entry('o', "о"),
      Map.entry('p', "п"),
      Map.entry('q', "к"),
      Map.entry('r', "р"),
      Map.entry('s', "с"),
      Map.entry('t', "т"),
      Map.entry('u', "у"),
      Map.entry('v', "в"),
      Map.entry('w', "в"),
      Map.entry('x', "кс"),
      Map.entry('y', "й"),
      Map.entry('z', "з")
  );

  private QuestionPayloadFinalNormalizer() {
  }

  public static JSONArray normalizeQuestions(JSONArray questions) {
    JSONArray normalizedQuestions = new JSONArray();
    for (int i = 0; i < questions.length(); i++) {
      normalizedQuestions.put(normalizeQuestion(questions.getJSONObject(i)));
    }
    return normalizedQuestions;
  }

  public static JSONObject normalizeQuestion(JSONObject question) {
    JSONObject normalized = new JSONObject(question.toString());

    if (normalized.has("question_text")) {
      normalized.put("question_text", normalizeText(normalized.optString("question_text", "")));
    }

    JSONArray availableAnswers = normalized.optJSONArray("available_answers");
    if (availableAnswers == null) {
      return normalized;
    }

    for (int i = 0; i < availableAnswers.length(); i++) {
      JSONObject answer = availableAnswers.optJSONObject(i);
      if (answer == null || !answer.has("answer")) {
        continue;
      }
      answer.put("answer", normalizeText(answer.optString("answer", "")));
    }

    return normalized;
  }

  public static boolean containsChineseText(JSONObject question) {
    if (containsChineseCharacters(question.optString("question_text", ""))) {
      return true;
    }

    JSONArray availableAnswers = question.optJSONArray("available_answers");
    if (availableAnswers == null) {
      return false;
    }

    for (int i = 0; i < availableAnswers.length(); i++) {
      JSONObject answer = availableAnswers.optJSONObject(i);
      if (answer != null && containsChineseCharacters(answer.optString("answer", ""))) {
        return true;
      }
    }

    return false;
  }

  public static String normalizeText(String text) {
    if (text == null || text.isBlank()) {
      return text;
    }

    Matcher matcher = WORD_PATTERN.matcher(text);
    StringBuffer normalized = new StringBuffer();
    while (matcher.find()) {
      String word = matcher.group();
      String replacement = containsLatinAndCyrillic(word) ? transliterateMixedWord(word) : word;
      matcher.appendReplacement(normalized, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(normalized);
    return normalized.toString();
  }

  private static boolean containsLatinAndCyrillic(String word) {
    boolean hasLatin = false;
    boolean hasCyrillic = false;
    for (int i = 0; i < word.length(); i++) {
      Character.UnicodeScript script = Character.UnicodeScript.of(word.charAt(i));
      if (script == Character.UnicodeScript.LATIN) {
        hasLatin = true;
      } else if (script == Character.UnicodeScript.CYRILLIC) {
        hasCyrillic = true;
      }
      if (hasLatin && hasCyrillic) {
        return true;
      }
    }
    return false;
  }

  private static String transliterateMixedWord(String word) {
    StringBuilder normalized = new StringBuilder();
    StringBuilder latinSegment = new StringBuilder();

    for (int i = 0; i < word.length(); i++) {
      char current = word.charAt(i);
      if (Character.UnicodeScript.of(current) == Character.UnicodeScript.LATIN) {
        latinSegment.append(current);
        continue;
      }

      flushLatinSegment(normalized, latinSegment);
      normalized.append(current);
    }

    flushLatinSegment(normalized, latinSegment);
    return normalized.toString();
  }

  private static void flushLatinSegment(StringBuilder target, StringBuilder latinSegment) {
    if (latinSegment.isEmpty()) {
      return;
    }
    target.append(transliterateLatinSegment(latinSegment.toString()));
    latinSegment.setLength(0);
  }

  private static String transliterateLatinSegment(String segment) {
    boolean allUpper = segment.equals(segment.toUpperCase(Locale.ROOT));
    boolean capitalized = Character.isUpperCase(segment.charAt(0))
        && segment.substring(1).equals(segment.substring(1).toLowerCase(Locale.ROOT));
    String lowerCased = segment.toLowerCase(Locale.ROOT);
    StringBuilder transliterated = new StringBuilder();

    int index = 0;
    while (index < lowerCased.length()) {
      Map.Entry<String, String> multiCharReplacement = matchMultiCharReplacement(lowerCased, index);
      if (multiCharReplacement != null) {
        transliterated.append(multiCharReplacement.getValue());
        index += multiCharReplacement.getKey().length();
        continue;
      }

      char current = lowerCased.charAt(index);
      transliterated.append(transliterateLatinCharacter(current, lowerCased, index));
      index++;
    }

    if (allUpper) {
      return transliterated.toString().toUpperCase(Locale.ROOT);
    }
    if (capitalized && transliterated.length() > 0) {
      return Character.toUpperCase(transliterated.charAt(0)) + transliterated.substring(1);
    }
    return transliterated.toString();
  }

  private static Map.Entry<String, String> matchMultiCharReplacement(String segment, int startIndex) {
    for (Map.Entry<String, String> replacement : MULTI_CHAR_REPLACEMENTS) {
      if (segment.startsWith(replacement.getKey(), startIndex)) {
        return replacement;
      }
    }
    return null;
  }

  private static String transliterateLatinCharacter(char current, String segment, int index) {
    if (current == 'c') {
      if (index + 1 < segment.length()) {
        char next = segment.charAt(index + 1);
        if (next == 'e' || next == 'i' || next == 'y') {
          return "с";
        }
      }
      return "к";
    }

    return SINGLE_CHAR_REPLACEMENTS.getOrDefault(current, String.valueOf(current));
  }

  private static boolean containsChineseCharacters(String text) {
    for (int i = 0; i < text.length(); i++) {
      Character.UnicodeScript script = Character.UnicodeScript.of(text.charAt(i));
      if (script == Character.UnicodeScript.HAN) {
        return true;
      }
    }
    return false;
  }
}
