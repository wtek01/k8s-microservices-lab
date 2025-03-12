# Étape de construction
FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /workspace/app

# Copier le pom parent et ses dépendances
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x ./mvnw

# Copier tous les modules mentionnés dans le POM parent
COPY user-service/pom.xml ./user-service/
COPY order-service/pom.xml ./order-service/

# Maintenant exécuter Maven
RUN ./mvnw dependency:go-offline -B -pl user-service

# Copier le code source et construire
COPY user-service/src ./user-service/src/
RUN ./mvnw package -DskipTests -pl user-service

# Étape de déploiement
FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp

# Copier l'application compilée
COPY --from=build /workspace/app/user-service/target/*.jar /app/app.jar

# Script wait-for-it pour attendre les dépendances
RUN echo '#!/bin/sh\n\
set -e\n\
\n\
host="$1"\n\
port="$2"\n\
shift 2\n\
cmd="$@"\n\
\n\
until nc -z "$host" "$port" || [ "$WAIT_TIMEOUT" = "0" ]; do\n\
  >&2 echo "Service on $host:$port is unavailable - sleeping"\n\
  sleep 1\n\
done\n\
\n\
>&2 echo "Service on $host:$port is up - executing command"\n\
exec $cmd' > /wait-for-it.sh && \
chmod +x /wait-for-it.sh

# Installer netcat pour le script wait-for-it
RUN apk add --no-cache netcat-openbsd

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Exécuter l'application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]