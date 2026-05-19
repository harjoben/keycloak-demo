FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/calendar.jar /app/calendar.jar
VOLUME ["/app/data"]
EXPOSE 9080
CMD ["java", "-jar", "/app/calendar.jar"]
