# ============================================================
# newbee-mall 多阶段构建
# 阶段 1: Maven 编译 → 阶段 2: JRE 运行
# ============================================================

# ---------- 构建阶段 ----------
FROM maven:3.8-openjdk-8 AS builder
WORKDIR /build

# 先复制 pom.xml，利用 Docker 缓存加速依赖下载
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并打包（使用生产 profile）
COPY src ./src
RUN mvn clean package -Pprod -DskipTests -B

# ---------- 运行阶段 ----------
FROM openjdk:8-jre-slim
LABEL maintainer="newbee-mall"

# 安装 curl（用于健康检查）并清理缓存
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# 创建非 root 用户运行应用
RUN groupadd -r app && useradd -r -g app app

WORKDIR /app

# 从构建阶段复制 jar
COPY --from=builder /build/target/newbee-mall-*.jar app.jar

# 日志目录
RUN mkdir -p /app/logs && chown -R app:app /app

USER app

EXPOSE 28089

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:28089/admin/login || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]
