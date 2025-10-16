package com.codigoquatro.atendimento_ai.service;

import com.codigoquatro.atendimento_ai.ai.AiService;
import com.codigoquatro.atendimento_ai.model.InteractionLog;
import com.codigoquatro.atendimento_ai.repository.InteractionLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InteractionServiceTest {

    @Mock
    private InteractionLogRepository repository;

    @Mock
    private AiService aiService; // Mockamos o serviço de IA

    @InjectMocks
    private InteractionService interactionService;

    @Test
    void shouldProcessQuestionAndReturnInteractionWithAnswer() {
        // Arrange
        String question = "Como redefinir minha senha?";
        String expectedAnswer = "Você pode redefinir sua senha acessando a página de login e clicando em 'Esqueci minha senha'.";
        // O objeto salvo deve ter a resposta preenchida
        InteractionLog interactionToBeSaved = new InteractionLog(question, expectedAnswer);
        InteractionLog savedLog = new InteractionLog(question, expectedAnswer);
        savedLog.setId(1L); // Simula que o ID foi gerado pelo banco

        // Simula o save retornando o objeto com ID
        when(repository.save(any(InteractionLog.class))).thenReturn(savedLog);
        when(aiService.getAnswerForQuestion(question)).thenReturn(expectedAnswer);

        // Act
        InteractionLog result = interactionService.processQuestion(question);

        // Assert
        assertNotNull(result);
        assertEquals(question, result.getQuestion());
        assertEquals(expectedAnswer, result.getAnswer());
        // Verifica que save foi chamado 1 vez com qualquer InteractionLog
        verify(repository, times(1)).save(any(InteractionLog.class));
        // Verifica que o save foi chamado com um objeto que tem a resposta correta
        verify(repository).save(argThat(log -> expectedAnswer.equals(log.getAnswer())));
        verify(aiService, times(1)).getAnswerForQuestion(question);
    }

    @Test
    void shouldReturnInteractionById() {
        // Arrange
        Long id = 1L;
        InteractionLog expectedLog = new InteractionLog("Pergunta?", "Resposta.");
        expectedLog.setId(id);

        when(repository.findById(id)).thenReturn(java.util.Optional.of(expectedLog));

        // Act
        InteractionLog result = interactionService.findById(id);

        // Assert
        assertNotNull(result);
        assertEquals(expectedLog, result);
        verify(repository, times(1)).findById(id);
    }
}