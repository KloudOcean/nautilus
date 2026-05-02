#!/usr/bin/env bash
# Renders docs/assets/architecture.svg to docs/assets/architecture-linkedin.png
# at LinkedIn's preferred OG-image size (1200x630, 1.91:1) with white padding.
#
# Run from repo root:
#   ./scripts/render-architecture-linkedin.sh
#
# Requires: librsvg (`brew install librsvg` on macOS, `apt install librsvg2-bin` on Debian/Ubuntu).

set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
SRC="${ROOT}/docs/assets/architecture.svg"
DST="${ROOT}/docs/assets/architecture-linkedin.png"

if ! command -v rsvg-convert >/dev/null 2>&1; then
  echo "rsvg-convert not found. Install with:" >&2
  echo "  macOS:   brew install librsvg" >&2
  echo "  Linux:   apt-get install librsvg2-bin" >&2
  exit 1
fi

# 1200x630 canvas, source SVG (940x580 viewBox) scaled to 972x600 and centered.
rsvg-convert \
  --page-width 1200 --page-height 630 \
  --top 15 --left 114 \
  -w 972 -h 600 -a \
  -b '#FFFFFF' \
  -o "${DST}" \
  "${SRC}"

echo "Rendered ${DST}"
file "${DST}"
