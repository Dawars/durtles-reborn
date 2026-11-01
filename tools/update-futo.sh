#!/bin/sh
# Moves the one-commit field language patch onto a newer FUTO release, then rebuilds.
# A conflict stops the rebase so it can be resolved by hand; it should only touch the added hooks
# in LatinIME.kt and JapaneseIME.kt. usage: update-futo.sh <output.apk> [tag], default the newest stable tag.
set -eu
OUT=$1
HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
FUTO=${FUTO_DIR:-$HOME/projects/futo-keyboard}
cd "$FUTO"
git fetch --quiet --tags origin
TAG=${2:-$(git tag --sort=-creatordate | grep -vE -- '-(rc|beta|alpha)' | head -1)}
echo "Rebasing onto $TAG"
git rebase --onto "$TAG" field-language-hint~1 field-language-hint
git submodule update --quiet --init --recursive
git submodule foreach --quiet --recursive 'git lfs pull'
"$HERE/futo-build.sh" "$OUT"
