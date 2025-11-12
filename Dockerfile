# Usar imagem oficial do Java 21
FROM eclipse-temurin:21-jdk-alpine AS build

# Diretório de trabalho
WORKDIR /app

# Copiar arquivos Maven
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Dar permissão de execução
RUN chmod +x mvnw

# Baixar dependências
RUN ./mvnw dependency:go-offline

# Copiar código-fonte
COPY src src

# Compilar o projeto
RUN ./mvnw clean package -DskipTests

# ========== ETAPA 2: IMAGEM FINAL ==========
FROM eclipse-temurin:21-jre-alpine

# Diretório de trabalho
WORKDIR /app

# Copiar JAR da etapa de build
COPY --from=build /app/target/*.jar app.jar

# Expor porta 8080
EXPOSE 8080

# Comando para executar
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]