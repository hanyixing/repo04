# syntax=docker/dockerfile:1
# ============================================================
# newbee-mall 多阶段构建：构建阶段编译打包，运行阶段仅含 JRE 与可执行 jar，
# 镜像更小且不含构建工具，符合生产精简与安全要求。
# ============================================================

# ---------- 构建阶段 ----------
FROM maven:3.8-openjdk-8 AS builder
WORKDIR /build
# 先复制 pom，单独缓存依赖下载层，加速后续构建
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true
# 复制源码并以生产 profile 打包（测试在 CI 阶段执行，此处跳过以加速镜像构建）
COPY src ./src
RUN mvn -B -Pprod clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:8-jre
LABEL maintainer="newbee-mall" \
      description="newbee-mall e-commerce application"
WORKDIR /app
# 创建非 root 运行用户，降低容器逃逸风险
RUN groupadd -r app && useradd -r -g app app \
    && mkdir -p /app/logs \
    && chown -R app:app /app
COPY --from=builder /build/target/newbee-mall.jar /app/app.jar
RUN chown app:app /app/app.jar
USER app
EXPOSE 28089
# 默认生产 profile，可被 docker-compose / docker run -e 覆盖
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms256m -Xmx512m"
# 使用 exec 让 java 进程成为 PID 1，正确接收停止信号
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
