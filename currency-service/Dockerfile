# Java 21 kullanıyoruz (Karargah ile aynı)
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
# Derlenen JAR dosyasını kopyala
COPY target/*.jar app.jar
# Askeri ayağa kaldır
ENTRYPOINT ["java", "-jar", "app.jar"]