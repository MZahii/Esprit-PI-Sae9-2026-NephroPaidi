FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package
RUN JAR_FILE=$(ls /app/target/*.jar | grep -v '\.original$' | head -n 1) && cp "$JAR_FILE" /app/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl python3 python3-pip python3-venv \
    && python3 -m venv /opt/ml-venv \
    && /opt/ml-venv/bin/pip install --no-cache-dir --upgrade pip \
    && /opt/ml-venv/bin/pip install --no-cache-dir pandas scikit-learn xgboost joblib numpy \
    && rm -rf /var/lib/apt/lists/*
ENV PATH="/opt/ml-venv/bin:${PATH}"
COPY --from=build /app/app.jar app.jar
EXPOSE 8092
ENTRYPOINT ["java","-jar","/app/app.jar"]
