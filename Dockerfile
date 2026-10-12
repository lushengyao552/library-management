FROM eclipse-temurin:25-jre
WORKDIR /app
COPY target/library-management-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
