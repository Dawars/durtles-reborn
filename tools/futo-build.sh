#!/bin/sh
# Builds the patched FUTO Keyboard in the wk-builder container (see docker-setup.sh) and prints the
# APK path. The container never sees the host's home folder: source goes in through the Docker daemon.
# usage: futo-build.sh <output.apk>
set -eu
OUT=$1
HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
FUTO=${FUTO_DIR:-$HOME/projects/futo-keyboard}
VERSION=$(git -C "$FUTO" describe --tags --abbrev=0)
# Without git-lfs some model files are small placeholders, and the keyboard crashes when it loads them.
if grep -rlI --exclude-dir=.git --exclude-dir=build -m1 'version https://git-lfs.github.com/spec' "$FUTO" >/dev/null; then
    echo "Git LFS placeholders found: install git-lfs and run 'git submodule foreach git lfs pull'" >&2
    exit 1
fi
docker exec wk-builder sh -c 'command -v python3 >/dev/null || (apt-get update -qq >/dev/null && apt-get install -y -qq python3 >/dev/null)'
docker exec wk-builder sh -c 'mkdir -p /work/futo && cd /work/futo && find . -mindepth 1 -maxdepth 1 ! -name build ! -name .gradle ! -name .kotlin ! -name .cxx -exec rm -rf {} +'
COPYFILE_DISABLE=1 tar -C "$FUTO" --no-xattrs --no-mac-metadata --exclude .git --exclude build --exclude .gradle --exclude .kotlin --exclude .cxx --exclude local.properties --exclude .idea --exclude .DS_Store -cf - . | docker cp -q - wk-builder:/work/futo
docker cp -q "$HERE/futo-personal.init.gradle" wk-builder:/work/futo/personal.init.gradle
docker exec -e VERSION_CODE=1 -e "VERSION_NAME=$VERSION-field-language" -e BRANCH_NAME=field-language-hint wk-builder sh -c "cd /work/futo && echo sdk.dir=/sdk > local.properties && sh ./gradlew --console=plain --init-script personal.init.gradle assembleStableRelease -Pandroid.injected.signing.store.file=/root/.android/debug.keystore -Pandroid.injected.signing.store.password=android -Pandroid.injected.signing.key.alias=androiddebugkey -Pandroid.injected.signing.key.password=android"
APK=$(docker exec wk-builder sh -c 'find /work/futo/build/outputs/apk -name "*.apk" -type f | head -1')
docker cp -q "wk-builder:$APK" "$OUT"
echo "$OUT"
