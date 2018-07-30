FROM openjdk

RUN mkdir -p /griffin
ADD ./service/target /griffin

EXPOSE 8080

ENTRYPOINT [ "java", "-jar", "/griffin/service-0.2.0-incubating-SNAPSHOT.jar" ]