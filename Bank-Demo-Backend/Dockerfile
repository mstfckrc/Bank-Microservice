# 1. AŞAMA: Derleme (Build) - Kodu makine diline çeviriyoruz
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Sadece pom.xml'i kopyalayıp bağımlılıkları indiriyoruz (Docker Cache avantajı)
COPY pom.xml .
RUN mvn dependency:go-offline

# Kaynak kodları kopyalayıp projeyi derliyoruz (.jar dosyası üretiyoruz)
COPY src ./src
RUN mvn clean package -DskipTests

# 2. AŞAMA: Çalıştırma (Run) - Sadece çalışan incecik bir sistem kuruyoruz
FROM eclipse-temurin:21-jre
WORKDIR /app

# 1. aşamada üretilen o .jar dosyasını bu yeni odaya kopyalıyoruz
COPY --from=build /app/target/*.jar app.jar

# 8080 portundan dışarıya yayın yapacağımızı belirtiyoruz
EXPOSE 8080

# Konteyner uyanınca bu komutu çalıştır diyoruz
ENTRYPOINT ["java", "-jar", "app.jar"]