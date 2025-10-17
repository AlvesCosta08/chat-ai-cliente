package com.codigoquatro.atendimento_ai.e2e;

import com.codigoquatro.atendimento_ai.model.InteractionLog;
import com.codigoquatro.atendimento_ai.repository.InteractionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate; // Import correto
import org.springframework.boot.test.web.server.LocalServerPort; // Import correto para obter a porta
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

// Importante: Use RANDOM_PORT para testes E2E que simulam uma chamada HTTP real
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatEndpointE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InteractionLogRepository interactionLogRepository;


   /* @Test
    void shouldReturnAnswerAndSaveInteractionWhenQuestionIsProvided() {
        // Arrange
        String question = "como redefinir minha senha?";
        String expectedAnswer = "Você pode redefinir sua senha acessando a página de login e clicando em 'Esqueci minha senha'. Siga as instruções enviadas ao seu e-mail cadastrado.";
        // Monta a URL completa com a porta
        String url = "http://localhost:" + port + "/api/chat/question";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String jsonRequest = "{\"question\": \"" + question + "\"}";
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonRequest, headers);

        // Act & Assert
        ResponseEntity<String> response = restTemplate.exchange(
                url, // Usa a URL completa
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"answer\":\"" + expectedAnswer + "\"");
        assertThat(response.getBody()).contains("\"interactionId\"");

        // Verifica se a interação foi salva no banco de dados
        InteractionLog savedInteraction = interactionLogRepository.findAll().stream()
                .filter(log -> question.equals(log.getQuestion()))
                .findFirst()
                .orElse(null);

        assertThat(savedInteraction).isNotNull();
        assertThat(savedInteraction.getQuestion()).isEqualTo(question);
        assertThat(savedInteraction.getAnswer()).isEqualTo(expectedAnswer);
    }*/

    @Test
    void shouldReturnBadRequestWhenQuestionIsEmpty() {
        // Arrange
        String url = "http://localhost:" + port + "/api/chat/question";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String jsonRequest = "{\"question\": \"\"}";
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonRequest, headers);

        // Act & Assert
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("\"error\":\"Pergunta não pode estar vazia.\"");
    }

    @Test
    void shouldReturnBadRequestWhenQuestionIsNull() {
        // Arrange
        String url = "http://localhost:" + port + "/api/chat/question";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String jsonRequest = "{}"; // 'question' ausente
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonRequest, headers);

        // Act & Assert
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("\"error\":\"Pergunta não pode estar vazia.\"");
    }
}