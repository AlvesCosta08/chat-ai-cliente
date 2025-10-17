# --- Build Stage ---
FROM openjdk:21-jdk-slim AS build
WORKDIR /app
# Copia o pom.xml primeiro para aproveitar o cache
COPY pom.xml .
# Instala o Maven e baixa dependências
RUN apt-get update && \
    apt-get install -y --no-install-recommends maven && \
    mvn dependency:go-offline -B && \
    apt-get purge -y maven && \
    apt-get autoremove -y && \
    apt-get clean

# Copia o código fonte
COPY src ./src
# Compila o projeto, pulando testes por enquanto
RUN mvn clean package -DskipTests

# --- Runtime Stage ---
FROM openjdk:21-jre-slim
WORKDIR /app

# Encontra o JAR gerado no estágio de build e copia com um nome fixo
# Isso lida com o nome variável do JAR
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]