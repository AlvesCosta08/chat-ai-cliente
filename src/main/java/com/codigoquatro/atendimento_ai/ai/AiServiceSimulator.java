package com.codigoquatro.atendimento_ai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiServiceSimulator implements AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiServiceSimulator.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpClient httpClient;
    private String apiKey;
    private String model;
    private List<KnowledgeEntry> knowledgeBase = List.of();

    public AiServiceSimulator(@Value("${openrouter.api.key}") String apiKey,
                              @Value("${openrouter.model:openai/gpt-3.5-turbo}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @PostConstruct
    public void init() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        loadKnowledgeBase();
    }

    private void loadKnowledgeBase() {
        try {
            ClassPathResource resource = new ClassPathResource("knowledge_base.json");
            try (InputStream inputStream = resource.getInputStream()) {
                this.knowledgeBase = objectMapper.readValue(inputStream, objectMapper.getTypeFactory().constructCollectionType(List.class, KnowledgeEntry.class));
                logger.info("Base de conhecimento carregada com sucesso. {} entradas.", knowledgeBase.size());
            }
        } catch (Exception e) {
            logger.error("Erro ao carregar a base de conhecimento. Usando base vazia.", e);
        }
    }

    @Override
    public String getAnswerForQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            return "A pergunta não pode estar vazia.";
        }

        // 🔍 1. Busca respostas relevantes na base local
        List<KnowledgeEntry> relevantEntries = findRelevantEntries(question);

        // 🧠 2. Monta o contexto para a IA (apenas se houver entradas relevantes)
        String context = buildContext(relevantEntries);

        // 🤖 3. Monta o prompt dinamicamente
        String prompt = buildPrompt(question, context);

        // 🌐 4. Chama a OpenRouter com o prompt construído
        return callOpenRouter(prompt);
    }

    // 🔑 Busca por entradas relevantes (mesma lógica anterior)
    private List<KnowledgeEntry> findRelevantEntries(String question) {
        String lowerQuestion = question.toLowerCase().trim();

        return knowledgeBase.stream()
                .filter(entry -> containsAny(lowerQuestion, entry.getQuestion().toLowerCase()) ||
                        containsAny(entry.getQuestion().toLowerCase(), lowerQuestion))
                .limit(3)
                .collect(Collectors.toList());
    }

    // 🔧 Verifica se alguma palavra está contida (mesma lógica anterior)
    private boolean containsAny(String text, String target) {
        String[] words = target.split("\\s+");
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    // 🧩 Monta o contexto *apenas* se houver entradas relevantes
    private String buildContext(List<KnowledgeEntry> entries) {
        if (entries.isEmpty()) {
            // Retorna uma string vazia ou uma indicação de que não há informações locais
            return "";
            // Ou, para ser explícito: return "Nenhuma informação específica encontrada na base de conhecimento local.";
        }

        StringBuilder sb = new StringBuilder("Base de Conhecimento:\n");
        for (KnowledgeEntry entry : entries) {
            sb.append("- Pergunta: ").append(entry.getQuestion()).append("\n");
            sb.append("  Resposta: ").append(entry.getAnswer()).append("\n\n");
        }
        return sb.toString();
    }

    // 🧠 Monta o prompt *dinamicamente*, incluindo ou não o contexto
    private String buildPrompt(String question, String context) {
        // Se houver contexto, incluímos ele no prompt para RAG
        if (!context.isEmpty()) {
            return """
                Você é um assistente de suporte ao cliente especializado em e-commerce da CodeChat.
                Use a base de conhecimento abaixo para responder à pergunta do usuário, se aplicável.
                Se a base não tiver a resposta, responda com base em seu conhecimento geral, mantendo o tom profissional e útil.
                Base de Conhecimento:
                %s
                Pergunta do Usuário:
                %s
                Resposta:
                """.formatted(context, question);
        } else {
            // Se não houver contexto, pedimos para a IA responder com base em seu conhecimento geral
            return """
                Você é um assistente de suporte ao cliente especializado em e-commerce da CodeChat.
                A base de conhecimento local não tinha informações específicas para esta pergunta.
                Por favor, responda à pergunta do usuário com base em seu conhecimento geral, mantendo o tom profissional, útil e humanizado.
                Pergunta do Usuário:
                %s
                Resposta:
                """.formatted(question);
        }
    }

    // 🌐 Faz a chamada à API da OpenRouter (mesma lógica anterior, exceto por possíveis erros de digitação)
    private String callOpenRouter(String prompt) {
        // Corrigido: removido espaço extra no final da URL e no header HTTP-Referer
        String url = "https://openrouter.ai/api/v1/chat/completions"; // <= URL Correta
        String referer = "https://chat-ai-cliente.onrender.com:8080"; // <= Referer Correto

        String requestBody = """
            {
              "model": "%s",
              "messages": [
                {
                  "role": "user",
                  "content": "%s"
                }
              ],
              "temperature": 0.7 // Aumentado ligeiramente para mais criatividade, opcional
            }
            """.formatted(model, prompt);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url)) // <= Usando a URL correta
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", referer) // <= Usando o referer correto
                    .header("X-Title", "Chat Java OpenRouter RAG")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("Erro na API da OpenRouter (Status: {}): {}", response.statusCode(), response.body());
                return "Hmm... parece que tive um problema ao processar sua pergunta. Por favor, tente novamente mais tarde.";
            }

            JsonNode json = objectMapper.readTree(response.body());
            JsonNode choice = json.at("/choices/0/message/content");

            if (choice.isMissingNode()) {
                logger.error("Resposta da OpenRouter não contém conteúdo esperado: {}", response.body());
                return "Hmm... ainda estou aprendendo! 😅 Poderia reformular sua pergunta? Ou fale com nosso time!";
            }

            return choice.asText().trim();

        } catch (IOException e) {
            logger.error("Erro de E/S ao se comunicar com a API da OpenRouter", e);
            return "Hmm... parece que tive um problema ao processar sua pergunta. Por favor, tente novamente mais tarde.";
        } catch (InterruptedException e) {
            logger.error("Requisição para OpenRouter foi interrompida", e);
            Thread.currentThread().interrupt(); // Restaura o status de interrupção
            return "Hmm... parece que tive um problema ao processar sua pergunta. Por favor, tente novamente mais tarde.";
        }
    }
}