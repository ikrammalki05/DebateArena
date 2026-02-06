FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY backend/pom.xml .
RUN mvn dependency:go-offline

COPY backend/src ./src

# Build du projet (sans exécuter les tests ici)
RUN mvn -DskipTests package -e -X

FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Copier le fichier .jar depuis l’étape de build
COPY --from=build /workspace/target/*.jar app.jar

# Exposer le port Spring Boot
EXPOSE 8080

# Lancer l’application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

# # Backend placeholder
# FROM eclipse-temurin:17-jdk

# WORKDIR /app

# COPY . .

# EXPOSE 8080

# # Commande placeholder pour rester en vie
# CMD ["sh", "-c", "echo Backend placeholder running && tail -f /dev/null"]
