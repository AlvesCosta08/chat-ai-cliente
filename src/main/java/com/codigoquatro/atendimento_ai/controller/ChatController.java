package com.codigoquatro.atendimento_ai.controller;

import com.codigoquatro.atendimento_ai.model.InteractionLog;
import com.codigoquatro.atendimento_ai.service.InteractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private InteractionService interactionService;

    // Endpoint para receber a pergunta do usuário
    @PostMapping("/question")
    public ResponseEntity<Map<String, String>> handleQuestion(@RequestBody Map<String, String> request) {
        String question = request.get("question");

        if (question == null || question.trim().isEmpty()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Pergunta não pode estar vazia.");
            logger.warn("Recebida requisição com pergunta vazia ou nula.");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            // Chama o serviço para processar a pergunta e obter a interação completa
            // O interactionService.processQuestion encapsula o tratamento de erro da AI
            InteractionLog interaction = interactionService.processQuestion(question);

            // Preparar a resposta de sucesso para o cliente
            Map<String, String> response = new HashMap<>();
            response.put("answer", interaction.getAnswer());
            response.put("interactionId", String.valueOf(interaction.getId()));

            logger.debug("Pergunta processada com sucesso. ID da interação: {}", interaction.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Captura qualquer exceção lançada pelo service
            logger.error("Erro inesperado no controller ao processar a pergunta '{}' : ", question, e);

            // Preparar uma resposta de erro amigável para o cliente
            Map<String, String> errorResponse = new HashMap<>();
            // Esta é a mensagem que será retornada em caso de falha no service
            errorResponse.put("answer", "Desculpe, ocorreu um erro ao processar sua pergunta. Tente novamente ou reformule sua dúvida. Agradecemos sua compreensão.");

            // Retorna 200 OK com a mensagem de erro dentro do corpo esperado pelo cliente
            return ResponseEntity.ok(errorResponse);

            // OU, alternativamente, retornar 500 Internal Server Error
            // return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
