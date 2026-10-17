#!/usr/bin/env python3
"""Converts a mascot SVG (viewBox 0 0 160 160, token colours, tail/body/paws/head/eyes groups) into a
Kotlin file holding the mascot's Paths and one draw function per layer.

usage: svg2compose.py <mascot.svg> <ClassName> <out.kt>
Emits `internal object <ClassName>Art : MascotArt` with one `Path` val per shape, one `Stroke` val per distinct
stroke width, `tailPivot`/`eyesPivot`/`pawsBounds`, and
`DrawScope.drawShadow/drawTail/drawBody/drawPaws/drawHead/drawEyes/drawFace(palette)` functions.
Shapes: path (M L H V C S Q T Z, abs/rel), circle, ellipse, rect(rx), line, polyline, polygon."""
import re, sys
import xml.etree.ElementTree as ET

NS = '{http://www.w3.org/2000/svg}'
TOKENS = {'fur', 'furLight', 'furDark', 'outline', 'ink', 'white', 'pink', 'shadow'}

def fmt(v):
    return ('%.2f' % round(float(v), 2)).rstrip('0').rstrip('.') + 'f'

def tokenize_d(d):
    return re.findall(r'[MLHVCSQTZmlhvcsqtz]|-?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?', d)

def draw_call(*args):
    """One drawPath call, wrapped one argument per line when it would pass the 120 column ktlint limit."""
    line = f'    drawPath({", ".join(args)})'
    if len(line) <= 120: return line
    return '    drawPath(\n' + ''.join(f'      {a},\n' for a in args) + '    )'

def path_commands(d):
    """Returns Kotlin Path builder lines for an SVG path, with S/T/H/V and relatives resolved."""
    toks = tokenize_d(d); i = 0; cmd = None
    cx = cy = sx = sy = 0.0; lcx = lcy = None; lqx = lqy = None; lines = []
    def take(n):
        nonlocal i
        vals = [float(t) for t in toks[i:i + n]]; i += n; return vals
    while i < len(toks):
        if re.match(r'[A-Za-z]', toks[i]): cmd = toks[i]; i += 1
        elif cmd is None: raise SystemExit('path data must start with a command')
        rel = cmd.islower(); c = cmd.upper()
        if c == 'Z':
            lines.append('close()'); cx, cy = sx, sy; lcx = lcy = lqx = lqy = None
            continue
        if c == 'M':
            x, y = take(2)
            if rel: x += cx; y += cy
            lines.append(f'moveTo({fmt(x)}, {fmt(y)})'); cx, cy = sx, sy = x, y; cmd = 'l' if rel else 'L'
            lcx = lcy = lqx = lqy = None; continue
        if c == 'L':
            x, y = take(2)
            if rel: x += cx; y += cy
            lines.append(f'lineTo({fmt(x)}, {fmt(y)})'); cx, cy = x, y; lcx = lcy = lqx = lqy = None; continue
        if c == 'H':
            (x,) = take(1)
            if rel: x += cx
            lines.append(f'lineTo({fmt(x)}, {fmt(cy)})'); cx = x; lcx = lcy = lqx = lqy = None; continue
        if c == 'V':
            (y,) = take(1)
            if rel: y += cy
            lines.append(f'lineTo({fmt(cx)}, {fmt(y)})'); cy = y; lcx = lcy = lqx = lqy = None; continue
        if c == 'C':
            x1, y1, x2, y2, x, y = take(6)
            if rel: x1 += cx; y1 += cy; x2 += cx; y2 += cy; x += cx; y += cy
            lines.append(f'cubicTo({fmt(x1)}, {fmt(y1)}, {fmt(x2)}, {fmt(y2)}, {fmt(x)}, {fmt(y)})')
            lcx, lcy = x2, y2; cx, cy = x, y; lqx = lqy = None; continue
        if c == 'S':
            x2, y2, x, y = take(4)
            if rel: x2 += cx; y2 += cy; x += cx; y += cy
            x1, y1 = (2 * cx - lcx, 2 * cy - lcy) if lcx is not None else (cx, cy)
            lines.append(f'cubicTo({fmt(x1)}, {fmt(y1)}, {fmt(x2)}, {fmt(y2)}, {fmt(x)}, {fmt(y)})')
            lcx, lcy = x2, y2; cx, cy = x, y; lqx = lqy = None; continue
        if c == 'Q':
            x1, y1, x, y = take(4)
            if rel: x1 += cx; y1 += cy; x += cx; y += cy
            lines.append(f'quadraticTo({fmt(x1)}, {fmt(y1)}, {fmt(x)}, {fmt(y)})')
            lqx, lqy = x1, y1; cx, cy = x, y; lcx = lcy = None; continue
        if c == 'T':
            x, y = take(2)
            if rel: x += cx; y += cy
            x1, y1 = (2 * cx - lqx, 2 * cy - lqy) if lqx is not None else (cx, cy)
            lines.append(f'quadraticTo({fmt(x1)}, {fmt(y1)}, {fmt(x)}, {fmt(y)})')
            lqx, lqy = x1, y1; cx, cy = x, y; lcx = lcy = None; continue
        raise SystemExit(f'unsupported path command {cmd}')
    return lines

