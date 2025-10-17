Sistema de Suporte ao Cliente com IA (MVP)
Este é um MVP (Produto Mínimo Viável) para um sistema de suporte ao cliente baseado em Inteligência Artificial. O objetivo é fornecer respostas automatizadas a perguntas frequentes, integrando-se a uma base de conhecimento estática (FAQs, manuais, políticas) e gerando respostas com IA real via RAG (Retrieval-Augmented Generation).

✅ Funcionalidades do MVP
API RESTful: Endpoints para receber perguntas dos usuários e retornar respostas.
Armazenamento de Interações: Registra todas as perguntas recebidas e respostas fornecidas em um banco de dados (H2 em memória para o MVP).
RAG com IA Real: Procura por respostas relevantes em uma base de conhecimento local (arquivo knowledge_base.json) e envia o contexto para um modelo de IA (via OpenRouter) para geração de respostas contextualizadas.
Testes Unitários e de Integração: Cobertura de testes para todas as camadas principais (Modelo, Repositório, Serviço, Controlador).
Widget de Chat (Simulado): Exemplo de como integrar o backend via frontend (HTML/JS).
Teste de Integração E2E: Teste que simula requisições HTTP reais para o endpoint da API e verifica persistência no banco de dados.


🛠️ Tecnologias Utilizadas
Backend: Java 21+, Spring Boot 3.x
Banco de Dados: H2 (em memória)
Persistência: Spring Data JPA
Web: Spring Web MVC
Testes: JUnit 5, Mockito, Spring Boot Test, TestRestTemplate
Build: Maven
API REST: Jackson (serialização/desserialização JSON)
IA: Integração com OpenRouter (ex: openai/gpt-3.5-turbo, mistralai/mistral-7b-instruct)
RAG: Busca semântica simples com base em knowledge_base.json


src/
├── main/
│   ├── java/
│   │   └── com/codigoquatro/atendimento_ai/
│   │       ├── AtendimentoAiApplication.java
│   │       ├── controller/
│   │       │   └── ChatController.java
│   │       ├── service/
│   │       │   └── InteractionService.java
│   │       ├── model/
│   │       │   └── InteractionLog.java
│   │       ├── repository/
│   │       │   └── InteractionLogRepository.java
│   │       ├── ai/
│   │       │   ├── AiService.java
│   │       │   └── AiServiceSimulator.java
│   │       └── config/ (se necessário)
│   └── resources/
│       ├── application.properties
│       └── knowledge_base.json
└── test/
└── java/
└── com/codigoquatro/atendimento_ai/
├── controller/
│   └── ChatControllerTest.java
├── service/
│   └── InteractionServiceTest.java
├── repository/
│   └── InteractionLogRepositoryTest.java
└── model/
└── InteractionLogTest.java


🚀 Como Executar
Pré-requisitos:
Java 21 ou superior
Maven 3.6 ou superior
Chave de API da OpenRouter
Configurar variáveis de ambiente (recomendado)


export OPENROUTER_API_KEY="SUA_CHAVE_AQUI"
export OPENROUTER_MODEL="openai/gpt-3.5-turbo" # ou outro modelo suportado

Ou adicione ao arquivo src/main/resources/application.properties:

openrouter.api.key=SUA_CHAVE_AQUI
openrouter.model=openai/gpt-3.5-turbo

Clonar o Repositório (se aplicável):
# git clone <URL_DO_SEU_REPOSITORIO>
# cd <NOME_DO_PROJETO>

Compilar e Executar:
./mvnw spring-boot:run
ou
./mvnw clean install
java -jar target/<NOME_DO_JAR>.jar

Acessar a API:
O servidor iniciará em http://localhost:8080.
O endpoint principal para perguntas é: POST http://localhost:8080/api/chat/question
Exemplo de corpo da requisição (JSON):

{
"question": "Como redefinir minha senha?"
}

Acessar o Widget de Chat:
Após iniciar a aplicação, acesse: http://localhost:8080/chat.html
Digite uma pergunta que esteja cadastrada no knowledge_base.json (ex: "como redefinir minha senha?").
O widget enviará a pergunta para o endpoint /api/chat/question e exibirá a resposta gerada pela IA com base no contexto da base de conhecimento.

🧪 Como Executar os Testes
Execute todos os testes (unitários e de integração) com Maven:
./mvnw test

Este comando executará:

InteractionLogTest (Teste unitário do modelo)
InteractionLogRepositoryTest (Teste de integração de repositório)
InteractionServiceTest (Teste unitário do serviço)
ChatControllerTest (Teste de integração de controlador)
ChatEndpointE2ETest (Teste E2E de ponta a ponta)
Todos devem passar com sucesso.

🔌 Fonte de Conhecimento
A base de conhecimento do MVP é definida no arquivo src/main/resources/knowledge_base.json. Você pode editá-lo para adicionar ou modificar perguntas e respostas sem recompilar o código. O sistema faz uma busca semântica simples para encontrar entradas relevantes e as envia como contexto para a IA gerar respostas personalizadas.

🤝 Contribuição
Contribuições são bem-vindas! Por favor, abra uma issue ou envie um pull request.

Este projeto é um MVP e não está pronto para produção. Ele serve como uma demonstração de conceito e base para desenvolvimento futuro com integração real de IA e RAG.

Teste no ambiente real

https://chat-ai-cliente.onrender.com/chat.html