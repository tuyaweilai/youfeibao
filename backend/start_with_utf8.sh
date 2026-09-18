#!/bin/bash

# 芋道项目UTF-8启动脚本
# 解决中文日志乱码问题

echo "启动芋道项目，使用UTF-8字符编码..."

# 设置环境变量
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8

# JVM 参数设置
export MAVEN_OPTS="-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dspring.profiles.active=local -Xms512m -Xmx2048m -Dspring.output.ansi.enabled=ALWAYS"

echo "环境变量设置:"
echo "LANG=$LANG"
echo "LC_ALL=$LC_ALL" 
echo "MAVEN_OPTS=$MAVEN_OPTS"

# 启动项目
echo "正在启动项目..."
cd yudao-server
mvn spring-boot:run 