FROM openjdk:14-alpine

RUN mkdir -p /opt/ritzel
WORKDIR /opt/ritzel
COPY target/ritzel-standalone.jar .

ENTRYPOINT ["java", "-jar", "ritzel-standalone.jar"]
