"""
Lays the 16:9 deck onto A4 pages for printing.

The .pptx stays 16:9, because that is what a projector and a laptop screen are — reshaping the
deck to fit paper would waste half of every screen it is ever shown on. Printing is a separate
job, so it gets a separate file: each slide is scaled and centred on an A4 sheet, unchanged.

Two outputs:
  * ..._A4.pdf        one slide per landscape A4 sheet — the full-size read
  * ..._A4_handout.pdf  two slides per portrait A4 sheet — the meeting handout, half the paper

Nothing is re-rendered. The pages are the same vector content as the screen PDF, transformed,
so text stays selectable and sharp at any print resolution.
"""
import sys
from pypdf import PdfReader, PdfWriter, Transformation
from pypdf.generic import RectangleObject

PT = 72.0
A4_W, A4_H = 595.276, 841.890  # A4 portrait, in points
MARGIN = 28.0                   # ~10mm breathing room so nothing lands in the unprintable edge


def fit(src_w, src_h, box_w, box_h):
    """Largest scale that keeps the slide inside the box, and the offsets that centre it."""
    scale = min(box_w / src_w, box_h / src_h)
    return scale, (box_w - src_w * scale) / 2, (box_h - src_h * scale) / 2


def one_per_page(reader, writer):
    """One slide per landscape A4 sheet."""
    page_w, page_h = A4_H, A4_W  # landscape
    for src in reader.pages:
        sw = float(src.mediabox.width)
        sh = float(src.mediabox.height)
        scale, dx, dy = fit(sw, sh, page_w - 2 * MARGIN, page_h - 2 * MARGIN)

        dest = writer.add_blank_page(width=page_w, height=page_h)
        dest.merge_transformed_page(
            src, Transformation().scale(scale).translate(MARGIN + dx, MARGIN + dy)
        )


def two_per_page(reader, writer):
    """
    Two slides stacked on a portrait A4 sheet — the handout.

    A 16:9 slide across A4 portrait is limited by width, not height, so each one ends up much
    shorter than half the page. Centring within a half-page slot would therefore open a band of
    dead paper between the two. The pair is sized from the width and then centred as a block,
    which keeps them together and leaves the margin where it belongs — around the outside.
    """
    page_w, page_h = A4_W, A4_H
    gap = 30.0
    slide_w = page_w - 2 * MARGIN

    pages = list(reader.pages)
    for i in range(0, len(pages), 2):
        chunk = pages[i:i + 2]
        dest = writer.add_blank_page(width=page_w, height=page_h)

        # Every slide in this deck is the same size, so one height drives the whole block.
        first = chunk[0]
        scale = slide_w / float(first.mediabox.width)
        slide_h = float(first.mediabox.height) * scale
        block_h = slide_h * len(chunk) + gap * (len(chunk) - 1)
        top = (page_h + block_h) / 2  # y of the block's top edge, measuring upward

        for j, src in enumerate(chunk):
            s = slide_w / float(src.mediabox.width)
            h = float(src.mediabox.height) * s
            y = top - (j + 1) * h - j * gap
            dest.merge_transformed_page(
                src, Transformation().scale(s).translate(MARGIN, y)
            )


def build(src_path, out_path, layout):
    reader = PdfReader(src_path)
    writer = PdfWriter()
    layout(reader, writer)
    with open(out_path, "wb") as f:
        writer.write(f)
    print(f"{out_path}: {len(writer.pages)} A4 pages from {len(reader.pages)} slides")


if __name__ == "__main__":
    src = sys.argv[1] if len(sys.argv) > 1 else "docs/SitePulse_HR_Presentation.pdf"
    stem = src[:-4] if src.lower().endswith(".pdf") else src
    build(src, f"{stem}_A4.pdf", one_per_page)
    build(src, f"{stem}_A4_handout.pdf", two_per_page)
