ARG DOCKER_REGISTRY=docker.io

FROM ${DOCKER_REGISTRY}/maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY settings.xml /root/.m2/settings.xml

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn package -DskipTests -B

FROM ${DOCKER_REGISTRY}/eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8103

ENTRYPOINT ["java", "-jar", "app.jar"]
