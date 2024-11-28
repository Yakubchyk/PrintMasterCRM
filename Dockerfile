FROM gradle:7.6.0-jdk17 AS build

# Устанавливаем Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Создаем рабочую директорию
WORKDIR /app

# Копируем Gradle файл
COPY build.gradle .

# Копируем исходный код
COPY src/ ./src/

# Собираем проект
RUN mvn clean package -DskipTests=true

# Переходим на облегченный образ для выполнения
FROM openjdk:17-jdk-slim AS run

# Копируем результат сборки из предыдущего этапа
COPY --from=build /app/target/*.jar /app/app.jar

# Устанавливаем команду запуска
CMD ["java", "-jar", "/app/app.jar"]