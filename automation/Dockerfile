FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN mvn clean -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/automation-1.0-SNAPSHOT.jar app.jar

EXPOSE 8000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
