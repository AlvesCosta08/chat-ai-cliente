package com.codigoquatro.atendimento_ai.ai;

import com.codigoquatro.atendimento_ai.model.Product;
import com.codigoquatro.atendimento_ai.service.SmComponentesScraperService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.similarity.LevenshteinDistance;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiServiceSimulator implements AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiServiceSimulator.class);
    private static final double SIMILARITY_THRESHOLD = 0.70;
    private static final int MAX_LEVENSHTEIN_DISTANCE = 5;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LevenshteinDistance levenshteinDistance = LevenshteinDistance.getDefaultInstance();
    private HttpClient httpClient;
    private String apiKey;
    private String model;
    private List<KnowledgeEntry> knowledgeBase = List.of();

    // Cache inteligente para perguntas frequentes
    private final Map<String, CacheEntry> responseCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
            return size() > 100; // Mantém apenas 100 entradas no cache
        }
    };

    private final SmComponentesScraperService scraperService;

    // Palavras-chave para detecção de intenção
    private static final Set<String> PRODUCT_KEYWORDS = Set.of(
            "comprar", "produto", "componente", "conector", "cabo", "adaptador",
            "preço", "valor", "custo", "onde encontrar", "quero comprar"
    );

    private static final Set<String> SUPPORT_KEYWORDS = Set.of(
            "problema", "ajuda", "suporte", "dúvida", "como usar", "funcionamento",
            "defeito", "não funciona", "garantia", "tutorial"
    );

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

        String normalizedQuestion = normalizeQuestion(question);

        // Verifica cache primeiro
        CacheEntry cachedResponse = responseCache.get(normalizedQuestion);
        if (cachedResponse != null && !cachedResponse.isExpired()) {
            logger.info("Resposta recuperada do cache para: {}", normalizedQuestion);
            return cachedResponse.getResponse();
        }

        try {
            // 1. Detectar intenção da pergunta
            QuestionIntent intent = detectIntent(question);

            // 2. Busca na base de conhecimento estática
            List<KnowledgeEntry> relevantEntries = findRelevantEntries(normalizedQuestion);

            // 3. Busca dinâmica de produtos (apenas se for intenção de compra/produto)
            List<Product> relevantProducts = Collections.emptyList();
            if (intent == QuestionIntent.PRODUCT_INQUIRY) {
                relevantProducts = scraperService.searchProducts(question);
            }

            // 4. Monta contexto combinado baseado na intenção
            String context = buildContext(intent, relevantEntries, relevantProducts, question);

            // 5. Se encontrou resposta exata na base de conhecimento, usa ela
            if (!relevantEntries.isEmpty() && hasExactMatch(relevantEntries, normalizedQuestion)) {
                String exactAnswer = relevantEntries.get(0).getAnswer();
                cacheResponse(normalizedQuestion, exactAnswer);
                return exactAnswer;
            }

            // 6. Monta prompt otimizado baseado na intenção
            String prompt = buildPrompt(intent, question, context);

            // 7. Chama OpenRouter
            String aiResponse = callOpenRouter(prompt);

            // Processa links para garantir que sejam clicáveis
            String processedResponse = ensureClickableLinks(aiResponse);

            // Cache da resposta
            cacheResponse(normalizedQuestion, processedResponse);

            return processedResponse;

        } catch (Exception e) {
            logger.error("Erro inesperado ao processar pergunta: '{}'", question, e);
            return getFallbackResponse(question);
        }
    }

    private String normalizeQuestion(String question) {
        return question.toLowerCase()
                .replaceAll("[^a-z0-9áéíóúâêîôûãõç\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private QuestionIntent detectIntent(String question) {
        String lowerQuestion = question.toLowerCase();

        boolean hasProductKeywords = PRODUCT_KEYWORDS.stream()
                .anyMatch(lowerQuestion::contains);

        boolean hasSupportKeywords = SUPPORT_KEYWORDS.stream()
                .anyMatch(lowerQuestion::contains);

        if (hasProductKeywords) {
            return QuestionIntent.PRODUCT_INQUIRY;
        } else if (hasSupportKeywords) {
            return QuestionIntent.SUPPORT_REQUEST;
        } else {
            return QuestionIntent.GENERAL_INQUIRY;
        }
    }

    private List<KnowledgeEntry> findRelevantEntries(String userQuestion) {
        if (knowledgeBase.isEmpty()) return List.of();

        return knowledgeBase.stream()
                .map(entry -> {
                    double similarityScore = calculateSimilarity(userQuestion, entry.getQuestion());
                    return Map.entry(entry, similarityScore);
                })
                .filter(e -> e.getValue() >= SIMILARITY_THRESHOLD)
                .sorted(Map.Entry.<KnowledgeEntry, Double>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private double calculateSimilarity(String question1, String question2) {
        String normalized1 = normalizeQuestion(question1);
        String normalized2 = normalizeQuestion(question2);

        // Usa Levenshtein para similaridade
        int maxLength = Math.max(normalized1.length(), normalized2.length());
        if (maxLength == 0) return 1.0;

        int distance = levenshteinDistance.apply(normalized1, normalized2);
        double similarity = 1.0 - ((double) distance / maxLength);

        return similarity;
    }

    private boolean hasExactMatch(List<KnowledgeEntry> entries, String userQuestion) {
        return entries.stream()
                .anyMatch(entry -> {
                    String normalizedEntryQuestion = normalizeQuestion(entry.getQuestion());
                    int distance = levenshteinDistance.apply(userQuestion, normalizedEntryQuestion);
                    return distance <= MAX_LEVENSHTEIN_DISTANCE;
                });
    }

    private String buildContext(QuestionIntent intent, List<KnowledgeEntry> entries,
                                List<Product> products, String originalQuestion) {
        StringBuilder sb = new StringBuilder();

        // Informações institucionais (para todos os tipos de intenção)
        if (!entries.isEmpty()) {
            sb.append("📚 **Informações institucionais relevantes:**\n");
            for (KnowledgeEntry entry : entries) {
                sb.append("• ").append(entry.getAnswer()).append("\n");
            }
            sb.append("\n");
        }

        // Produtos (apenas para intenção de produto)
        if (intent == QuestionIntent.PRODUCT_INQUIRY) {
            if (!products.isEmpty()) {
                sb.append("🛒 **Produtos encontrados na SM Componentes:**\n");
                for (Product p : products) {
                    sb.append(String.format(
                            "• **%s** (Categoria: %s)\n  🔗 <a href=\"%s\" target=\"_blank\" style=\"color: #007bff; text-decoration: none;\">Ver produto</a>\n\n",
                            p.getName(),
                            p.getCategory(),
                            p.getProductUrl()
                    ));
                }
            } else {
                sb.append("🔍 **Sugestão de categorias para sua busca:**\n");
                sb.append("• <a href=\"https://smcomponentes.com.br/loja/categoria-conectores-variados\" target=\"_blank\" style=\"color: #007bff; text-decoration: none;\">Conectores Variados</a>\n");
                sb.append("• <a href=\"https://smcomponentes.com.br/loja/categoria-potenciometros\" target=\"_blank\" style=\"color: #007bff; text-decoration: none;\">Potenciômetros</a>\n");
                sb.append("• <a href=\"https://smcomponentes.com.br/loja/categoria-audio-e-video\" target=\"_blank\" style=\"color: #007bff; text-decoration: none;\">Áudio e Vídeo</a>\n");
                sb.append("• <a href=\"https://smcomponentes.com.br/loja/categoria-acessorios\" target=\"_blank\" style=\"color: #007bff; text-decoration: none;\">Acessórios</a>\n");
                sb.append("• <a href=\"https://smcomponentes.com.br/loja/categoria-cabos-de-energia\" target=\"_blank\" style=\"color: #007bff; text-decoration: none;\">Cabos de Energia</a>\n");
            }
        }

        // Informações de suporte (apenas para intenção de suporte)
        if (intent == QuestionIntent.SUPPORT_REQUEST) {
            sb.append("🔧 **Informações de suporte técnico:**\n");
            sb.append("• Horário de atendimento: Segunda a Sexta, 8h às 18h\n");
            sb.append("• Email de suporte: <a href=\"mailto:suporte@smcomponentes.com.br\" style=\"color: #007bff; text-decoration: none;\">suporte@smcomponentes.com.br</a>\n");
            sb.append("• WhatsApp: <a href=\"https://wa.me/5511999999999\" target=\"_blank\" style=\"color: #007bff; text-decoration: none;\">(11) 99999-9999</a>\n");
        }

        return sb.toString().trim();
    }

    private String buildPrompt(QuestionIntent intent, String question, String context) {
        String role = getRoleByIntent(intent);

        return """
            %s

            Contexto disponível:
            %s

            Pergunta do cliente:
            "%s"

            Regras importantes:
            - Seja direto e útil
            - Use formatação Markdown quando apropriado
            - Se não souber a resposta exata, seja honesto
            - Mantenha o tom profissional mas amigável
            - Use emojis com moderação (máximo 2-3 por resposta)
            - SEMPRE use links HTML para URLs: <a href="URL" target="_blank">Texto</a>
            - Para emails use: <a href="mailto:email@exemplo.com">email@exemplo.com</a>
            - Para WhatsApp use: <a href="https://wa.me/5511999999999" target="_blank">(11) 99999-9999</a>

            Resposta (em português do Brasil):
            """.formatted(role, context, question);
    }

    private String getRoleByIntent(QuestionIntent intent) {
        switch (intent) {
            case PRODUCT_INQUIRY:
                return "Você é um vendedor especializado da SM Componentes, loja de componentes eletrônicos. Sua missão é ajudar clientes a encontrar produtos e fornecer informações técnicas precisas. SEMPRE inclua links clicáveis para produtos e categorias usando HTML <a> tags.";
            case SUPPORT_REQUEST:
                return "Você é um técnico de suporte da SM Componentes. Sua missão é resolver problemas técnicos, fornecer orientações e direcionar para o canal apropriado quando necessário. SEMPRE inclua links de contato clicáveis usando HTML <a> tags.";
            default:
                return "Você é um atendente da SM Componentes. Sua missão é responder dúvidas gerais sobre a empresa, produtos e serviços de forma clara e útil. SEMPRE use links HTML clicáveis quando mencionar URLs.";
        }
    }

    private String callOpenRouter(String prompt) {
        String url = "https://openrouter.ai/api/v1/chat/completions";
        String referer = "https://smcomponentes.com.br";

        try {
            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("model", model);

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);

            requestBodyMap.put("messages", messages);
            requestBodyMap.put("temperature", 0.6);
            requestBodyMap.put("max_tokens", 800);

            String requestBody = objectMapper.writeValueAsString(requestBodyMap);

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
                throw new RuntimeException("Erro na API de IA: " + response.statusCode());
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

    private String ensureClickableLinks(String response) {
        // Converte markdown [texto](url) para HTML <a>
        String withHtmlLinks = response.replaceAll(
            "\\[([^\\]]+)\\]\\(([^)]+)\\)", 
            "<a href=\"$2\" target=\"_blank\" style=\"color: #007bff; text-decoration: none;\">$1</a>"
        );
        
        // Garante que URLs soltas também sejam links
        String withBareUrls = withHtmlLinks.replaceAll(
            "(?<!href=\")(https?://[^\\s<>\"]+)(?!\"[^>]*>)(?![^<]*</a>)", 
            "<a href=\"$1\" target=\"_blank\" style=\"color: #007bff; text-decoration: none;\">$1</a>"
        );
        
        return withBareUrls;
    }

    private void cacheResponse(String question, String response) {
        responseCache.put(question, new CacheEntry(response));
    }

    private String getFallbackResponse(String originalQuestion) {
        return """
            Olá! 😊 
            
            No momento, estou com dificuldades técnicas, mas posso te ajudar de outras formas:

            🔍 **Para encontrar produtos:** 
            Visite nossas categorias principais em <a href="https://smcomponentes.com.br" target="_blank" style="color: #007bff; text-decoration: none;">smcomponentes.com.br</a>

            📞 **Para suporte técnico:**
            Entre em contato pelo <a href="https://wa.me/5511999999999" target="_blank" style="color: #007bff; text-decoration: none;">WhatsApp (11) 99999-9999</a>

            📧 **Para outras dúvidas:**
            Envie um email para <a href="mailto:contato@smcomponentes.com.br" style="color: #007bff; text-decoration: none;">contato@smcomponentes.com.br</a>

            Enquanto isso, você pode reformular sua pergunta? Vou tentar novamente! 🔧
            """;
    }

    // Classes internas para organização
    private enum QuestionIntent {
        PRODUCT_INQUIRY, SUPPORT_REQUEST, GENERAL_INQUIRY
    }

    private static class CacheEntry {
        private final String response;
        private final long timestamp;
        private static final long CACHE_TTL = 30 * 60 * 1000; // 30 minutos

        CacheEntry(String response) {
            this.response = response;
            this.timestamp = System.currentTimeMillis();
        }

        public String getResponse() {
            return response;
        }

        public boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > CACHE_TTL;
        }
    }
}