FROM eclipse-temurin:17-jre-jammy

# 时区 / 编码
ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8 \
--add-opens java.base/java.lang=ALL-UNNAMED \
--add-opens java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens java.base/java.util=ALL-UNNAMED \
--add-opens java.base/java.text=ALL-UNNAMED \
--add-opens java.base/java.math=ALL-UNNAMED \
--add-opens java.base/java.time=ALL-UNNAMED \
--add-opens java.base/java.nio=ALL-UNNAMED \
--add-opens java.base/sun.nio.ch=ALL-UNNAMED \
--add-opens java.base/java.io=ALL-UNNAMED \
--add-opens java.base/java.net=ALL-UNNAMED \
--add-opens java.management/java.lang.management=ALL-UNNAMED"

# 为什么要 JRE 17 而不是 8：
#   项目 pom 声明 java.version=1.8，但 yudao-module-icbc/erp/enterprise 里实际用了
#   112 处 Set.of/List.of/Map.of（Java 9+ API）。本机用 JDK 21 编译 -target 1.8，
#   不会拦截新 API，于是产物是「Java 8 字节码 + Java 9+ 调用」。
#   在 JRE 8 上启动会崩：NoSuchMethodError: java.util.Set.of(...)（SellerPortalServiceImpl）。
#   本地开发实际跑的就是 JDK 21，故线上取 LTS 的 17 对齐。
#   根治办法是给 maven-compiler-plugin 加 <release>8</release> 并清掉这些调用，属独立议题。

WORKDIR /app

COPY app/yudao-server.jar /app/app.jar

RUN mkdir -p /app/logs /app/config /app/upload

EXPOSE 48080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
