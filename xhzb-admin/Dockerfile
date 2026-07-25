# 基础镜像
FROM eclipse-temurin:17-jdk
# 设定时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 环境变量
ENV OSS_ACCESS_KEY_ID=LTAI5t9ZNBF3xPXD5YaKJBhY
ENV OSS_ACCESS_KEY_SECRET=LYGEOSNzumcAjXaB0AN6kmVWDeXo1A
ENV OPENAI_API_KEY=sk-ec32de355d65410e8888be274497024d


# 拷贝jar包
COPY xhzb-admin.jar /app.jar

# 暴露端口
EXPOSE 9000

# 入口
ENTRYPOINT ["java", "-jar", "/app.jar"]