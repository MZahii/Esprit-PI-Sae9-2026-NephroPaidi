FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests -Dmaven.wagon.http.retryHandler.count=6 -Dmaven.wagon.http.pool=false org.apache.maven.plugins:maven-dependency-plugin:3.8.1:go-offline
COPY src ./src
RUN mvn -q -DskipTests -Dmaven.wagon.http.retryHandler.count=6 package
RUN JAR_FILE=$(ls /app/target/*.jar | grep -v '\.original$' | head -n 1) && cp "$JAR_FILE" /app/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java","-jar","/app/app.jar"]
