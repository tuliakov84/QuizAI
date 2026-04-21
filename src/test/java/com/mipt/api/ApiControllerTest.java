package com.mipt.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.Achievement;
import com.mipt.domainModel.AnswerObject;
import com.mipt.domainModel.Question;
import com.mipt.domainModel.Topic;
import com.mipt.service.AvatarStorageService;
import com.mipt.service.QuestionLoadingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiControllerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private DbService dbService;
  private MockMvc mockMvc;

  private static String malformedJson() {
    return "{";
  }

  @BeforeEach
  void setUp() throws Exception {
    dbService = mock(DbService.class);
    QuestionLoadingService questionLoadingService = mock(QuestionLoadingService.class);
    AvatarStorageService avatarStorageService = mock(AvatarStorageService.class);

    when(dbService.getAllTopics()).thenReturn(new Topic[0]);
    when(dbService.getAllAchievements()).thenReturn(new Achievement[0]);

    ApiController controller = new ApiController(dbService, questionLoadingService, avatarStorageService);
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setMessageConverters(new MappingJackson2HttpMessageConverter())
        .build();

    clearInvocations(dbService, questionLoadingService);
  }

  @Test
  void verifyAnswer_returnsBadRequestWhenTimeTakenIsMissing() throws Exception {
    AnswerObject answerObject = new AnswerObject();
    answerObject.setGameId(10);
    answerObject.setQuestionNumber(1);
    answerObject.setSubmittedAnswerNumber(2);
    answerObject.setSession("session-1");

    mockMvc.perform(post("/api/game/verify-answer")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(answerObject)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("\"Field 'timeTakenToAnswerInSeconds' must not be null\""));

    verifyNoInteractions(dbService);
  }

  @Test
  void verifyAnswer_returnsConflictWhenGameIsNotActive() throws Exception {
    AnswerObject answerObject = new AnswerObject();
    answerObject.setGameId(10);
    answerObject.setQuestionNumber(1);
    answerObject.setSubmittedAnswerNumber(2);
    answerObject.setTimeTakenToAnswerInSeconds(12);
    answerObject.setSession("session-1");

    when(dbService.getRightAnswer(10, 1)).thenThrow(new DatabaseAccessException("Game is not active"));

    mockMvc.perform(post("/api/game/verify-answer")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(answerObject)))
        .andExpect(status().isConflict())
        .andExpect(content().string("\"Game is not active\""));

    verify(dbService).getRightAnswer(10, 1);
    verify(dbService, never()).getPreset(anyInt());
    verify(dbService, never()).addCurrentGamePoints(anyString(), anyInt());
    verify(dbService, never()).addGlobalPoints(anyString(), anyInt());
    verify(dbService, never()).addCorrectAnswer(anyString(), anyInt());
    verify(dbService, never()).addGlobalPossiblePoints(anyString(), anyInt());
  }

  @Test
  void verifyAnswer_returnsBadRequestForMalformedJson() throws Exception {
    mockMvc.perform(post("/api/game/verify-answer")
            .contentType(MediaType.APPLICATION_JSON)
            .content(malformedJson()))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("\"Malformed request body\""));

    verifyNoInteractions(dbService);
  }

  @Test
  void verifyAnswer_processesCorrectAnswer() throws Exception {
    AnswerObject answerObject = new AnswerObject();
    answerObject.setGameId(10);
    answerObject.setQuestionNumber(1);
    answerObject.setSubmittedAnswerNumber(2);
    answerObject.setTimeTakenToAnswerInSeconds(12);
    answerObject.setSession("session-1");

    Question question = new Question();
    question.setQuestionId(99);

    when(dbService.getRightAnswer(10, 1)).thenReturn(2);
    when(dbService.getPreset(10)).thenReturn(new Integer[]{null, 1, null, null, null});
    when(dbService.getQuestion(10, 1)).thenReturn(question);

    mockMvc.perform(post("/api/game/verify-answer")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(answerObject)))
        .andExpect(status().isOk());

    verify(dbService).getRightAnswer(10, 1);
    verify(dbService).getPreset(10);
    verify(dbService).getQuestion(10, 1);
    verify(dbService).addCurrentGamePoints(eq("session-1"), anyInt());
    verify(dbService).addGlobalPoints(eq("session-1"), anyInt());
    verify(dbService).addCorrectAnswer("session-1", 99);
    verify(dbService).addGlobalPossiblePoints(eq("session-1"), anyInt());
  }

  @Test
  void getAllGameModes_returnsCatalog() throws Exception {
    mockMvc.perform(get("/api/game-mode/get-all"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("CASUAL")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("DUEL")));
  }
}
