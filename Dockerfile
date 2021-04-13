FROM adoptopenjdk:15-jdk-hotspot-bionic

RUN mkdir /app

WORKDIR /app

COPY build/libs/*.jar ./

CMD ["java", "-jar", "app.jar"]