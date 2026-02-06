# ============================
# 1) BUILD STAGE (Maven)
# ============================
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copier tout le projet backend (pom.xml + src + resources + config...)
COPY backend/ .

# Compiler le projet sans tests
RUN mvn clean package -DskipTests


# ============================
# 2) RUN STAGE (Java Runtime)
# ============================
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copier le fichier jar généré depuis l’étape build
COPY --from=build /workspace/target/*.jar app.jar

# Exposer le port utilisé par Spring Boot
EXPOSE 8080

# Lancer l'application Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
