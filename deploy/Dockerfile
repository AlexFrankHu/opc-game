## syntax=docker/dockerfile:1
##
## Last Bastion server image — multi-stage build.
##
## 用法（在仓库根执行）：
##     docker build -f deploy/Dockerfile -t lastbastion-app:dev .

# ---------- stage 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY assets/numeric/ assets/numeric/
COPY SourceCode/server/ SourceCode/server/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -f SourceCode/server/pom.xml clean package -DskipTests

# ---------- stage 2: runtime ----------
FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.source="https://github.com/AlexFrankHu/opc-game"
LABEL org.opencontainers.image.title="last-bastion-app"
LABEL org.opencontainers.image.description="Last Bastion ioGame + JSON gateway"

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
ENV STORE_KIND=file
ENV STORE_ROOT=/data/players
ENV LOGIN_SHARED_SECRET=""

RUN useradd -r -u 10001 lastbastion && mkdir -p /data/players /assets/numeric \
    && chown -R lastbastion:lastbastion /data /assets

COPY --from=build /workspace/SourceCode/server/app/target/last-bastion-app-fat.jar /app/last-bastion-app.jar
COPY --from=build /workspace/assets/numeric/ /assets/numeric/

USER lastbastion
WORKDIR /app
EXPOSE 10100 10110 10210 10199

HEALTHCHECK --interval=15s --timeout=3s --start-period=20s --retries=3 \
    CMD wget -qO- http://127.0.0.1:10199/healthz | grep -q ok || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS \
  -Dnumeric.dir=/assets/numeric \
  -Dstore.kind=$STORE_KIND \
  -Dstore.root=$STORE_ROOT \
  -jar /app/last-bastion-app.jar"]
