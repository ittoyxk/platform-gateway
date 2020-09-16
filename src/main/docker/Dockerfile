FROM alpine-oraclejdk8:slim as builder

WORKDIR application

COPY platform-gateway.jar platform-gateway.jar
RUN java -Djarmode=layertools -jar platform-gateway.jar extract

FROM alpine-oraclejdk8:slim

COPY gosu/gosu-amd64 /usr/bin/gosu
RUN chmod +x /usr/bin/gosu

COPY entrypoint.sh /application/entrypoint.sh
RUN chmod +x /application/entrypoint.sh

WORKDIR application

COPY --from=builder application/dependencies/ /application
COPY --from=builder application/spring-boot-loader/ /application
COPY --from=builder application/snapshot-dependencies/ /application
COPY --from=builder application/application/ /application

ENTRYPOINT ["/application/entrypoint.sh"]