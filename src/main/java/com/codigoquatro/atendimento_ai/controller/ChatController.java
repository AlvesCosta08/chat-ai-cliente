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


import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private InteractionService interactionService;

    // Endpoint para receber a pergunta do usuário
    @PostMapping("/question")
    public ResponseEntity<String> handleQuestion(@RequestBody Map<String, String> request) {
        String question = request.get("question");

        if (question == null || question.trim().isEmpty()) {
            logger.warn("Recebida requisição com pergunta vazia ou nula.");
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"Pergunta não pode estar vazia.\"}");
        }

        try {
            InteractionLog interaction = interactionService.processQuestion(question);

            // Retorna diretamente o HTML gerado pela AI, sem envolver em JSON
            // Isso evita qualquer escape automático do Jackson
            String htmlResponse = interaction.getAnswer();

            logger.debug("Pergunta processada com sucesso. ID da interação: {}", interaction.getId());
            return ResponseEntity.ok(htmlResponse);

        } catch (Exception e) {
            logger.error("Erro inesperado no controller ao processar a pergunta '{}' : ", question, e);
            String fallbackHtml = """
                <p>Desculpe, ocorreu um erro ao processar sua pergunta. Tente novamente ou reformule sua dúvida. Agradecemos sua compreensão.</p>
                """;
            return ResponseEntity.ok(fallbackHtml);
        }
    }
}
