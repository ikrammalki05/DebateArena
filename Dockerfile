# ============================
# 1) BUILD STAGE (Maven)
# ============================
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY backend/ .

RUN mvn clean package -Dmaven.test.skip=true


# ============================
# 2) RUN STAGE (Java Runtime)
# ============================
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
