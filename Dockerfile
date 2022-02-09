ARG APP_HOME=/home/gradle/src

FROM gradle:7.0.0-jdk11-hotspot AS source
ARG APP_HOME
WORKDIR $APP_HOME
COPY gradle $APP_HOME/gradle
COPY settings.gradle gradlew $APP_HOME/
COPY src $APP_HOME/src
COPY build.gradle $APP_HOME/build.gradle

FROM source AS build
RUN gradle clean build

FROM eclipse-temurin:11.0.12_7-jre
ARG APP_HOME
EXPOSE 8080
COPY --from=build $APP_HOME/build/libs/shadow.jar /shadow.jar
COPY --from=build $APP_HOME/src/main/resources/config.yml /config.yml
RUN mkdir -p /otel
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.10.1/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar
ADD https://github.com/aws-observability/aws-otel-java-instrumentation/releases/download/v1.10.1/aws-opentelemetry-agent.jar /otel/aws-opentelemetry-agent.jar
ENTRYPOINT ["java", "-jar", "shadow.jar", "server", "config.yml"]
