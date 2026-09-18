# 中文日志乱码解决方案

## 问题描述
在启动Java应用时，中文字符在日志输出中显示为乱码，如：`????` 等。

## 解决方案

### 方案1：使用UTF-8启动脚本（推荐）

直接运行项目根目录下的启动脚本：
```bash
./start_with_utf8.sh
```

### 方案2：手动设置环境变量

在启动项目前设置以下环境变量：
```bash
export LANG=zh_CN.UTF-8
export LC_ALL=zh_CN.UTF-8
export MAVEN_OPTS="-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"

cd yudao-server
mvn spring-boot:run
```

### 方案3：IDE配置（IntelliJ IDEA）

在IDE中配置VM options：
```
-Dfile.encoding=UTF-8
-Dconsole.encoding=UTF-8
-Dsun.jnu.encoding=UTF-8
-Dspring.output.ansi.enabled=ALWAYS
```

### 方案4：系统级别配置

在 `~/.bashrc` 或 `~/.profile` 中添加：
```bash
export LANG=zh_CN.UTF-8
export LC_ALL=zh_CN.UTF-8
```

## 配置文件修改

本解决方案已经对以下配置文件进行了修改：

1. **logback-spring.xml** - 添加了 `<charset>UTF-8</charset>` 配置
2. **application-local.yaml** - 添加了日志字符集配置
3. **application.yaml** - 确认servlet编码配置正确

## 验证方法

启动项目后，在日志中应该能看到正确的中文字符而不是乱码。

## 常见问题

### Q: 为什么有些终端仍然显示乱码？
A: 确保你的终端本身支持UTF-8编码，可以在终端设置中修改字符编码为UTF-8。

### Q: Docker环境下如何解决？
A: 在Dockerfile中添加：
```dockerfile
ENV LANG=zh_CN.UTF-8
ENV LC_ALL=zh_CN.UTF-8
```

### Q: 数据库中文乱码如何解决？
A: 确保MySQL数据库使用utf8mb4字符集，连接URL中包含 `?characterEncoding=UTF-8&useUnicode=true` 