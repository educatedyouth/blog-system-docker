# 1. 基础镜像：我们要在一个安装了 Java 17 的 Linux 环境下运行
#    (这是一个官方提供的精简版 OpenJDK 17 镜像)
FROM openjdk:17-jdk-slim

# 2. 维护者信息 (可选)
LABEL maintainer="Hzj <Hzj@abcdefg.com>"

# 3. 设置工作目录：在容器内部，我们将在这个目录下工作
WORKDIR /app

# 4. 复制文件：把我们电脑上的 JAR 包，复制进容器里的 /app 目录下
#    并重命名为 app.jar
COPY target/demo-0.0.1-SNAPSHOT.jar app.jar

# 5. 暴露端口：告诉 Docker，这个应用会占用 8081 端口
EXPOSE 8081

# 6. 启动命令：容器启动时，自动执行这行命令
#    等同于你在终端输入 java -jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]