def shape_lines(el):
    t = el.tag.replace(NS, ''); g = lambda a, d='0': float(el.get(a, d))
    if t == 'path': return path_commands(el.get('d', ''))
    if t == 'circle':
        cx, cy, r = g('cx'), g('cy'), g('r'); return [f'addOval(Rect({fmt(cx - r)}, {fmt(cy - r)}, {fmt(cx + r)}, {fmt(cy + r)}))']
    if t == 'ellipse':
        cx, cy, rx, ry = g('cx'), g('cy'), g('rx'), g('ry'); return [f'addOval(Rect({fmt(cx - rx)}, {fmt(cy - ry)}, {fmt(cx + rx)}, {fmt(cy + ry)}))']
    if t == 'rect':
        x, y, w, h, rx = g('x'), g('y'), g('width'), g('height'), g('rx', el.get('ry', '0'))
        if rx: return [f'addRoundRect(RoundRect(Rect({fmt(x)}, {fmt(y)}, {fmt(x + w)}, {fmt(y + h)}), CornerRadius({fmt(rx)})))']
        return [f'addRect(Rect({fmt(x)}, {fmt(y)}, {fmt(x + w)}, {fmt(y + h)}))']
    if t == 'line':
        return [f'moveTo({fmt(g("x1"))}, {fmt(g("y1"))})', f'lineTo({fmt(g("x2"))}, {fmt(g("y2"))})']
    if t in ('polyline', 'polygon'):
        pts = [float(v) for v in re.findall(r'-?[\d.]+', el.get('points', ''))]
        out = [f'moveTo({fmt(pts[0])}, {fmt(pts[1])})'] + [f'lineTo({fmt(pts[i])}, {fmt(pts[i + 1])})' for i in range(2, len(pts), 2)]
        if t == 'polygon': out.append('close()')
        return out
    raise SystemExit(f'unsupported element {t}')

def token(v):
    if v is None or v.strip() == 'none': return None
    m = re.match(r'var\(--(\w+)\)', v.strip())
    if not m or m.group(1) not in TOKENS: raise SystemExit(f'colour {v} is not a token')
    return m.group(1)

def color_expr(tok, alpha):
    e = f'palette.{tok}'
    return f'{e}.copy(alpha = {e}.alpha * {fmt(alpha)})' if alpha != 1 else e

def paws_bounds(items):
    xs, ys = [], []
    for e in items:
        t = e.tag.replace(NS, '')
        if t == 'path':
            for cmd in path_commands(e.get('d', '')):
                vals = [float(v) for v in re.findall(r'-?\d+\.?\d*', cmd)]
                xs += vals[0::2]; ys += vals[1::2]
        elif t in ('circle', 'ellipse'):
            cx, cy = float(e.get('cx', 0)), float(e.get('cy', 0)); rx = float(e.get('r', e.get('rx', 0))); ry = float(e.get('r', e.get('ry', 0)))
            xs += [cx - rx, cx + rx]; ys += [cy - ry, cy + ry]
        elif t == 'rect':
            x, y, w, h = (float(e.get(a, 0)) for a in ('x', 'y', 'width', 'height')); xs += [x, x + w]; ys += [y, y + h]
        else:
            vals = [float(v) for v in re.findall(r'-?\d+\.?\d*', e.get('points', '') + ' ' + ' '.join(e.get(a, '') for a in ('x1', 'y1', 'x2', 'y2')))]
            xs += vals[0::2]; ys += vals[1::2]
    if not xs: return 'Rect.Zero'
    return f'Rect({fmt(min(xs))}, {fmt(min(ys))}, {fmt(max(xs))}, {fmt(max(ys))})'

