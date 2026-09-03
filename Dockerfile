# ==============================================================================
# Etapa 1: Construcción (Google Cloud Build compilará esto en sus servidores)
# ==============================================================================
FROM gradle:8.13-jdk17-alpine AS builder

WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . /home/gradle/src

# Genera el Fat JAR (el .jar con todas las dependencias empaquetadas dentro)
RUN gradle buildFatJar --no-daemon

# ==============================================================================
# Etapa 2: Imagen Final de Producción (Ultra ligera, basada en Alpine Linux)
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copia únicamente el archivo .jar compilado
COPY --from=builder /home/gradle/src/build/libs/*-all.jar /app/app.jar

# Google Cloud Run inyecta la variable de entorno PORT (por defecto 8080)
ENV PORT=8080
EXPOSE 8080

# Arranca el servidor
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
