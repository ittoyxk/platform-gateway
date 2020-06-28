FROM alpine-oraclejdk8:slim

COPY gosu/gosu-amd64 /usr/bin/gosu
RUN chmod +x /usr/bin/gosu

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

COPY ./libs libs
COPY @project.build.finalName@.jar @project.build.finalName@.jar

ENTRYPOINT ["/entrypoint.sh"]