# Derleme: Maven/JDK yalnizca bu asamada kullanilir.
FROM maven:3.9.16-eclipse-temurin-21@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320 AS build
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
FROM eclipse-temurin:21-jre@sha256:227118764cccd3c3294617061aaa93a08309c63a395eefa83e6b35032992ea99 AS runtime
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
