FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp
COPY target/*.jar app.jar
ENTRYPOINT ["sh", "-c",  "java", "-jar", "/app.jar"]