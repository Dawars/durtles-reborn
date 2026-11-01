#!/bin/sh
# usage: build.sh <name> <host-src-dir> <gradle args...>
# Copies source in through the Docker daemon and runs Gradle in the container; the Mac is never mounted.
set -eu
NAME=$1; SRC=$2; shift 2
docker exec wk-builder sh -c "mkdir -p /work/$NAME && cd /work/$NAME && find . -mindepth 1 -maxdepth 1 ! -name build ! -name .gradle ! -name .kotlin -exec rm -rf {} + ; rm -rf /work/$NAME/app/src"
COPYFILE_DISABLE=1 tar -C "$SRC" --no-xattrs --no-mac-metadata --exclude .git --exclude build --exclude .gradle --exclude .kotlin --exclude local.properties --exclude .idea --exclude .DS_Store -cf - . | docker cp -q - "wk-builder:/work/$NAME"
docker exec wk-builder sh -c "cd /work/$NAME && echo sdk.dir=/sdk > local.properties && sh ./gradlew --console=plain $*"
