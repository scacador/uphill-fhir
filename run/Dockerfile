FROM gradle:8.2.1-jdk17-alpine AS build
RUN mkdir -p /app/src/
COPY --chown=gradle:gradle . /app/src/
WORKDIR /app/src/
RUN gradle build -x test --no-daemon

FROM openjdk:17-alpine
RUN mkdir -p /app/
WORKDIR /app/
COPY --from=build /app/src/build/libs/*.jar /app/challengeUpHill.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "challengeUpHill.jar"]