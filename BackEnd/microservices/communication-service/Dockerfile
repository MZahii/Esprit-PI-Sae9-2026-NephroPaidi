FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package && \
    JAR_FILE="$(find /app/target -maxdepth 1 -type f -name '*.jar' \
      ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name '*.original' ! -name '*-tests.jar' \
      | head -n 1)" && \
    test -n "$JAR_FILE" && cp "$JAR_FILE" /app/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java","-jar","/app/app.jar"]
