# ============================
# 1) BUILD STAGE (Maven)
# ============================
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copier tout le projet backend
COPY backend/ .

# Compiler le projet en ignorant COMPLETEMENT les tests
RUN mvn clean package -Dmaven.test.skip=true


# ============================
# 2) RUN STAGE (Java Runtime)
# ============================
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copier le jar généré depuis l'étape build
COPY --from=build /workspace/target/*.jar app.jar

# Port Spring Boot
EXPOSE 8080

# Lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
