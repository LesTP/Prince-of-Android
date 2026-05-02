#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
C_WORKDIR="${C_WORKDIR:-/tmp/sdlpop-render}"
REF_DIR="$ROOT_DIR/SDLPoP/screenshots/reference"
KOTLIN_DIR="$ROOT_DIR/SDLPoP-kotlin/build/render"
DIFF_DIR="$ROOT_DIR/SDLPoP-kotlin/build/render/diff"

command -v compare >/dev/null
command -v gradle >/dev/null

rm -rf "$C_WORKDIR"
mkdir -p "$C_WORKDIR" "$REF_DIR" "$KOTLIN_DIR" "$DIFF_DIR"
cp "$ROOT_DIR/SDLPoP/prince" "$C_WORKDIR/prince"
chmod +x "$C_WORKDIR/prince"
ln -s "$ROOT_DIR/SDLPoP/data" "$C_WORKDIR/data"
ln -s "$ROOT_DIR/SDLPoP/SDLPoP.ini" "$C_WORKDIR/SDLPoP.ini"

for level in $(seq 1 14); do
  rm -rf "$C_WORKDIR/screenshots"
  mkdir -p "$C_WORKDIR/screenshots"
  SDL_VIDEODRIVER=offscreen SDL_AUDIODRIVER=dummy "$C_WORKDIR/prince" megahit "$level" --screenshot --screenshot-level >/tmp/sdlpop_screenshot_"$level".log || true
  cp "$C_WORKDIR/screenshots/screenshot_000.png" "$REF_DIR/level_$(printf '%02d' "$level")_c.png"
done

(
  cd "$ROOT_DIR/SDLPoP-kotlin"
  gradle run --args="render-level-screenshots --assets ../app/src/main/assets --out build/render --levels 1-14" --no-daemon
)

printf "level,absolute_error\n" > "$DIFF_DIR/summary.csv"
for level in $(seq 1 14); do
  level_id="$(printf '%02d' "$level")"
  ref="$REF_DIR/level_${level_id}_c.png"
  test="$KOTLIN_DIR/level_${level_id}_kotlin.png"
  diff="$DIFF_DIR/level_${level_id}_diff.png"
  ae="$(compare -metric AE "$ref" "$test" "$diff" 2>&1 >/dev/null || true)"
  printf "%s,%s\n" "$level_id" "$ae" | tee -a "$DIFF_DIR/summary.csv"
done
