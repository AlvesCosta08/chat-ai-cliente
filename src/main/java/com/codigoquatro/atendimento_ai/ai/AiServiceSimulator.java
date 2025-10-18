package com.codigoquatro.atendimento_ai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.similarity.JaroWinklerSimilarity; // Import adicionado
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap; // Para cache simples (opcional, melhoria futura)
import java.util.stream.Collectors;

@Service
public class AiServiceSimulator implements AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiServiceSimulator.class);
    private static final double SIMILARITY_THRESHOLD = 0.75; // Ajuste conforme necessário

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity(); // Instância para similaridade
    private HttpClient httpClient;
    private String apiKey;
    private String model;
    private List<KnowledgeEntry> knowledgeBase = List.of();
    // Mapa para armazenar perguntas processadas recentemente e suas respostas (opcional, cache simples)
    private final Map<String, String> responseCache = new ConcurrentHashMap<>();

    public AiServiceSimulator(@Value("${openrouter.api.key}") String apiKey,
                              @Value("${openrouter.model:openai/gpt-3.5-turbo}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @PostConstruct
    public void init() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)) // Ajuste o tempo limite conforme necessário
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
            logger.error("Erro FATAL ao carregar a base de conhecimento. Aplicação pode não funcionar corretamente.", e);
            // Se for crítico, você pode lançar uma exceção para impedir o startup
            // throw new RuntimeException("Falha ao carregar knowledge_base.json", e);
            // Por enquanto, vamos continuar com uma base vazia
            this.knowledgeBase = List.of();
        }
    }

    @Override
    public String getAnswerForQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            return "A pergunta não pode estar vazia.";
        }

        // Opcional: Verificar cache (melhoria de desempenho para perguntas frequentes)
        // String cachedAnswer = responseCache.get(question);
        // if (cachedAnswer != null) {
        //     logger.debug("Resposta encontrada no cache para: {}", question);
        //     return cachedAnswer;
        // }

        try {
            // 🔍 1. Busca respostas relevantes na base local usando similaridade semântica
            List<KnowledgeEntry> relevantEntries = findRelevantEntriesBySimilarity(question);

            // 🧠 2. Monta o contexto para a IA (apenas se houver entradas relevantes acima do limiar)
            String context = buildContext(relevantEntries);

            // 🤖 3. Monta o prompt otimizado para RAG e contexto
            String prompt = buildPrompt(question, context);

            // 🌐 4. Chama a OpenRouter com o prompt construído
            String answer = callOpenRouter(prompt);

            // Opcional: Armazenar no cache
            // responseCache.put(question, answer);

            return answer;

        } catch (Exception e) {
            logger.error("Erro inesperado ao processar a pergunta: '{}'", question, e);
            // Resposta amigável para o usuário final
            return "Hmm... parece que tive um problema ao processar sua pergunta. Por favor, tente novamente mais tarde ou reformule sua dúvida. Obrigado pela compreensão!";
        }
    }

    // 🔑 Busca por entradas relevantes usando similaridade Jaro-Winkler
    private List<KnowledgeEntry> findRelevantEntriesBySimilarity(String userQuestion) {
        if (knowledgeBase.isEmpty()) {
            logger.debug("Base de conhecimento vazia, nenhuma entrada relevante encontrada.");
            return List.of();
        }

        String lowerUserQuestion = userQuestion.toLowerCase().trim();

        return knowledgeBase.stream()
                .map(entry -> {
                    // Calcula a similaridade entre a pergunta do usuário e a pergunta da base
                    double score = similarity.apply(lowerUserQuestion, entry.getQuestion().toLowerCase());
                    return Map.entry(entry, score); // Armazena entrada e pontuação
                })
                .filter(entryWithScore -> entryWithScore.getValue() >= SIMILARITY_THRESHOLD) // Filtra por limiar
                .sorted(Map.Entry.<KnowledgeEntry, Double>comparingByValue().reversed()) // Ordena por pontuação (mais similar primeiro)
                .limit(3) // Pega no máximo 3 respostas mais relevantes
                .map(Map.Entry::getKey) // Retorna apenas as entradas KnowledgeEntry
                .collect(Collectors.toList());
    }


    // 🧩 Monta o contexto *apenas* se houver entradas relevantes acima do limiar
    private String buildContext(List<KnowledgeEntry> entries) {
        if (entries.isEmpty()) {
            logger.debug("Nenhuma entrada da base de conhecimento atingiu o limiar de similaridade.");
            return "";
        }

        logger.debug("Encontradas {} entradas relevantes para o contexto.", entries.size());
        StringBuilder sb = new StringBuilder("Base de Conhecimento (informações específicas relevantes):\n");
        for (KnowledgeEntry entry : entries) {
            sb.append("- Pergunta: ").append(entry.getQuestion()).append("\n");
            sb.append("  Resposta: ").append(entry.getAnswer()).append("\n\n");
        }
        return sb.toString();
    }

    // 🧠 Monta o prompt otimizado para RAG e contexto
    private String buildPrompt(String question, String context) {
        String systemMessage = """
            Você é um assistente de suporte ao cliente especializado em e-commerce da CodeChat.
            Sua função é fornecer respostas claras, úteis, cordiais e humanizadas.
            """;

        String contextMessage = "";
        if (!context.isEmpty()) {
             contextMessage = """
             A seguir está a Base de Conhecimento com informações relevantes para a pergunta do usuário:
             %s
             Use esta base como prioridade para formular sua resposta. Se a base não tiver a resposta,
             use seu conhecimento geral para responder de forma útil e contextualizada.
             """.formatted(context);
        } else {
             contextMessage = """
             A base de conhecimento local não encontrou informações específicas para esta pergunta.
             Responda com base em seu conhecimento geral, mantendo o tom profissional, útil e humanizado.
             Se a pergunta for algo fora do escopo do e-commerce ou da empresa, responda de forma cordial e direta,
             talvez com um toque de personalidade, mas evite respostas evasivas ou genéricas como
             "não tenho acesso a informações em tempo real".
             """;
        }

        String userQuestionMessage = """
            Pergunta do Usuário:
            %s
            """.formatted(question);

        String assistantInstruction = """
            Resposta (seja direto, cordial, útil e humanizado, integrando o contexto se disponível):
            """;

        // Retorna o prompt completo como uma única string
        return systemMessage + contextMessage + userQuestionMessage + assistantInstruction;
    }


    // 🌐 Faz a chamada à API da OpenRouter com tratamento de erros robusto
    private String callOpenRouter(String prompt) {
        String url = "https://openrouter.ai/api/v1/chat/completions";
        String referer = "https://chat-ai-cliente.onrender.com:8080"; // ou sua URL real

        // Estrutura a mensagem como um array de objetos (padrão OpenAI/OpenRouter)
        String requestBody = """
            {
              "model": "%s",
              "messages": [
                {
                  "role": "user",
                  "content": "%s"
                }
              ],
              "temperature": 0.7
            }
            """.formatted(model, prompt);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", referer)
                    .header("X-Title", "Chat Java OpenRouter RAG") // Opcional
                    .timeout(Duration.ofSeconds(30)) // Define timeout para a requisição
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode != 200) {
                logger.error("Erro na API da OpenRouter (Status: {}). Resposta: {}", statusCode, responseBody);
                // Lança uma exceção para ser capturada no método getAnswerForQuestion
                throw new RuntimeException("Erro da API: " + statusCode + " - " + responseBody);
            }

            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode choicesNode = json.get("choices");

            if (choicesNode == null || choicesNode.isEmpty()) {
                logger.error("Resposta da OpenRouter não contém o array 'choices'. Resposta bruta: {}", responseBody);
                throw new RuntimeException("Formato de resposta inválido da API.");
            }

            JsonNode firstChoice = choicesNode.get(0);
            if (firstChoice == null) {
                logger.error("Primeira opção 'choice' não encontrada na resposta da OpenRouter. Resposta bruta: {}", responseBody);
                throw new RuntimeException("Formato de resposta inválido da API.");
            }

            JsonNode messageNode = firstChoice.get("message");
            if (messageNode == null) {
                logger.error("Objeto 'message' não encontrado dentro da primeira opção. Resposta bruta: {}", responseBody);
                throw new RuntimeException("Formato de resposta inválido da API.");
            }

            JsonNode contentNode = messageNode.get("content");
            if (contentNode == null || contentNode.isNull()) {
                logger.error("Campo 'content' dentro de 'message' é nulo ou ausente. Resposta bruta: {}", responseBody);
                throw new RuntimeException("Conteúdo da resposta da API é nulo ou ausente.");
            }

            String answer = contentNode.asText().trim();
            logger.debug("Resposta da IA recebida com sucesso.");
            return answer;

        } catch (IOException e) {
            logger.error("Erro de E/S ao se comunicar com a API da OpenRouter", e);
            // Lança para ser tratado no nível superior
            throw new RuntimeException("Erro de comunicação com o serviço de IA.", e);
        } catch (InterruptedException e) {
            logger.error("Requisição para OpenRouter foi interrompida", e);
            Thread.currentThread().interrupt(); // Restaura o status de interrupção
            // Lança para ser tratado no nível superior
            throw new RuntimeException("Requisição para o serviço de IA foi interrompida.", e);
        }
    }
}