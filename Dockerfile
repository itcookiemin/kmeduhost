# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# 의존성 캐시 레이어 (pom.xml 변경 시만 재다운로드)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B 2>/dev/null || \
    (apk add --no-cache maven && mvn dependency:go-offline -B)

COPY src ./src

RUN apk add --no-cache maven && \
    mvn package -DskipTests -B && \
    mv target/*.jar app.jar

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 보안: non-root 사용자
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder /app/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
