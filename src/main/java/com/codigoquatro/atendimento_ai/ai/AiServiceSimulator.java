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
    private String model; // <= Adicionado
    private List<KnowledgeEntry> knowledgeBase = List.of();

    public AiServiceSimulator(@Value("${openrouter.api.key}") String apiKey,
                              @Value("${openrouter.model:openai/gpt-3.5-turbo}") String model) { // <= Injetado com fallback
        this.apiKey = apiKey;
        this.model = model; // <= Armazenado
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

        // 🧠 2. Monta o contexto para a IA
        String context = buildContext(relevantEntries);

        // 🤖 3. Monta a mensagem para a OpenRouter
        String prompt = buildPrompt(question, context);

        // 🌐 4. Chama a OpenRouter
        return callOpenRouter(prompt);
    }

    // 🔑 Busca por entradas relevantes
    private List<KnowledgeEntry> findRelevantEntries(String question) {
        String lowerQuestion = question.toLowerCase().trim();

        return knowledgeBase.stream()
                .filter(entry -> containsAny(lowerQuestion, entry.getQuestion().toLowerCase()) ||
                        containsAny(entry.getQuestion().toLowerCase(), lowerQuestion))
                .limit(3) // Pega no máximo 3 respostas relevantes
                .collect(Collectors.toList());
    }

    // 🔧 Verifica se alguma palavra está contida
    private boolean containsAny(String text, String target) {
        // Ex: "esqueci minha senha" contém "senha"?
        String[] words = target.split("\\s+");
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    // 🧩 Monta o contexto com entradas relevantes
    private String buildContext(List<KnowledgeEntry> entries) {
        if (entries.isEmpty()) {
            return "Nenhuma informação encontrada na base de conhecimento.";
        }

        StringBuilder sb = new StringBuilder("Base de Conhecimento:\n");
        for (KnowledgeEntry entry : entries) {
            sb.append("- Pergunta: ").append(entry.getQuestion()).append("\n");
            sb.append("  Resposta: ").append(entry.getAnswer()).append("\n\n");
        }
        return sb.toString();
    }

    // 🧠 Monta o prompt para a IA
    private String buildPrompt(String question, String context) {
        return """
            Você é um assistente de suporte ao cliente especializado em e-commerce.
            Use a base de conhecimento abaixo para responder à pergunta do usuário.
            Se a base não tiver a resposta, diga que está aprendendo e peça para reformular ou falar com o suporte.
            Base de Conhecimento:
            %s
            Pergunta do Usuário:
            %s
            Resposta:
            """.formatted(context, question);
    }

    // 🌐 Faz a chamada à API da OpenRouter
    private String callOpenRouter(String prompt) {
        String requestBody = """
            {
              "model": "%s",
              "messages": [
                {
                  "role": "user",
                  "content": "%s"
                }
              ],
              "temperature": 0.5
            }
            """.formatted(model, prompt); // <= Usando o modelo injetado

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://openrouter.ai/api/v1/chat/completions")) // <= Corrigido: removido espaço extra
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", "http://localhost:8080") // opcional
                    .header("X-Title", "Chat Java OpenRouter RAG") // opcional
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("Erro na API da OpenRouter: {}", response.body());
                return "Hmm... parece que tive um problema ao processar sua pergunta. Por favor, tente novamente mais tarde.";
            }

            JsonNode json = objectMapper.readTree(response.body());
            JsonNode choice = json.at("/choices/0/message/content");

            if (choice.isMissingNode()) {
                logger.error("Resposta da OpenRouter não contém conteúdo esperado: {}", response.body());
                return "Hmm... ainda estou aprendendo! 😅 Poderia reformular sua pergunta? Ou fale com nosso time pelo e-mail contato@seudominio.com.br!";
            }

            return choice.asText().trim();

        } catch (IOException | InterruptedException e) {
            logger.error("Erro ao se comunicar com a API da OpenRouter", e);
            Thread.currentThread().interrupt();
            return "Hmm... parece que tive um problema ao processar sua pergunta. Por favor, tente novamente mais tarde.";
        }
    }
}