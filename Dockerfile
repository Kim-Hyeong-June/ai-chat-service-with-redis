# Dockerfile

# 1단계: 빌드 전용
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

# 2단계: 실행 전용
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
