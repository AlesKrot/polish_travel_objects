FROM gradle:8.5-jdk17 AS build

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

RUN ./gradlew :server:installDist --no-daemon

FROM openjdk:17-slim

WORKDIR /app
COPY --from=build /home/gradle/src/server/build/install/server /app

EXPOSE 8080
CMD ["./bin/server"]
