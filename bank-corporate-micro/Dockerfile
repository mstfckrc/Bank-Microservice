# Derleme: Maven/JDK yalnizca bu asamada kullanilir.
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Kaynak kod degisince bagimlilik hazirligi katmanini tekrar calistirma.
COPY pom.xml .
# Sekiz Java servisi indirilmis bagimliliklari paylasir.
# Cache uygulama verisi degildir; silinirse Maven tekrar indirir.
# sharing=locked yalnizca bu cache'i kullanan Maven adimlarini koordine eder.
RUN --mount=type=cache,id=bank-demo-maven-java21,target=/root/.m2,sharing=locked \
    mvn -B -ntp dependency:go-offline

COPY src ./src
# Testler Jenkins clean verify adiminda calisir; burada JAR paketlenir.
RUN --mount=type=cache,id=bank-demo-maven-java21,target=/root/.m2,sharing=locked \
    mvn -B -ntp -DskipTests clean package

# Calistirma: kaynak kod, Maven ve bagimlilik cache'i son imaja alinmaz.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