def main():
    src, cls, out = sys.argv[1:4]
    root = ET.parse(src).getroot()
    groups = {g.get('id'): g for g in root.iter(NS + 'g') if g.get('id')}
    for k in ('tail', 'body', 'head', 'eyes'): assert k in groups, f'missing group {k}'
    # Layers: tail = tail group; body = body group minus head subtree; head = head minus eyes subtree, split
    # into face-before-eyes and face-after-eyes so blink scale wraps only the eyes.
    def direct_shapes(group, stop):
        """Yields (element) in document order under group, skipping the subtree of `stop` but yielding a marker."""
        for child in group:
            if child is stop: yield 'STOP'; continue
            if child.tag == NS + 'g':
                yield from direct_shapes(child, stop)
            else:
                yield child
    paws = groups.get('paws')
    body_items = [e for e in direct_shapes(groups['body'], groups['head']) if e != 'STOP']
    paw_items = list(direct_shapes(paws, None)) if paws is not None else []
    layers = {
        'shadow': [c for c in root if c.tag != NS + 'g'],
        'tail': [e for e in direct_shapes(groups['tail'], None)],
        'body': [e for e in body_items if e not in paw_items],
        'paws': paw_items,
        'eyes': [e for e in direct_shapes(groups['eyes'], None)],
    }
    paw_index = next((i for i, e in enumerate(body_items) if e in paw_items), len(body_items))
    head_items = list(direct_shapes(groups['head'], groups['eyes']))
    split = head_items.index('STOP') if 'STOP' in head_items else len(head_items)
    layers['head'] = head_items[:split]
    layers['face'] = head_items[split + 1:]
    names, used = {}, {}
    def name_for(el, layer, idx):
        base = el.get('id') or f'{layer}{idx}'
        base = re.sub(r'[^A-Za-z0-9]', '', base); base = base[0].lower() + base[1:]
        n = used.get(base, 0); used[base] = n + 1
        return base if n == 0 else f'{base}{n + 1}'
    path_decls, draw_fns, strokes = [], [], {}
    for layer in ('shadow', 'tail', 'body', 'paws', 'head', 'eyes', 'face'):
        calls = []
        if layer == 'body' and paw_items:
            layers['body'] = layers['body'][:paw_index] + ['PAWS'] + layers['body'][paw_index:]
        for idx, el in enumerate(layers[layer]):
            if el == 'PAWS':
                calls.append('    drawPaws(palette)'); continue
            n = name_for(el, layer, idx); names[id(el)] = n
            body = '\n'.join('    ' + l for l in shape_lines(el))
            path_decls.append(f'  private val {n} = Path().apply {{\n{body}\n  }}')
            op = float(el.get('opacity', 1))
            if el.get('fill') is None and el.tag not in (NS + 'line', NS + 'polyline'):
                raise SystemExit(f'<{el.tag.replace(NS, "")} id={el.get("id")}> has no fill attribute; set fill="none" or a token')
            fill = token(el.get('fill'))
            stroke = token(el.get('stroke'))
            if fill:
                calls.append(draw_call(n, color_expr(fill, op * float(el.get("fill-opacity", 1)))))
            if stroke:
                w = float(el.get('stroke-width', 1))
                stroke_color = color_expr(stroke, op * float(el.get("stroke-opacity", 1)))
                style = strokes.setdefault(w, 'stroke' + fmt(w).rstrip('f').replace('.', '_'))
                calls.append(draw_call(n, stroke_color, f'style = {style}'))
        fn = 'draw' + layer[0].upper() + layer[1:]
        draw_fns.append(f'  override fun DrawScope.{fn}(palette: MascotPalette) {{\n' + '\n'.join(calls) + '\n  }')
    tp = [fmt(v) for v in groups['tail'].get('data-pivot').split(',')]
    ep = [fmt(v) for v in groups['eyes'].get('data-pivot').split(',')]
    stroke_decls = [f'  private val {name} = Stroke({fmt(w)}, cap = StrokeCap.Round, join = StrokeJoin.Round)'
                    for w, name in strokes.items()]
    body_text = '\n'.join(path_decls + stroke_decls + draw_fns)
    candidates = ['androidx.compose.ui.geometry.CornerRadius', 'androidx.compose.ui.geometry.Offset',
                  'androidx.compose.ui.geometry.Rect', 'androidx.compose.ui.geometry.RoundRect',
                  'androidx.compose.ui.graphics.Path', 'androidx.compose.ui.graphics.StrokeCap',
                  'androidx.compose.ui.graphics.StrokeJoin', 'androidx.compose.ui.graphics.drawscope.DrawScope',
                  'androidx.compose.ui.graphics.drawscope.Stroke']
    imports = '\n'.join(f'import {c}' for c in candidates if re.search(r'\b' + c.rsplit('.', 1)[1] + r'\b', body_text + ' Offset Rect DrawScope'))
    header = f'''/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.mascot

{imports}

// Generated from {src.split('/')[-1]} by art/mascot/svg2compose.py; edit the SVG in art/mascot, not this file.
internal object {cls}Art : MascotArt {{
  override val tailPivot = Offset({tp[0]}, {tp[1]})
  override val eyesPivot = Offset({ep[0]}, {ep[1]})
  override val pawsBounds = {paws_bounds(paw_items)}
'''
    text = header + '\n'.join(path_decls + stroke_decls) + '\n\n' + '\n\n'.join(draw_fns) + '\n}\n'
    open(out, 'w').write(text)
    print(out, sum(len(v) for v in layers.values()), 'shapes')
main()
