# ============================
# 1) BUILD STAGE (Maven)
# ============================
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copier le projet backend
COPY backend/ .

# Build sans tests
RUN mvn clean package -DskipTests


# ============================
# 2) RUN STAGE (Java Runtime)
# ============================
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copier le jar généré
COPY --from=build /workspace/target/*.jar app.jar

# Railway fournit le port via $PORT
EXPOSE 8080

# Lancer Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
