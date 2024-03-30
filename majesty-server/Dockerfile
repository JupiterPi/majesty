# TODO: dockerize build

FROM amazoncorretto:11-alpine
WORKDIR /app
ENV port=80
COPY build/libs/majesty-server-all.jar /app/majesty-server.jar
CMD ["java", "-jar", "majesty-server.jar"]