FROM alpine-oraclejdk8:slim as builder

WORKDIR application

COPY @project.build.finalName@.jar @project.build.finalName@.jar
RUN java -Djarmode=layertools -jar @project.build.finalName@.jar extract

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