#!/usr/bin/env bash
# 本地 Maven 包装脚本：解决 Git Bash 下 POSIX 路径无法被 Windows java.exe 识别的问题。
# 用法：bash tools/mvn.sh <maven 参数>   （在 Maven 工程目录内执行）
export JAVA_HOME="/c/Program Files/Java/jdk-21"
export PATH="$JAVA_HOME/bin:$PATH"
M2="/c/Users/hc/Software/apache-maven-3.9.9"
M2W=$(cygpath -m "$M2")
JARW=$(cygpath -m "$M2/boot/plexus-classworlds-2.8.0.jar")
PWDW=$(cygpath -m "$PWD")
exec java -cp "$JARW" \
  -Dclassworlds.conf="$M2W/bin/m2.conf" \
  -Dmaven.home="$M2W" \
  -Dmaven.multiModuleProjectDirectory="$PWDW" \
  -Dlibrary.jansi.path="$M2W/lib/jansi-native" \
  org.codehaus.plexus.classworlds.launcher.Launcher "$@"
