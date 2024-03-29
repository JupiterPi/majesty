# TODO: dockerize build

FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
ENV port=80
COPY build/libs/majesty-server-all.jar /app/majesty-server.jar
CMD ["java", "-jar", "majesty-server.jar"]