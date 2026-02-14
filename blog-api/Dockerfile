FROM sbtscala/scala-sbt:eclipse-temurin-17.0.15_6_1.10.11_2.13.16 AS builder

WORKDIR /app

COPY project/build.properties project/plugins.sbt project/Dependencies.scala project/
COPY build.sbt .
RUN sbt update

COPY . .
RUN sbt stage

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=builder /app/target/universal/stage/ .

EXPOSE 9000

ENTRYPOINT ["bin/wps-blog"]
