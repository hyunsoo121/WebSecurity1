# ---------------------------------------------------------
# 빌드 스테이지: gradle:8-jdk17 태그로 변경 (멀티 플랫폼 지원)
# ---------------------------------------------------------
FROM gradle:8-jdk17 AS builder

WORKDIR /app

COPY gradlew .
COPY settings.gradle .
COPY build.gradle .
COPY gradle ./gradle

RUN chmod +x gradlew
RUN ./gradlew dependencies

COPY src ./src

RUN ./gradlew bootJar -x test

# ---------------------------------------------------------
# 실행 스테이지: Java 17 JRE 유지
# ---------------------------------------------------------
FROM eclipse-temurin:17-jre

RUN mkdir -p /app/uploads

COPY --from=builder /app/build/libs/*.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]