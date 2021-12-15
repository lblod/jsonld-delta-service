FROM maven:3.6.3-amazoncorretto-15 as builder
LABEL maintainer="info@redpencil.io"

WORKDIR /app

COPY pom.xml .

COPY .mvn .mvn

COPY settings.xml settings.xml

RUN mvn -B dependency:resolve-plugins dependency:resolve

COPY ./src ./src

RUN mvn package -DskipTests

FROM amazoncorretto:15

WORKDIR /app

COPY --from=builder /app/target/jsonld-delta-service.jar ./app.jar

ENTRYPOINT ["sh", "-c", "java -Dlog4j2.formatMsgNoLookups=true ${JAVA_OPTS} -jar /app/app.jar"]
