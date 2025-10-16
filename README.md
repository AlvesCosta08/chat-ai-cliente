# Sistema de Suporte ao Cliente com IA (MVP)

Este é um **MVP (Produto Mínimo Viável)** para um sistema de suporte ao cliente baseado em Inteligência Artificial. O objetivo é fornecer respostas automatizadas a perguntas frequentes, integrando-se a uma base de conhecimento estática (FAQs, manuais, políticas) e simulando a geração de respostas com IA.

### ⚠️ Aviso Importante sobre a IA

**Este MVP NÃO implementa uma IA real (como um modelo de linguagem LLM via API).** A "IA" neste projeto é **simulada**. O componente `AiServiceSimulator` procura correspondências exatas ou parciais em uma base de conhecimento fixa (arquivo `knowledge_base.json` em `src/main/resources`) para encontrar respostas. **Esta é uma limitação intencional do MVP**. A intenção é provar o conceito e a arquitetura, servindo como base para a integração futura com um serviço de IA real (por exemplo, via OpenAI GPT, Anthropic Claude, ou modelos locais com RAG).

---

### ✅ Funcionalidades do MVP

- **API RESTful**: Endpoints para receber perguntas dos usuários e retornar respostas.
- **Armazenamento de Interações**: Registra todas as perguntas recebidas e respostas fornecidas em um banco de dados (H2 em memória para o MVP).
- **Simulação de IA (RAG Simples)**: Procura por respostas relevantes em uma base de conhecimento estática (simulada a partir de `knowledge_base.json`).
- **Testes Unitários e de Integração**: Cobertura de testes para todas as camadas principais (Modelo, Repositório, Serviço, Controlador).
- **Widget de Chat (Simulado)**: Exemplo de como integrar o backend via frontend (HTML/JS).
- **Teste de Integração E2E**: Teste que simula requisições HTTP reais para o endpoint da API e verifica persistência no banco de dados.

---

### 🛠️ Tecnologias Utilizadas

- **Backend**: Java 17+, Spring Boot 3.x
- **Banco de Dados**: H2 (em memória)
- **Persistência**: Spring Data JPA
- **Web**: Spring Web MVC
- **Testes**: JUnit 5, Mockito, Spring Boot Test, TestRestTemplate
- **Build**: Maven
- **API REST**: Jackson (serialização/desserialização JSON)
- **Mock de IA**: Simulado com leitura de arquivo JSON (`knowledge_base.json`)

---

### 📁 Estrutura do Projeto


```

src/
├── main/
│ ├── java/
│ │ └── com/codigoquatro/atendimento_ai/
│ │ ├── AtendimentoAiApplication.java
│ │ ├── controller/
│ │ │ └── ChatController.java
│ │ ├── service/
│ │ │ └── InteractionService.java
│ │ ├── model/
│ │ │ └── InteractionLog.java
│ │ ├── repository/
│ │ │ └── InteractionLogRepository.java
│ │ ├── ai/
│ │ │ ├── AiService.java
│ │ │ └── AiServiceSimulator.java
│ │ └── config/ (se necessário)
│ └── resources/
│ ├── application.properties
│ └── static/ (para o widget HTML, se necessário)
└── test/
└── java/
└── com/codigoquatro/atendimento_ai/
├── controller/
│ └── ChatControllerTest.java
├── service/
│ └── InteractionServiceTest.java
├── repository/
│ └── InteractionLogRepositoryTest.java
└── model/
└── InteractionLogTest.java


```


### 🚀 Como Executar

1.  **Pré-requisitos**:
    - Java 17 ou superior
    - Maven 3.6 ou superior

2.  **Clonar o Repositório** (se aplicável):
    ```bash
    # git clone <URL_DO_SEU_REPOSITORIO>
    # cd <NOME_DO_PROJETO>
    ```

3.  **Compilar e Executar**:
    ```bash
    ./mvnw spring-boot:run
    ```
    ou
    ```bash
    ./mvnw clean install
    java -jar target/<NOME_DO_JAR>.jar
    ```

4.  **Acessar a API**:
    - O servidor iniciará em `http://localhost:8080`.
    - O endpoint principal para perguntas é: `POST http://localhost:8080/api/chat/question`
    - Exemplo de corpo da requisição (JSON):
      ```json
      {
        "question": "Como redefinir minha senha?"
      }
      ```

5.  **Acessar o Widget de Chat**:
    - Após iniciar a aplicação, acesse: `http://localhost:8080/chat.html`
    - Digite uma pergunta que esteja cadastrada no `knowledge_base.json` (ex: "como redefinir minha senha?").
    - O widget enviará a pergunta para o endpoint `/api/chat/question` e exibirá a resposta.

---

### 🧪 Como Executar os Testes

Execute todos os testes (unitários e de integração) com Maven:

```
./mvnw test
```
###  Este comando executará:

InteractionLogTest (Teste unitário do modelo)
InteractionLogRepositoryTest (Teste de integração de repositório)
InteractionServiceTest (Teste unitário do serviço)
ChatControllerTest (Teste de integração de controlador)
ChatEndpointE2ETest (Teste E2E de ponta a ponta)
Todos devem passar com sucesso.

🔌 Fonte de Conhecimento
A base de conhecimento do MVP é definida no arquivo src/main/resources/knowledge_base.json. Você pode editá-lo para adicionar ou modificar perguntas e respostas sem recompilar o código.

🤝 Contribuição
Contribuições são bem-vindas! Por favor, abra uma issue ou envie um pull request.

Este projeto é um MVP e não está pronto para produção. Ele serve como uma demonstração de conceito e base para desenvolvimento futuro com integração real de IA.# chat-ai-cliente
