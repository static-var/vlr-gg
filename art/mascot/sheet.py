#!/usr/bin/env python3
"""Renders mascot SVGs (viewBox 0 0 160 160, colours as var(--token)) into one comparison PNG.
Per candidate: light and dark rows, each with the 160 figure, the 88 About size, the card peek crop at 3x
device density (60x48 px) and at 1.5x (30x24), and the celebration pose (rotated -45 deg at the right edge).
usage: sheet.py out.png label1=cand1.svg [label2=cand2.svg ...]"""
import os, re, subprocess, sys, tempfile

PALETTES = {
  'light': dict(bg='#FFFFFF', surface='#F5F5F5', fur='#9E6EEF', furLight='#DAC8F9', furDark='#6E4CAF',
                outline='#381A6B', ink='#2A2233', white='#FFFFFF', pink='#F2A7B8',
                contentPrimary='#000000', shadow='rgba(0,0,0,0.08)', text='#000000'),
  'dark': dict(bg='#0A0A0A', surface='#141414', fur='#7E6ABA', furLight='#B8ADD9', furDark='#4F4177',
               outline='#D7CBFD', ink='#1E1826', white='#FFFFFF', pink='#E9A3AD',
               contentPrimary='#FFFFFF', shadow='rgba(255,255,255,0.10)', text='#FFFFFF'),
}
CROP = '28 12 104 83'  # head peek region: x 28..132, y 12..95 (20:16 aspect), as MascotPeekWindow

def inner(svg_text):
    m = re.search(r'<svg[^>]*>(.*)</svg>\s*$', svg_text, re.S)
    if not m: raise SystemExit('candidate must be a single <svg viewBox="0 0 160 160"> element')
    return m.group(1)

def figure(body, x, y, size, view='0 0 160 160', extra=''):
    return f'<svg x="{x}" y="{y}" width="{size}" height="{size * (1 if view == "0 0 160 160" else 0.8)}" viewBox="{view}" preserveAspectRatio="xMidYMin slice" {extra}>{body}</svg>'

def row(body, label, theme, y):
    p = PALETTES[theme]
    out = [f'<g class="{theme}" transform="translate(0,{y})">',
           f'<rect width="{W}" height="{ROW}" fill="{p["bg"]}"/>',
           f'<text x="12" y="20" font-family="Helvetica" font-size="13" fill="{p["text"]}">{label} / {theme}</text>']
    # 160 figure on a surface card
    out.append(f'<rect x="12" y="30" width="176" height="176" rx="8" fill="{p["surface"]}"/>')
    out.append(figure(body, 20, 38, 160))
    # 88 figure (About screen)
    out.append(f'<rect x="200" y="30" width="104" height="104" rx="8" fill="{p["surface"]}"/>')
    out.append(figure(body, 208, 38, 88))
    # card peek crops: 60x48 (3x density) and 30x24 (1.5x); card edge below
    for cx, cw in ((330, 60), (410, 30)):
        ch = cw * 0.8
        out.append(f'<rect x="{cx - 12}" y="{40 + ch}" width="{cw + 60}" height="40" rx="6" fill="{p["surface"]}" stroke="{p["outline"]}" stroke-opacity="0.35"/>')
        out.append(f'<svg x="{cx}" y="40" width="{cw}" height="{ch}" viewBox="{CROP}" preserveAspectRatio="xMidYMin slice">{body}</svg>')
    # celebration pose: 160 figure rotated -45 deg, pushed 50 past the right edge of a 220x160 frame
    fx, fy = 200, 150
    out.append(f'<rect x="{fx}" y="{fy}" width="220" height="160" fill="{p["surface"]}"/>')
    out.append(f'<svg x="{fx}" y="{fy}" width="220" height="160" viewBox="0 0 220 160">'
               f'<g transform="translate(190,70) rotate(-45) translate(-80,-80)">{body}</g></svg>')
    out.append('</g>')
    return '\n'.join(out)

W, ROW = 440, 320
def main():
    out_png, cands = sys.argv[1], sys.argv[2:]
    css = ''.join('.%s{%s}' % (t, ';'.join(f'--{k}:{v}' for k, v in p.items())) for t, p in PALETTES.items())
    tiles = []
    for c in cands:
        label, path = c.split('=', 1)
        body = inner(open(path).read())
        for theme in ('light', 'dark'):
            tiles.append((body, label, theme))
    n = len(tiles)
    cols = min(n, max(1, round((n * ROW / W) ** 0.5)))
    rows = -(-n // cols)
    side = max(cols * W, rows * ROW)
    parts = []
    for i, (body, label, theme) in enumerate(tiles):
        x, y = (i % cols) * W, (i // cols) * ROW
        parts.append(f'<g transform="translate({x},0)">{row(body, label, theme, y)}</g>')
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{side}" height="{side}" viewBox="0 0 {side} {side}">'
           f'<style>{css} svg{{overflow:hidden}} path,line,polyline,polygon,circle,ellipse,rect{{stroke-linecap:round;stroke-linejoin:round}}</style><rect width="{side}" height="{side}" fill="#808080"/>{"".join(parts)}</svg>')
    tmp = tempfile.mkdtemp()
    src = os.path.join(tmp, 'sheet.svg'); open(src, 'w').write(svg)
    subprocess.run(['qlmanage', '-t', '-s', str(min(side * 2, 4000)), '-o', tmp, src], check=True, capture_output=True)
    os.replace(os.path.join(tmp, 'sheet.svg.png'), out_png)
    print(out_png, f'{cols}x{rows} tiles')
main()
