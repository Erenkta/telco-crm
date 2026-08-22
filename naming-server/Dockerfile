# 1. Aşama: Derleme (Build)
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# 1. Root Parent POM'u kopyala
COPY pom.xml .

# 2. Maven Reactor doğrulamasını geçmek için diğer modüllerin SADECE pom.xml'lerini kopyala
COPY config-server/pom.xml ./config-server/
COPY gateway/pom.xml ./gateway/

# 3. Naming Server'ın hem pom.xml hem de kaynak kodlarını kopyala
COPY naming-server/pom.xml ./naming-server/
COPY naming-server/src ./naming-server/src

# 4. Sadece naming-server modülünü derle
RUN mvn clean package -pl naming-server -DskipTests

# 2. Aşama: Çalıştırma (Runtime)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

COPY --from=builder /app/naming-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]