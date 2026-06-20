ARG DOCKER_REGISTRY=docker.io

FROM ${DOCKER_REGISTRY}/library/maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY settings.xml /root/.m2/settings.xml

COPY pom.xml .

COPY src ./src

RUN mvn package -Dmaven.test.skip=true -B

FROM ${DOCKER_REGISTRY}/library/eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8103

ENTRYPOINT ["java", "-jar", "app.jar"]
