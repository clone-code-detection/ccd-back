# syntax=docker/dockerfile:1
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

RUN apt-get update && apt-get install -y maven
ENV PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games:/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/share/maven/bin"

COPY pom.xml ./
COPY src ./src

CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.profiles=dev"]
