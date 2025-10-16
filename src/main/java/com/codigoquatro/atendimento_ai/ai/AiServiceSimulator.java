package com.codigoquatro.atendimento_ai.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service // Marca como um componente Spring
public class AiServiceSimulator implements AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiServiceSimulator.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<KnowledgeEntry> knowledgeBase = List.of(); // Inicializa com lista vazia

    // Método chamado após a injeção de dependências e antes de o bean estar ativo
    @PostConstruct
    public void loadKnowledgeBase() {
        try {
            ClassPathResource resource = new ClassPathResource("knowledge_base.json");
            try (InputStream inputStream = resource.getInputStream()) {
                // Lê o JSON como uma lista de KnowledgeEntry
                this.knowledgeBase = objectMapper.readValue(inputStream, new TypeReference<List<KnowledgeEntry>>() {});
                logger.info("Base de conhecimento carregada com sucesso. {} entradas.", knowledgeBase.size());
            }
        } catch (IOException e) {
            logger.error("Erro ao carregar a base de conhecimento de 'knowledge_base.json'. Usando base vazia.", e);
            this.knowledgeBase = List.of(); // Garante que não seja nulo
        }
    }

    @Override
    public String getAnswerForQuestion(String question) {
        if (question == null || knowledgeBase.isEmpty()) {
            return "Desculpe, não consegui encontrar uma resposta para sua pergunta. Por favor, entre em contato com nosso suporte.";
        }

        String lowerQuestion = question.toLowerCase().trim();

        // Procura por uma correspondência exata ou parcial na pergunta
        for (KnowledgeEntry entry : knowledgeBase) {
            if (lowerQuestion.contains(entry.getQuestion().toLowerCase())) {
                return entry.getAnswer();
            }
        }

        // Se não encontrar, retorna uma resposta padrão
        return "Desculpe, não consegui encontrar uma resposta para sua pergunta. Por favor, entre em contato com nosso suporte.";
    }
}
