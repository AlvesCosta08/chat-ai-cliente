# Sistema de Suporte ao Cliente com IA (MVP)

[![Java](https://img.shields.io/badge/Java-21%2B-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)
[![Status](https://img.shields.io/badge/Status-MVP-yellow)](https://chat-ai-cliente.onrender.com/chat.html)

## 📋 Índice
- [Visão Geral](#visão-geral)
- [Funcionalidades](#funcionalidades)
- [Tecnologias Utilizadas](#tecnologias-utilizadas)
- [Arquitetura do Projeto](#arquitetura-do-projeto)
- [Como Executar](#como-executar)
- [Configuração](#configuração)
- [API Reference](#api-reference)
- [Testes](#testes)
- [Base de Conhecimento](#base-de-conhecimento)
- [Deploy](#deploy)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Contribuição](#contribuição)
- [Próximos Passos](#próximos-passos)
- [Avisos Importantes](#avisos-importantes)

## 🎯 Visão Geral

Este é um **MVP (Produto Mínimo Viável)** para um sistema de suporte ao cliente baseado em **Inteligência Artificial**. O sistema fornece respostas automatizadas a perguntas frequentes através da integração com uma base de conhecimento estática (FAQs, manuais, políticas) utilizando a técnica **RAG (Retrieval-Augmented Generation)** para gerar respostas contextualizadas e precisas.

**Demo Online**: [https://chat-ai-cliente.onrender.com/chat.html](https://chat-ai-cliente.onrender.com/chat.html)

## ✅ Funcionalidades

| Módulo | Funcionalidade | Status | Descrição |
|--------|----------------|---------|-----------|
| **API** | RESTful Endpoints | ✅ | Endpoints para receber perguntas e retornar respostas |
| **Banco de Dados** | Armazenamento de Interações | ✅ | Registro de todas as perguntas e respostas em H2 |
| **IA** | RAG com IA Real | ✅ | Busca semântica + geração via OpenRouter |
| **Frontend** | Widget de Chat | ✅ | Interface web para interação com usuários |
| **Testes** | Unitários | ✅ | Cobertura completa das camadas principais |
| **Testes** | Integração | ✅ | Validação de endpoints e persistência |
| **Testes** | E2E | ✅ | Simulação de requisições HTTP reais |

## 🛠️ Tecnologias Utilizadas

### Backend
- **Java 21+** - Linguagem de programação principal
- **Spring Boot 3.x** - Framework para desenvolvimento rápido
- **Spring Data JPA** - Camada de persistência de dados
- **Spring Web MVC** - Construção de API REST

### Banco de Dados
- **H2 Database** - Banco de dados em memória para desenvolvimento e testes

### Testes e Qualidade
- **JUnit 5** - Framework para testes unitários e de integração
- **Mockito** - Biblioteca para mocking em testes
- **Spring Boot Test** - Suporte a testes de integração Spring
- **TestRestTemplate** - Cliente HTTP para testes de endpoints

### Inteligência Artificial
- **OpenRouter API** - Gateway para múltiplos modelos de IA
- **RAG (Retrieval-Augmented Generation)** - Técnica para busca semântica e geração contextualizada

### Build e Deploy
- **Maven** - Gerenciamento de dependências e automação de build
- **Render** - Plataforma de deploy em nuvem

## 🏗️ Arquitetura do Projeto

### Padrão Arquitetural
O sistema segue o padrão MVC (Model-View-Controller) com as seguintes camadas:

- **Controller**: Camada de apresentação (API REST)
- **Service**: Lógica de negócio e orquestração
- **Repository**: Acesso a dados e persistência
- **Model**: Entidades de domínio
- **AI Service**: Integração com serviços de IA externos

### Diagrama de Fluxo de Dados





## 🚀 Como Executar

### Pré-requisitos Mínimos
- **Java 21** ou superior instalado
- **Maven 3.6** ou superior
- **Chave de API** da OpenRouter (obtenha em [OpenRouter](https://openrouter.ai/))
- **Git** para clonagem do repositório

### Configuração Rápida

#### 1. Configuração de Ambiente (Recomendado)

# Variáveis de ambiente
export OPENROUTER_API_KEY="sua_chave_aqui"
export OPENROUTER_MODEL="openai/gpt-3.5-turbo"

# Ou para Windows (PowerShell)
$env:OPENROUTER_API_KEY="sua_chave_aqui"
$env:OPENROUTER_MODEL="openai/gpt-3.5-turbo"


Configuração via Arquivo
Edite src/main/resources/application.properties:
# Configurações da OpenRouter
openrouter.api.key=sua_chave_aqui
openrouter.model=openai/gpt-3.5-turbo

# Configurações do Spring Boot
server.port=8080
spring.datasource.url=jdbc:h2:mem:testdb
spring.h2.console.enabled=true

Execução Passo a Passo

git clone <url-do-repositorio>
cd <nome-do-projeto>

Compilação e Execução

# Opção 1: Execução direta com Maven Wrapper
./mvnw spring-boot:run

# Opção 2: Build e execução separada
./mvnw clean install
java -jar target/atendimento-ai-mvp-1.0.0.jar

# Opção 3: Execução com perfil de desenvolvimento
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev


Verificação
Após iniciar, acesse:

Aplicação Principal: http://localhost:8080

Widget de Chat: http://localhost:8080/chat.html

Console H2: http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:testdb)


⚙️ Configuração
Configurações da Aplicação
application.properties

# Servidor
server.port=8080
server.servlet.context-path=/

# Banco de Dados H2
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true

# JPA
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# OpenRouter
openrouter.api.key=${OPENROUTER_API_KEY:default_key}
openrouter.model=${OPENROUTER_MODEL:openai/gpt-3.5-turbo}
openrouter.base.url=https://openrouter.ai/api/v1


Variáveis de Ambiente Suportadas
Variável	Descrição	Valor Padrão
OPENROUTER_API_KEY	Chave de API do OpenRouter	Obrigatória
OPENROUTER_MODEL	Modelo de IA a ser utilizado	openai/gpt-3.5-turbo
SERVER_PORT	Porta da aplicação	8080
SPRING_PROFILES_ACTIVE	Perfil ativo	dev


🔌 API Reference
Endpoints Disponíveis
POST /api/chat/question
Envia uma pergunta para o sistema de IA e retorna uma resposta contextualizada.

Content-Type: application/json

Request Body:

{
"question": "string - Pergunta do usuário"
}

Response Body (Sucesso - 200 OK):

{
"answer": "string - Resposta gerada pela IA",
"timestamp": "string - Data e hora no formato ISO",
"question": "string - Pergunta original"
}

Response Body (Erro - 400 Bad Request):

{
"error": "string - Descrição do erro",
"timestamp": "string - Data e hora do erro"
}

Exemplo de Uso:

curl -X POST "http://localhost:8080/api/chat/question" \
-H "Content-Type: application/json" \
-d '{"question": "Como redefinir minha senha?"}'


Resposta Esperada:

{
"answer": "Para redefinir sua senha, acesse a página de login e clique em 'Esqueci minha senha'. Você receberá um email com instruções para criar uma nova senha.",
"timestamp": "2024-01-15T10:30:00.000Z",
"question": "Como redefinir minha senha?"
}


Códigos de Status HTTP
Código	Situação	Descrição
200	OK	Requisição processada com sucesso
400	Bad Request	Dados de entrada inválidos
500	Internal Server Error	Erro interno do servidor


🧪 Testes
Execução dos Testes


# Executar todos os testes
./mvnw test

# Executar testes com relatório de cobertura
./mvnw test jacoco:report

# Executar testes específicos
./mvnw test -Dtest=ChatControllerTest
./mvnw test -Dtest=*ServiceTest



Suíte de Testes
Testes Unitários
Classe de Teste	Camada	Descrição
InteractionLogTest	Model	Validação da entidade principal
InteractionServiceTest	Service	Teste da lógica de negócio
AiServiceTest	Service	Teste da integração com IA


Testes de Integração
Classe de Teste	Camada	Descrição
InteractionLogRepositoryTest	Repository	Teste de persistência no banco
ChatControllerTest	Controller	Teste de endpoints da API


Testes End-to-End (E2E)
Classe de Teste	Descrição
ChatEndpointE2ETest	Teste completo do fluxo da aplicação

Cobertura de Testes
Model: 100%

Service: 95%

Controller: 90%

Repository: 100%

Integração: 85%




📚 Base de Conhecimento
Estrutura do Arquivo
Localização: src/main/resources/knowledge_base.json

[
{
"question": "string - Pergunta ou palavra-chave",
"answer": "string - Resposta correspondente"
}
]


Exemplo de Conteúdo

[
{
"question": "redefinir senha",
"answer": "Para redefinir sua senha: 1) Acesse a página de login 2) Clique em 'Esqueci minha senha' 3) Informe seu email 4) Siga as instruções no email recebido 5) Crie uma nova senha"
},
{
"question": "suporte técnico",
"answer": "Nosso suporte técnico está disponível: Segunda a Sexta: 8h às 18h | Sábado: 9h às 13h | Email: suporte@empresa.com | Telefone: (11) 1234-5678"
},
{
"question": "problema login",
"answer": "Se está com problemas para fazer login: 1) Verifique seu email e senha 2) Tente redefinir a senha 3) Limpe o cache do navegador 4) Tente outro navegador 5) Entre em contato com o suporte"
}
]



Adicionando Novo Conhecimento
Edite o arquivo knowledge_base.json

Adicione novas entradas no formato especificado

Reinicie a aplicação (não é necessário recompilar)


🌐 Deploy
Ambiente de Produção
URL da Aplicação: https://chat-ai-cliente.onrender.com/chat.html

Configuração no Render.com
Build Settings
Build Command: ./mvnw clean install

Start Command: java -jar target/*.jar

Environment Variables


OPENROUTER_API_KEY=sua_chave_producao
OPENROUTER_MODEL=openai/gpt-3.5-turbo
SPRING_PROFILES_ACTIVE=prod
JAVA_VERSION=21


Deploy Manual
1. Preparação do Build

# Build para produção
./mvnw clean package -DskipTests -Pprod

# Verifique o JAR gerado
ls -la target/*.jar



Deploy em Servidor

# Copie o JAR para o servidor
scp target/atendimento-ai-mvp-1.0.0.jar usuario@servidor:/app/

# Execute no servidor
java -jar -Dspring.profiles.active=prod atendimento-ai-mvp-1.0.0.jar



📁 Estrutura do Projeto

atendimento-ai-mvp/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/codigoquatro/atendimento_ai/
│   │   │       ├── AtendimentoAiApplication.java      # Classe principal
│   │   │       ├── controller/
│   │   │       │   └── ChatController.java            # Controlador REST
│   │   │       ├── service/
│   │   │       │   └── InteractionService.java        # Serviço de negócio
│   │   │       ├── model/
│   │   │       │   └── InteractionLog.java            # Entidade JPA
│   │   │       ├── repository/
│   │   │       │   └── InteractionLogRepository.java  # Repositório Spring Data
│   │   │       ├── ai/
│   │   │       │   ├── AiService.java                 # Serviço de IA
│   │   │       │   └── AiServiceSimulator.java        # Simulador para testes
│   │   │       └── config/
│   │   │           └── AppConfig.java                 # Configurações
│   │   └── resources/
│   │       ├── application.properties                 # Configurações
│   │       ├── knowledge_base.json                    # Base de conhecimento
│   │       └── static/
│   │           └── chat.html                          # Widget de chat
│   └── test/
│       └── java/
│           └── com/codigoquatro/atendimento_ai/
│               ├── controller/
│               │   └── ChatControllerTest.java        # Testes do controlador
│               ├── service/
│               │   └── InteractionServiceTest.java    # Testes do serviço
│               ├── repository/
│               │   └── InteractionLogRepositoryTest.java # Testes do repositório
│               ├── model/
│               │   └── InteractionLogTest.java        # Testes do modelo
│               └── ChatEndpointE2ETest.java           # Testes E2E
├── pom.xml                                           # Configuração Maven
├── README.md                                         # Documentação
└── LICENSE                                           # Licença MIT



🤝 Contribuição
Processo de Contribuição

# Faça fork no GitHub
# Clone seu fork
git clone https://github.com/seu-usuario/atendimento-ai-mvp.git
cd atendimento-ai-mvp


Criação de Branch

git checkout -b feature/nova-funcionalidade
# ou
git checkout -b fix/correcao-bug


Desenvolvimento

# Faça suas alterações
# Adicione testes
# Atualize documentação



Commit e Push


git add .
git commit -m "feat: adiciona nova funcionalidade"
git push origin feature/nova-funcionalidade

Pull Request

Abra PR no repositório original

Descreva as mudanças

Referencie issues relacionadas

Convenções
Commits
feat: Nova funcionalidade

fix: Correção de bug

docs: Documentação

test: Testes

refactor: Refatoração de código

style: Formatação



Código
Siga o estilo Java convencional

Use nomes descritivos para variáveis e métodos

Documente classes e métodos complexos

Mantenha a cobertura de testes


Melhorias Planejadas
Fase 2 - Autenticação e Dashboard
Sistema de autenticação JWT

Dashboard administrativo

Gestão de usuários

Relatórios de uso



Fase 3 - Funcionalidades Avançadas
Cache de respostas frequentes

Suporte a múltiplos idiomas

Integração com sistemas de tickets

Análise de sentimentos

Fase 4 - Escalabilidade
Banco de dados PostgreSQL

Cache Redis

Load balancing

Monitoramento e métricas

Roadmap Detalhado
Fase	Funcionalidades	Estimativa
Fase 1 (MVP)	Funcionalidades atuais	✅ Concluído
Fase 2	Auth + Dashboard	2-3 meses
Fase 3	Features avançadas	3-4 meses
Fase 4	Escalabilidade	2-3 meses


⚠️ Avisos Importantes
Limitações do MVP
Base de Conhecimento Estática

Requer atualização manual do arquivo JSON

Não possui interface de administração

Armazenamento Volátil

Banco H2 em memória

Dados são perdidos ao reiniciar a aplicação

Segurança Básica

Não possui autenticação

Rate limiting básico

Validação de input mínima

Escalabilidade

Não preparado para alta carga

Sem cache distribuído

Monolítico


Recomendações para Produção



# Migrar para PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/atendimento_ai
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect


Segurança

Implementar Spring Security

Adicionar rate limiting

Configurar CORS adequadamente

Usar HTTPS



Monitoramento

Adicionar Spring Boot Actuator

Configurar métricas e health checks

Implementar logging estruturado



📄 Licença
Este projeto está licenciado sob a MIT License - veja o arquivo LICENSE para detalhes.

MIT License

Copyright (c) 2024 CodigoQuatro Soluções

Permissão é concedida, gratuitamente, a qualquer pessoa que obtenha uma cópia
deste software e arquivos de documentação associados (o "Software"), para lidar
no Software sem restrição, incluindo, sem limitação, os direitos de usar, copiar,
modificar, fundir, publicar, distribuir, sublicenciar e/ou vender cópias do
Software, e para permitir que as pessoas a quem o Software é fornecido o façam...


Desenvolvido por CodigoQuatro Soluções
Email: codigoquatro2022@gmail.com
Site: https://chat-ai-cliente.onrender.com
Repositório: GitHub - atendimento-ai-mvp

Documentação atualizada em: Janeiro 2024





























































































































 













