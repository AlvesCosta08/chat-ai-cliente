package com.codigoquatro.atendimento_ai.ai;

import com.codigoquatro.atendimento_ai.model.Product;
import com.codigoquatro.atendimento_ai.service.SmComponentesScraperService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiServiceSimulator implements AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiServiceSimulator.class);
    private static final double SIMILARITY_THRESHOLD = 0.70;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();
    private HttpClient httpClient;
    private String apiKey;
    private String model;
    private List<KnowledgeEntry> knowledgeBase = List.of();

    private final SmComponentesScraperService scraperService;

    public AiServiceSimulator(
            @Value("${openrouter.api.key}") String apiKey,
            @Value("${openrouter.model:openai/gpt-3.5-turbo}") String model,
            SmComponentesScraperService scraperService) {
        this.apiKey = apiKey;
        this.model = model;
        this.scraperService = scraperService;
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
                this.knowledgeBase = objectMapper.readValue(inputStream,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, KnowledgeEntry.class));
                logger.info("Base de conhecimento carregada com sucesso. {} entradas.", knowledgeBase.size());
            }
        } catch (Exception e) {
            logger.error("Erro ao carregar a base de conhecimento. Continuando com base vazia.", e);
            this.knowledgeBase = List.of();
        }
    }

    @Override
    public String getAnswerForQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            return "Olá! 😊 Como posso te ajudar hoje na SM Componentes?";
        }

        try {
            // 1. Busca na base de conhecimento estática
            List<KnowledgeEntry> relevantEntries = findRelevantEntriesBySimilarity(question);

            // 2. Busca dinâmica de produtos na SM Componentes
            @SuppressWarnings("unchecked")
            List<Product> relevantProducts = (List<Product>)(List<?>) scraperService.searchProducts(question);

            // 3. Monta contexto combinado
            String context = buildContext(relevantEntries, relevantProducts);

            // 4. Monta prompt otimizado para componentes eletrônicos
            String prompt = buildPrompt(question, context);

            // 5. Chama OpenRouter
            return callOpenRouter(prompt);

        } catch (Exception e) {
            logger.error("Erro inesperado ao processar pergunta: '{}'", question, e);
            return "Desculpe, tive um probleminha técnico. Pode reformular sua dúvida? Estou aqui para ajudar! 😊";
        }
    }

    private List<KnowledgeEntry> findRelevantEntriesBySimilarity(String userQuestion) {
        if (knowledgeBase.isEmpty()) return List.of();

        String lowerUserQuestion = userQuestion.toLowerCase().trim();
        return knowledgeBase.stream()
                .map(entry -> {
                    double score = similarity.apply(lowerUserQuestion, entry.getQuestion().toLowerCase());
                    return Map.entry(entry, score);
                })
                .filter(e -> e.getValue() >= SIMILARITY_THRESHOLD)
                .sorted(Map.Entry.<KnowledgeEntry, Double>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private String buildContext(List<KnowledgeEntry> entries, List<Product> products) {
        StringBuilder sb = new StringBuilder();

        if (!entries.isEmpty()) {
            sb.append("ℹ️ Informações institucionais:\n");
            for (KnowledgeEntry entry : entries) {
                sb.append("- ").append(entry.getAnswer()).append("\n");
            }
            sb.append("\n");
        }

        if (!products.isEmpty()) {
            sb.append("🔌 **Produtos encontrados na SM Componentes:**\n");
            for (Product p : products) {
                sb.append(String.format(
                    "- **%s** (%s)\n  🔗 [Ver produto](%s)\n\n",
                    p.getName(),
                    p.getCategory(),
                    p.getProductUrl()
                ));
            }
            return sb.toString();
        }

        // Fallback: links das categorias principais
        sb.append("🔍 **Confira nossas categorias principais:**\n");
        sb.append("- [Conectores Variados](https://smcomponentes.com.br/loja/categoria-conectores-variados)\n");
        sb.append("- [Potenciômetros](https://smcomponentes.com.br/loja/categoria-potenciometros)\n");
        sb.append("- [Áudio e Vídeo](https://smcomponentes.com.br/loja/categoria-audio-e-video)\n");
        sb.append("- [Acessórios](https://smcomponentes.com.br/loja/categoria-acessorios)\n");
        // ... outras categorias

        return sb.toString();
    }

    private String buildPrompt(String question, String context) {
        return """
            Você é um atendente especializado da **SM Componentes**, loja especializada em componentes eletrônicos como:
            conectores, cabos, adaptadores, potenciômetros, bornes, plugs e acessórios técnicos.

            Sua missão:
            - Responder com clareza, precisão técnica e cordialidade.
            - Sempre que houver produtos listados acima, mencione-os com nome e link.
            - Se não souber a resposta exata, NÃO invente. Diga: "Vou verificar com nosso time técnico e te respondo em breve!"
            - Use emojis técnicos (🔌, ⚡, 📡, 🔌) com moderação.
            - Finalize com uma chamada para ação: "Precisa de ajuda para escolher?", "Quer que eu te envie o link direto?"

            Contexto disponível:
            %s

            Pergunta do cliente:
            "%s"

            Resposta (em português do Brasil, profissional e útil):
            """.formatted(context, question);
    }

    private String callOpenRouter(String prompt) {
        String url = "https://openrouter.ai/api/v1/chat/completions";
        String referer = "https://smcomponentes.com.br"; // URL real da sua loja

        try {
            String requestBody = """
                {
                  "model": "%s",
                  "messages": [
                    {
                      "role": "user",
                      "content": %s
                    }
                  ],
                  "temperature": 0.6
                }
                """.formatted(model, objectMapper.writeValueAsString(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", referer)
                    .header("X-Title", "SM Componentes - Atendente AI")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("Erro OpenRouter ({}): {}", response.statusCode(), response.body());
                throw new RuntimeException("Erro na API de IA");
            }

            JsonNode json = objectMapper.readTree(response.body());
            JsonNode contentNode = json.path("choices").get(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.isNull()) {
                throw new RuntimeException("Resposta da IA sem conteúdo");
            }

            return contentNode.asText().trim();

        } catch (IOException | InterruptedException e) {
            logger.error("Falha na chamada à OpenRouter", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Falha de comunicação com o serviço de IA", e);
        }
    }
}