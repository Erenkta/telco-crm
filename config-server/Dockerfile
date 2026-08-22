# 1. Aşama: Derleme (Build)
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# 1. Root Parent POM'u kopyala
COPY pom.xml .

# 2. Maven Reactor'ın patlamaması için diğer alt modüllerin SADECE pom.xml'lerini kopyala
COPY gateway/pom.xml ./gateway/
COPY naming-server/pom.xml ./naming-server/

# 3. Config Server'ın hem pom.xml hem de kaynak kodlarını kopyala
COPY config-server/pom.xml ./config-server/
COPY config-server/src ./config-server/src

# 4. Sadece config-server modülünü derle
RUN mvn clean package -pl config-server -DskipTests

# 2. Aşama: Çalıştırma (Runtime)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

COPY --from=builder /app/config-server/target/*.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]