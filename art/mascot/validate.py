#!/usr/bin/env python3
"""Checks a mascot SVG against the subset described in README.md. Prints OK or one line per violation; exit 1 on violations."""
import re, sys
import xml.etree.ElementTree as ET
NS = '{http://www.w3.org/2000/svg}'
ALLOWED = {'svg', 'g', 'path', 'circle', 'ellipse', 'rect', 'line', 'polyline', 'polygon'}
TOKENS = {'fur', 'furLight', 'furDark', 'outline', 'ink', 'white', 'pink', 'shadow'}
COLOR_RE = re.compile(r'^(none|var\(--(\w+)\))$')
errors = []
def err(msg): errors.append(msg)
path = sys.argv[1]
root = ET.parse(path).getroot()
if root.tag != NS + 'svg' or root.get('viewBox', '').split() != ['0', '0', '160', '160']:
    err('root must be <svg xmlns=svg viewBox="0 0 160 160">')
for a in ('width', 'height', 'style'):
    if root.get(a) is not None: err(f'root must not set {a}')
count = 0
for el in root.iter():
    tag = el.tag.replace(NS, '')
    if tag not in ALLOWED: err(f'<{tag}> is not allowed'); continue
    if tag not in ('svg', 'g'): count += 1
    if el.get('transform'): err(f'<{tag} id={el.get("id")}> uses transform')
    if el.get('style'): err(f'<{tag} id={el.get("id")}> uses style attribute')
    for a in ('fill', 'stroke'):
        v = el.get(a)
        if v is None: continue
        m = COLOR_RE.match(v.strip())
        if not m or (m.group(2) and m.group(2) not in TOKENS): err(f'<{tag} id={el.get("id")}> {a}="{v}" is not a token')
    if tag == 'path':
        d = el.get('d', '')
        bad = set(re.findall(r'[A-DF-Za-df-z]', re.sub(r'[MLHVCSQTZmlhvcsqtz0-9.,\s\-eE+]', '', d)))
        if bad or re.search(r'[Aa]', re.sub(r'[0-9.,\s\-eE+]', '', d)): err(f'<path id={el.get("id")}> uses unsupported commands {bad or "A"}')
    if tag in ('path', 'polygon', 'circle', 'ellipse', 'rect') and el.get('fill') is None:
        err(f'<{tag} id={el.get("id")}> has no fill attribute; set fill="none" or a token')
    if tag in ('path', 'line', 'polyline', 'polygon', 'circle', 'ellipse', 'rect'):
        sw = el.get('stroke-width')
        if el.get('stroke') not in (None, 'none') and sw is not None and float(sw) < 1.2: err(f'<{tag} id={el.get("id")}> stroke-width {sw} < 1.2')
        for a in ('stroke-linecap', 'stroke-linejoin'):
            if el.get(a): err(f'<{tag} id={el.get("id")}> sets {a}; strokes are always round')
if count > 40: err(f'{count} elements, budget is 40')
groups = {g.get('id'): g for g in root.iter(NS + 'g') if g.get('id')}
for gid in ('tail', 'body', 'head', 'eyes'):
    if gid not in groups: err(f'missing <g id="{gid}">')
for gid in ('tail', 'eyes'):
    if gid in groups and not re.match(r'^-?[\d.]+\s*,\s*-?[\d.]+$', groups[gid].get('data-pivot', '')): err(f'<g id="{gid}"> needs data-pivot="x,y"')
def contains(outer, inner): return inner is not None and any(c is inner for c in outer.iter())
if 'body' in groups and 'head' in groups and not contains(groups['body'], groups['head']): err('<g id="head"> must be inside <g id="body">')
if 'head' in groups and 'eyes' in groups and not contains(groups['head'], groups['eyes']): err('<g id="eyes"> must be inside <g id="head">')
top = [c for c in root if c.tag != NS + 'g']
for c in top:
    if c.tag != NS + 'ellipse' or c.get('fill') != 'var(--shadow)': err(f'only a shadow ellipse may sit outside the groups, found <{c.tag.replace(NS, "")}>')
print('OK' if not errors else '\n'.join(errors)); sys.exit(1 if errors else 0)
