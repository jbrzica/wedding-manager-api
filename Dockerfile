# 1. Angular frontend
FROM node:22-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npx ng build

# 2. Spring Boot, s ugrađenim frontendom u static/
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
COPY --from=frontend /frontend/dist/frontend/browser/ src/main/resources/static/
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew bootJar --no-daemon

# 3. Samo ono što treba za pokretanje
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
