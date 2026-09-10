"""
Geometric QA for the deck.

A companion to the visual render, not a replacement for it — the render is the authority, and
it caught defects this could not (see the note on font metrics below). This runs first because
it is fast and it names the offending shape, straight off the real .pptx:

  1. every shape sits inside the slide,
  2. content keeps a 0.5" margin (a few shapes bleed on purpose and are whitelisted),
  3. text actually fits the box it was put in, measured with a metric-compatible font,
  4. no two text boxes overlap.

Text width is measured with Liberation Sans/Serif. Calibri is ~8% narrower than Arial and
Liberation Sans is Arial-metric, so Calibri widths are scaled down; a safety factor is applied
on top so a "fits" verdict is conservative rather than optimistic.

Trust that verdict only so far. Liberation Serif stands in for Cambria and runs narrower than
the real face, so this check once passed five headings that actually wrapped onto a second line
and collided with the sub-line beneath them. Always look at the rendered pages as well.
"""
from pptx import Presentation
from pptx.util import Emu
from PIL import ImageFont
import sys

EMU = 914400.0
SLIDE_W, SLIDE_H = 10.0, 5.625
MARGIN = 0.5

SANS = "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"
SANS_B = "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"
SERIF_B = "/usr/share/fonts/truetype/liberation/LiberationSerif-Bold.ttf"
SERIF = "/usr/share/fonts/truetype/liberation/LiberationSerif-Regular.ttf"

# Calibri runs narrower than Arial; Cambria a touch wider than Times.
WIDTH_FACTOR = {"Calibri": 0.92, "Cambria": 1.06}
SLACK = 1.06  # treat boxes as 6% tighter than they are, so "fits" is a conservative verdict

_cache = {}


def font(face, size_pt, bold):
    key = (face, round(size_pt), bold)
    if key not in _cache:
        if face == "Cambria":
            path = SERIF_B if bold else SERIF
        else:
            path = SANS_B if bold else SANS
        _cache[key] = ImageFont.truetype(path, max(6, int(round(size_pt))))
    return _cache[key]


def text_width_in(s, face, size_pt, bold):
    f = font(face, size_pt, bold)
    px = f.getlength(s)
    return (px / size_pt) * (size_pt / 72.0) * WIDTH_FACTOR.get(face, 1.0)


def wrapped_lines(text, box_w_in, face, size_pt, bold):
    """Greedy wrap, mirroring how PowerPoint breaks on spaces."""
    usable = box_w_in / SLACK
    lines = 0
    for para in text.split("\n"):
        words = para.split(" ")
        cur = ""
        n = 1
        for w in words:
            trial = w if not cur else cur + " " + w
            if text_width_in(trial, face, size_pt, bold) <= usable:
                cur = trial
            else:
                n += 1
                cur = w
        lines += n
    return lines


# Shapes allowed outside the safe margin: the decorative circles that bleed off the title and
# closing slides by design.
BLEED_OK = {1, 15}

problems = []
info = []

prs = Presentation(sys.argv[1] if len(sys.argv) > 1 else "SitePulse_HR_Presentation.pptx")

for idx, slide in enumerate(prs.slides, start=1):
    boxes = []
    for sh in slide.shapes:
        if sh.left is None:
            continue
        x, y = sh.left / EMU, sh.top / EMU
        w, h = (sh.width or 0) / EMU, (sh.height or 0) / EMU

        # 1 — inside the slide
        if x < -0.01 or y < -0.01 or x + w > SLIDE_W + 0.01 or y + h > SLIDE_H + 0.01:
            if idx not in BLEED_OK:
                problems.append(
                    f"slide {idx}: shape off-slide  x={x:.2f} y={y:.2f} w={w:.2f} h={h:.2f}"
                )

        # 2 — margin
        if idx not in BLEED_OK:
            if x < MARGIN - 0.01 or y < 0.3 - 0.01 or x + w > SLIDE_W - MARGIN + 0.01 or y + h > SLIDE_H - 0.25 + 0.01:
                problems.append(
                    f"slide {idx}: shape breaks margin  x={x:.2f} y={y:.2f} "
                    f"right={x + w:.2f} bottom={y + h:.2f}"
                )

        if not sh.has_text_frame:
            continue
        text = sh.text_frame.text
        if not text.strip():
            continue

        # 3 — text fits
        runs = [r for p in sh.text_frame.paragraphs for r in p.runs]
        if not runs:
            continue
        r0 = runs[0]
        size_pt = (r0.font.size.pt if r0.font.size else 18.0)
        face = r0.font.name or "Calibri"
        bold = bool(r0.font.bold)

        lines = wrapped_lines(text, w, face, size_pt, bold)
        line_h = size_pt * 1.22 / 72.0  # line box incl. leading
        needed = lines * line_h
        if needed > h + 0.02:
            problems.append(
                f"slide {idx}: TEXT OVERFLOW  {needed:.2f}in needed vs {h:.2f}in box "
                f"({lines} lines @ {size_pt:.0f}pt) :: {text[:58]!r}"
            )
        elif needed > h * 0.94:
            info.append(
                f"slide {idx}: tight ({needed:.2f}/{h:.2f}in) :: {text[:44]!r}"
            )

        boxes.append((x, y, w, h, text[:34]))

    # 4 — text box overlaps
    for i in range(len(boxes)):
        for j in range(i + 1, len(boxes)):
            ax, ay, aw, ah, at = boxes[i]
            bx, by, bw, bh, bt = boxes[j]
            ox = min(ax + aw, bx + bw) - max(ax, bx)
            oy = min(ay + ah, by + bh) - max(ay, by)
            if ox > 0.06 and oy > 0.06:
                problems.append(
                    f"slide {idx}: TEXT OVERLAP {ox:.2f}x{oy:.2f}in :: {at!r} / {bt!r}"
                )

print(f"slides: {len(prs.slides.__iter__.__self__._sldIdLst)}")
print(f"\n--- PROBLEMS ({len(problems)}) ---")
for p in problems:
    print(" ", p)
print(f"\n--- tight but OK ({len(info)}) ---")
for i in info:
    print(" ", i)
