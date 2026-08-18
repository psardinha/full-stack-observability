FROM eclipse-temurin:25-jre

WORKDIR /deployments

COPY target/quarkus-app/ ./

RUN useradd --uid 1001 --create-home appuser

USER 1001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/deployments/quarkus-run.jar"]