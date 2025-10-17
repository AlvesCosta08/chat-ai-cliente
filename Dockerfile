# --- Estágio de Build ---
FROM eclipse-temurin:21-jdk-alpine AS build

# Define o diretório de trabalho dentro do container de build
WORKDIR /app

# Copia o arquivo pom.xml para o diretório de trabalho
# Esta etapa é feita primeiro para aproveitar o cache de camadas do Docker
# Se o pom.xml não mudar, esta camada não será reconstruída
COPY pom.xml .

# Instala o Maven no container Alpine e baixa as dependências do projeto
# mvn dependency:go-offline é uma forma eficiente de baixar todas as dependências
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B

# Copia o código-fonte do projeto para o diretório de trabalho
COPY src ./src

# Compila o projeto Maven, criando o JAR executável no diretório 'target'
# Os testes são pulados com -DskipTests para agilizar o build
# A instalação do Maven é mantida até aqui para que o comando mvn funcione
RUN mvn clean package -DskipTests

# (Opcional) Remove o Maven após a compilação para reduzir o tamanho da imagem final
# Isso pode ser feito no final do estágio de build
RUN apk del maven


# --- Estágio de Runtime ---
FROM eclipse-temurin:21-jre-alpine

# Define o diretório de trabalho dentro do container de runtime
WORKDIR /app

# Copia o JAR gerado no estágio de build para o diretório de trabalho do runtime
# O padrão *.jar garante que qualquer JAR gerado (independentemente do nome exato) seja copiado
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta 8080, onde o Spring Boot geralmente roda
EXPOSE 8080

# Comando para executar o JAR. A variável OPENROUTER_API_KEY deve ser definida no ambiente de execução.
ENTRYPOINT ["java", "-jar", "app.jar"]