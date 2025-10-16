package com.codigoquatro.atendimento_ai.service;

import com.codigoquatro.atendimento_ai.ai.AiService;
import com.codigoquatro.atendimento_ai.model.InteractionLog;
import com.codigoquatro.atendimento_ai.repository.InteractionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InteractionService {

    @Autowired
    private InteractionLogRepository interactionLogRepository;

    @Autowired
    private AiService aiService;

    // Método para salvar uma interação inicial (sem resposta) - Mantido, mas talvez não mais usado
    public InteractionLog saveInitialInteraction(String question) {
        InteractionLog log = new InteractionLog(question, null);
        return interactionLogRepository.save(log); // Chamada 1 a save()
    }

    // Novo método: Processa a pergunta e salva a interação completa com a resposta
    public InteractionLog processQuestion(String question) {
        String answer = aiService.getAnswerForQuestion(question);
        InteractionLog log = new InteractionLog(question, answer); // Cria com resposta já definida
        return interactionLogRepository.save(log); // Chamada 2 a save() - Agora é a ÚNICA chamada em processQuestion
    }

    // Método para buscar uma interação pelo ID
    public InteractionLog findById(Long id) {
        return interactionLogRepository.findById(id).orElse(null);
    }
}
