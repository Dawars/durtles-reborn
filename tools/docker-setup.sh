#!/bin/sh
# Creates an isolated Android build container: named volumes only, no bind mounts from the host.
# Set CMD_URL and CMD_SHA to the current Linux command-line tools from
# https://dl.google.com/android/repository/repository2-3.xml before running it.
set -eu
: "${CMD_URL:?set CMD_URL}" "${CMD_SHA:?set CMD_SHA}"
for v in wk-src wk-gradle wk-sdk wk-android; do docker volume create "$v" >/dev/null; done
docker rm -f wk-builder >/dev/null 2>&1 || true
docker run -d --name wk-builder --platform linux/amd64 --security-opt no-new-privileges \
  -v wk-src:/work -v wk-gradle:/root/.gradle -v wk-sdk:/sdk -v wk-android:/root/.android \
  -e ANDROID_HOME=/sdk -e "GRADLE_OPTS=-Dorg.gradle.jvmargs=-Xmx5g" eclipse-temurin:21-jdk sleep infinity >/dev/null
docker exec -e CMD_URL="$CMD_URL" -e CMD_SHA="$CMD_SHA" wk-builder sh -euc '
  apt-get update -qq >/dev/null && apt-get install -y -qq unzip >/dev/null
  if [ ! -x /sdk/cmdline-tools/latest/bin/sdkmanager ]; then
    curl -sS --fail -o /tmp/c.zip "$CMD_URL"
    [ "$(sha1sum /tmp/c.zip | cut -d" " -f1)" = "$CMD_SHA" ] || { echo CHECKSUM_FAILED; exit 1; }
    mkdir -p /sdk/cmdline-tools /tmp/c && unzip -q /tmp/c.zip -d /tmp/c && mv /tmp/c/cmdline-tools /sdk/cmdline-tools/latest
  fi
  (yes | /sdk/cmdline-tools/latest/bin/sdkmanager --licenses >/dev/null 2>&1) || true
  /sdk/cmdline-tools/latest/bin/sdkmanager --install "platforms;android-37.0" "build-tools;37.0.0" 2>&1 | tail -1
'
