FROM eclipse-temurin:17-jdk-alpine

RUN mkdir /app

WORKDIR /app

COPY build/libs/*.jar ./

CMD ["java", "-jar", "app.jar"]