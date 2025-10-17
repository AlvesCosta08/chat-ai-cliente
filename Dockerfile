# --- Build Stage ---
FROM eclipse-temurin:21-jdk-alpine AS build 
WORKDIR /app
# Copia o pom.xml primeiro para aproveitar o cache
COPY pom.xml .
# Instala o Maven e baixa dependências
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B && \
    apk del maven # Remove o Maven após o build para reduzir o tamanho da camada

# Copia o código fonte
COPY src ./src
# Compila o projeto, pulando testes por enquanto
RUN mvn clean package -DskipTests

# --- Runtime Stage ---
FROM eclipse-temurin:21-jre-alpine 
WORKDIR /app

# Encontra o JAR gerado no estágio de build e copia com um nome fixo
# Isso lida com o nome variável do JAR
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]