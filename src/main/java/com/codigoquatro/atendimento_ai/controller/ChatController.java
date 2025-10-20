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
    public ResponseEntity<String> handleQuestion(@RequestBody Map<String, String> request) { // Muda o tipo de retorno
        String question = request.get("question");

        if (question == null || question.trim().isEmpty()) {
            String errorResponse = "{\"error\": \"Pergunta não pode estar vazia.\"}"; // JSON como string
            logger.warn("Recebida requisição com pergunta vazia ou nula.");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            // Chama o serviço para processar a pergunta e obter a interação completa
            InteractionLog interaction = interactionService.processQuestion(question);

            // Retorna o HTML puro diretamente como string, sem envolver um objeto Java
            String successResponse = "{\"answer\": \"" + escapeJsonString(interaction.getAnswer()) + "\"}";

            logger.debug("Pergunta processada com sucesso. ID da interação: {}", interaction.getId());
            return ResponseEntity.ok(successResponse);

        } catch (Exception e) {
            logger.error("Erro inesperado no controller ao processar a pergunta '{}' : ", question, e);
            String errorResponse = "{\"answer\": \"Desculpe, ocorreu um erro ao processar sua pergunta. Tente novamente ou reformule sua dúvida. Agradecemos sua compreensão.\"}";
            return ResponseEntity.ok(errorResponse); // Retorna 200 OK com a mensagem de erro
        }
    }

    // Função auxiliar simples para escapar aspas e barras invertidas em JSON
    // Isso é necessário porque o conteúdo de 'answer' pode conter aspas e quebras de linha
    private String escapeJsonString(String input) {
        if (input == null) return "null";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
