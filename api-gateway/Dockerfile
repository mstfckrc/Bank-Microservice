# 1. AŞAMA: İnşa ve Önbellek
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# 2. AŞAMA: Hafif Çalıştırma
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# 🚀 YENİ EKLENEN KISIM: Kök Sertifikayı Java'nın Kasasına (Truststore) Zorla Enjekte Et!
USER root
COPY rootCA.pem /app/rootCA.pem
RUN keytool -importcert -trustcacerts -cacerts -storepass changeit -noprompt -alias secdemobank -file /app/rootCA.pem

ENTRYPOINT ["java", "-jar", "app.jar"]