FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml pom.xml
COPY src src

# Usamos sh para asegurar compatibilidad en alpine
RUN chmod +x mvnw && sh ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="karlasof71@gmail.com"

WORKDIR /
# El comodín * ayuda a que encuentre el jar sin importar la versión exacta
COPY --from=build /workspace/target/*.jar app.jar

# Render inyecta la variable $PORT, así que esto está perfecto
ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT:-8080} -jar /app.jar"]