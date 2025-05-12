# --- Build Stage ---
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY . .
RUN ./mvnw clean package -DskipTests

# --- Runtime Stage ---
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

RUN apk add --no-cache curl ffmpeg python3 \
 && curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp \
    -o /usr/local/bin/yt-dlp \
 && chmod +x /usr/local/bin/yt-dlp

COPY --from=build /app/target/*.jar app.jar

VOLUME /data/audio

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
