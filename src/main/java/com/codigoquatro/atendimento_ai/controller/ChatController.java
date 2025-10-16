package com.codigoquatro.atendimento_ai.controller;

import com.codigoquatro.atendimento_ai.model.InteractionLog;
import com.codigoquatro.atendimento_ai.service.InteractionService;
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

    @Autowired
    private InteractionService interactionService;

    // Endpoint para receber a pergunta do usuário
    @PostMapping("/question")
    public ResponseEntity<Map<String, String>> handleQuestion(@RequestBody Map<String, String> request) {
        String question = request.get("question");

        if (question == null || question.trim().isEmpty()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Pergunta não pode estar vazia.");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // Chama o serviço para processar a pergunta e obter a interação completa
        InteractionLog interaction = interactionService.processQuestion(question);

        // Preparar a resposta para o cliente
        Map<String, String> response = new HashMap<>();
        response.put("answer", interaction.getAnswer());
        response.put("interactionId", String.valueOf(interaction.getId()));

        return ResponseEntity.ok(response);
    }
}
