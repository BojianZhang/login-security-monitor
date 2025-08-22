# 多阶段构建
# 阶段1：构建Vue应用
FROM node:18-alpine AS builder

# 设置工作目录
WORKDIR /app

# 复制package.json和package-lock.json
COPY frontend/package*.json ./

# 安装依赖
RUN npm ci --only=production

# 复制源代码
COPY frontend/ .

# 构建应用
RUN npm run build

# 阶段2：运行时环境
FROM nginx:alpine

# 复制自定义nginx配置
COPY frontend/nginx.conf /etc/nginx/nginx.conf

# 复制构建好的应用
COPY --from=builder /app/dist /usr/share/nginx/html

# 创建nginx运行时所需的目录
RUN mkdir -p /var/cache/nginx/client_temp \
    && mkdir -p /var/cache/nginx/proxy_temp \
    && mkdir -p /var/cache/nginx/fastcgi_temp \
    && mkdir -p /var/cache/nginx/uwsgi_temp \
    && mkdir -p /var/cache/nginx/scgi_temp

# 设置正确的权限
RUN chown -R nginx:nginx /var/cache/nginx \
    && chown -R nginx:nginx /usr/share/nginx/html \
    && chmod -R 755 /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:80/ || exit 1

# 启动命令
CMD ["nginx", "-g", "daemon off;"]