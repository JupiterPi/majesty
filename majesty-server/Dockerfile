FROM gradle:jdk21 as build
WORKDIR /app
COPY . .
RUN gradle shadowJar

FROM amazoncorretto:21-alpine
WORKDIR /app
ENV port=80
COPY --from=build /app/build/libs/majesty-server-all.jar /app/majesty-server.jar
CMD ["java", "-jar", "majesty-server.jar"]