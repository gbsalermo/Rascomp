FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
RUN ./mvnw -B -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd --system rascomp \
    && useradd --system --gid rascomp --home-dir /app --shell /usr/sbin/nologin rascomp

COPY --from=build /workspace/target/*.jar /app/app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod 755 /app/docker-entrypoint.sh

ENV PORT=8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=55.0 -XX:InitialRAMPercentage=10.0 -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080
USER rascomp

ENTRYPOINT ["/app/docker-entrypoint.sh"]
