# 后端应用 Dockerfile - 安全加固版
FROM openjdk:17-jdk-slim

# 创建非root用户
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 设置工作目录
WORKDIR /app

# 安装必要的工具
RUN apt-get update && apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get clean

# 复制Maven包装器和POM文件
COPY backend/mvnw backend/mvnw.cmd backend/pom.xml ./
COPY backend/.mvn .mvn

# 下载依赖
RUN ./mvnw dependency:go-offline -B

# 复制源代码
COPY backend/src src

# 构建应用
RUN ./mvnw clean package -DskipTests

# 创建应用目录并设置权限
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# 切换到非root用户
USER appuser

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# 启动命令 - 增加安全JVM参数
CMD ["java", \
     "-Djava.security.egd=file:/dev/./urandom", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-Dfile.encoding=UTF-8", \
     "-Djava.awt.headless=true", \
     "-Djava.security.policy=all.policy", \
     "-jar", "target/login-security-monitor-backend-1.0.0.jar"]