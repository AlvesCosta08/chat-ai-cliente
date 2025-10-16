package com.codigoquatro.atendimento_ai.controller;

import com.codigoquatro.atendimento_ai.model.InteractionLog;
import com.codigoquatro.atendimento_ai.service.InteractionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InteractionService interactionService;

    @Test
    void shouldReturnAnswerWhenQuestionIsProvided() throws Exception {
        // Arrange
        String question = "Como redefinir minha senha?";
        String answer = "Você pode redefinir sua senha acessando a página de login e clicando em 'Esqueci minha senha'.";
        InteractionLog processedLog = new InteractionLog(question, answer);
        processedLog.setId(1L); // Simula que o ID foi gerado pelo banco

        // Simula o método processQuestion do serviço
        when(interactionService.processQuestion(question)).thenReturn(processedLog);

        String jsonRequest = "{ \"question\": \"" + question + "\" }";

        // Act & Assert
        mockMvc.perform(post("/api/chat/question")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.answer").value(answer))
                .andExpect(jsonPath("$.interactionId").value("1"));

        // Verifica se o método do serviço foi chamado corretamente
        verify(interactionService, times(1)).processQuestion(question);
    }

    @Test
    void shouldReturnBadRequestWhenQuestionIsEmpty() throws Exception {
        // Arrange
        String jsonRequest = "{ \"question\": \"\" }";

        // Act & Assert
        mockMvc.perform(post("/api/chat/question")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Pergunta não pode estar vazia."));

        // Verifica que o serviço NÃO foi chamado
        verify(interactionService, never()).processQuestion(anyString());
    }

    @Test
    void shouldReturnBadRequestWhenQuestionIsNull() throws Exception {
        // Arrange
        String jsonRequest = "{ \"other_field\": \"value\" }"; // 'question' ausente

        // Act & Assert
        mockMvc.perform(post("/api/chat/question")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Pergunta não pode estar vazia."));

        // Verifica que o serviço NÃO foi chamado
        verify(interactionService, never()).processQuestion(anyString());
    }
}