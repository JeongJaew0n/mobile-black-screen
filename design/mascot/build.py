#!/usr/bin/env python3
"""
마스코트 모락 생성기 — 도형 정의(이 파일) → res/drawable/*.xml (VectorDrawable)

    python3 design/mascot/build.py            # 생성
    python3 design/mascot/build.py --check    # 생성물이 이 파일과 어긋났으면 실패

표준 라이브러리만 쓴다. 왜 생성하는가는 design/mascot/README.md 에 있다. 요지:

- 원본(SVG)의 흔들리는 선은 feTurbulence + feDisplacementMap 필터다. VectorDrawable 에는 필터가 없어서
  곡선을 점으로 펴고 값 노이즈로 밀어 **경로에 굽는다.** 표정마다 시드가 고정이라 매번 같은 선이 나온다.
- 원본의 빗금 채우기(pattern)도 VectorDrawable 에 없다. 모양을 <clip-path> 로 걸고 그 안에 35° 띠를 깐다.
- 색면을 선에서 (-5, 4) 어긋나게 찍는 리소 효과는 좌표에 그대로 더한다.
- 색은 전부 `@color/comp_mascot_*` — design/tokens/tokens.json 의 comp.mascot 이 원본이다.
  여기에 색 리터럴을 쓰지 않는다. 런처 단색 층과 타일만 흰색인데, 시스템이 한 색으로 다시 칠하는 자리다.
"""
import math
import random
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DRAWABLE = ROOT / "app/src/main/res/drawable"
COLORS = ROOT / "app/src/main/res/values/tokens_colors.xml"

NS = 'xmlns:android="http://schemas.android.com/apk/res/android"'
HEADER = ("<!-- ⚠️ 생성된 파일이다. 손으로 고치지 말 것.\n"
          "     원본: design/mascot/build.py (도형) · design/tokens/tokens.json (색)\n"
          "     생성: python3 design/mascot/build.py -->\n")

LINE = 3.2              # 원본 stroke-width
RISO = (-5.0, 4.0)      # 색면 어긋남
WOBBLE = 2.5            # 원본 feDisplacementMap scale 5 → ±2.5
FREQ = 0.035            # 원본 baseFrequency
SEEDS = {"base": 4, "happy": 8, "sad": 12}


def c(name):
    return f"@color/comp_mascot_{name}"


# ── 경로 ─────────────────────────────────────────────────────────────

TOKEN = re.compile(r"[MmLlHhVvCcQqZz]|-?\d*\.?\d+(?:e-?\d+)?")


def parse(d):
    """SVG path → 하위 경로 목록. 각 하위 경로는 (점 목록, 닫힘). 곡선은 점으로 편다."""
    toks = TOKEN.findall(d)
    i, cmd = 0, None
    x = y = sx = sy = 0.0
    subs, cur = [], None

    def num():
        nonlocal i
        v = float(toks[i])
        i += 1
        return v

    def bez(pts):
        n = max(4, int(sum(math.dist(pts[k], pts[k + 1]) for k in range(len(pts) - 1)) / 2.5))
        out = []
        for s in range(1, n + 1):
            t = s / n
            q = list(pts)
            while len(q) > 1:
                q = [((1 - t) * a[0] + t * b[0], (1 - t) * a[1] + t * b[1]) for a, b in zip(q, q[1:])]
            out.append(q[0])
        return out

    def line_to(nx, ny):
        n = max(1, int(math.dist((x, y), (nx, ny)) / 2.5))
        return [(x + (nx - x) * s / n, y + (ny - y) * s / n) for s in range(1, n + 1)]

    while i < len(toks):
        if toks[i].isalpha():
            cmd = toks[i]
            i += 1
        rel = cmd.islower()
        ox, oy = (x, y) if rel else (0.0, 0.0)
        C = cmd.upper()
        if C == "M":
            x, y = ox + num(), oy + num()
            sx, sy = x, y
            cur = [[(x, y)], False]
            subs.append(cur)
            cmd = "l" if rel else "L"
        elif C == "L":
            nx, ny = ox + num(), oy + num()
            cur[0] += line_to(nx, ny)
            x, y = nx, ny
        elif C == "H":
            nx = ox + num()
            cur[0] += line_to(nx, y)
            x = nx
        elif C == "V":
            ny = oy + num()
            cur[0] += line_to(x, ny)
            y = ny
        elif C == "C":
            p = [(ox + num(), oy + num()) for _ in range(3)]
            cur[0] += bez([(x, y)] + p)
            x, y = p[-1]
        elif C == "Q":
            p = [(ox + num(), oy + num()) for _ in range(2)]
            cur[0] += bez([(x, y)] + p)
            x, y = p[-1]
        elif C == "Z":
            cur[1] = True
            x, y = sx, sy
    return [(pts, closed) for pts, closed in subs]


def ellipse(cx, cy, rx, ry):
    n = max(16, int(2 * math.pi * max(rx, ry) / 2.5))
    return [([(cx + rx * math.cos(2 * math.pi * k / n), cy + ry * math.sin(2 * math.pi * k / n)) for k in range(n)], True)]


class Noise:
    """2옥타브 값 노이즈. 원본 fractalNoise 의 '느리게 출렁이는 선' 만 흉내 낸다 — 같은 값일 필요는 없다."""

    def __init__(self, seed):
        r = random.Random(seed)
        self.t = [[r.random() for _ in range(64)] for _ in range(4)]

    def _v(self, layer, x, y):
        def h(i, j):
            return self.t[layer][(i * 7 + j * 13) % 64]
        i, j = math.floor(x), math.floor(y)
        fx, fy = x - i, y - j
        sx, sy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        a = h(i, j) + (h(i + 1, j) - h(i, j)) * sx
        b = h(i, j + 1) + (h(i + 1, j + 1) - h(i, j + 1)) * sx
        return a + (b - a) * sy

    def offset(self, x, y):
        out = []
        for ch in (0, 2):
            v = self._v(ch, x * FREQ, y * FREQ) * 0.67 + self._v(ch + 1, x * FREQ * 2, y * FREQ * 2) * 0.33
            out.append((v - 0.5) * 2 * WOBBLE)
        return out


def bake(subs, noise, shift=(0.0, 0.0)):
    out = []
    for pts, closed in subs:
        moved = []
        for px, py in pts:
            px, py = px + shift[0], py + shift[1]
            dx, dy = noise.offset(px, py) if noise else (0, 0)
            moved.append((px + dx, py + dy))
        out.append((moved, closed))
    return out


def path_data(subs):
    parts = []
    for pts, closed in subs:
        head, *rest = pts
        s = f"M{head[0]:.1f},{head[1]:.1f}" + "".join(f"L{p[0]:.1f},{p[1]:.1f}" for p in rest)
        parts.append(s + ("Z" if closed else ""))
    return "".join(parts)


def bbox(subs):
    xs = [p[0] for pts, _ in subs for p in pts]
    ys = [p[1] for pts, _ in subs for p in pts]
    return min(xs), min(ys), max(xs), max(ys)


# ── 그림 요소 ─────────────────────────────────────────────────────────
# 좌표는 원본 cast.js 의 morak 과 같다(200×200).

MUG = "M54 92 L146 92 L142 162 Q141 176 126 176 L74 176 Q59 176 58 162 Z"
HANDLE = "M145 104 C180 98 182 156 141 154 L142 140 C164 142 164 116 144 118 Z"
BAND = "M57 148 L143 148 L142.4 158 L57.6 158 Z"
HEART = "M100 74 C74 54 80 28 100 44 C120 28 126 54 100 74 Z"
STEAM = "M100 84 C86 72 112 62 100 50 C90 40 102 28 114 26"
FEET = "M74 176 q0 9 9 9 q9 0 9 -9 M108 176 q0 9 9 9 q9 0 9 -9"


class Drawing:
    def __init__(self, noise):
        self.noise = noise
        self.items = []     # (xml 조각, 경계 계산용 하위 경로)

    def path(self, subs, fill=None, stroke=None, width=LINE, shift=(0.0, 0.0), wobble=True):
        baked = bake(subs, self.noise if wobble else None, shift)
        attrs = [f'android:pathData="{path_data(baked)}"']
        if fill:
            attrs.append(f'android:fillColor="{fill}"')
        if stroke:
            attrs += [f'android:strokeColor="{stroke}"', f'android:strokeWidth="{width:g}"',
                      'android:strokeLineCap="round"', 'android:strokeLineJoin="round"']
        self.items.append(("<path " + " ".join(attrs) + " />", baked, width / 2 if stroke else 0))

    def hatched(self, d, base, stripe):
        """원본 pattern(6 단위 칸, 2 단위 띠, 35° 회전)을 clip-path 안의 띠로."""
        shape = bake(parse(d), self.noise, RISO)
        x0, y0, x1, y1 = bbox(shape)
        ang = math.radians(35)
        n = (math.cos(ang), math.sin(ang))      # 띠에 수직
        u = (-math.sin(ang), math.cos(ang))     # 띠 방향
        cx, cy, reach = (x0 + x1) / 2, (y0 + y1) / 2, math.hypot(x1 - x0, y1 - y0) / 2 + 6
        stripes = []
        k = -math.ceil(reach / 6)
        while k * 6 <= reach:
            o = k * 6
            a = (cx + n[0] * o, cy + n[1] * o)
            b = (a[0] + n[0] * 2, a[1] + n[1] * 2)
            stripes.append(([(a[0] - u[0] * reach, a[1] - u[1] * reach), (a[0] + u[0] * reach, a[1] + u[1] * reach),
                             (b[0] + u[0] * reach, b[1] + u[1] * reach), (b[0] - u[0] * reach, b[1] - u[1] * reach)], True))
            k += 1
        xml = ("<group>"
               f'<clip-path android:pathData="{path_data(shape)}" />'
               f'<path android:pathData="{path_data(shape)}" android:fillColor="{base}" />'
               f'<path android:pathData="{path_data(stripes)}" android:fillColor="{stripe}" />'
               "</group>")
        self.items.append((xml, shape, 0))


def morak(expr, faceless=False):
    """표정 하나. base(기본) · happy(따끈) · sad(식음). 순서는 원본의 그리기 순서 그대로다."""
    d = Drawing(Noise(SEEDS[expr]))
    sad, happy = expr == "sad", expr == "happy"

    if happy:
        d.hatched(HEART, c("heart"), c("heart_hatch"))
        d.path(parse(HEART), stroke=c("line"))
        d.path(parse("M70 50 q-6 -8 0 -16 M132 46 q6 -8 0 -16"), stroke=c("line"), width=2.5)
    elif sad:
        d.path(parse("M90 86 q6 -8 12 -2 q4 4 10 -4"), stroke=c("steam_cold"), width=2.6)
    else:
        d.path(parse(STEAM), stroke=c("steam"), width=11, shift=RISO)
        d.path(parse(STEAM), stroke=c("line"))

    d.hatched(HANDLE, c("body"), c("body_hatch"))
    d.hatched(MUG, c("body"), c("body_hatch"))
    d.path(parse(BAND), fill=c("band"), shift=RISO)
    d.path(parse(HANDLE), stroke=c("line"))
    d.path(parse(MUG), stroke=c("line"))
    d.path(parse(BAND), stroke=c("line"), width=2.4)
    d.path(ellipse(100, 92, 46, 9), fill=c("rim"), stroke=c("line"))
    d.path(ellipse(100, 93, 38, 5.5), fill=c("coffee_cold" if sad else "coffee"))
    d.path(parse(FEET), fill=c("body"), stroke=c("line"))
    if sad:
        d.path(parse("M70 102 v12 M80 102 v16 M90 102 v10 M110 102 v10 M120 102 v16 M130 102 v12"),
               stroke=c("shade"), width=2.6)

    if not faceless:
        lx, rx, y = 82, 118, 124
        if happy:
            d.path(parse(f"M{lx - 7} {y + 3} l7 -7 l7 7 M{rx - 7} {y + 3} l7 -7 l7 7"), stroke=c("line"))
        else:
            d.path(ellipse(lx, y, 3.6, 3.6) + ellipse(rx, y, 3.6, 3.6), fill=c("line"))
            if sad:
                d.path(parse(f"M{lx - 9} {y - 10} l12 -4 M{rx + 9} {y - 10} l-12 -4"), stroke=c("line"))
                d.path(parse(f"M{lx} {y + 9} q-3 6 0 9 q3 -3 0 -9z"), fill=c("tear"), stroke=c("line"), width=2)
        blush = "".join(f"M{x - 8} 138 l5 -7 M{x - 2} 139 l5 -7 M{x + 4} 140 l5 -7 " for x in (72, 128))
        d.path(parse(blush), stroke=c("blush"), width=2.4)
        if happy:
            d.path(parse("M94 134 q6 7 12 0"), stroke=c("line"))
        elif sad:
            d.path(parse("M93 140 l4 -3 l3 3 l3 -3 l4 3"), stroke=c("line"), width=2.6)
        else:
            d.path(ellipse(100, 137, 3.4, 3.4), stroke=c("line"), width=2.8)
    return d


def extent(drawings):
    """모든 표정이 같은 틀을 쓰게 합친 경계. 표정을 바꿔도 잔이 제자리에 있다."""
    x0 = y0 = math.inf
    x1 = y1 = -math.inf
    for d in drawings:
        for _, subs, pad in d.items:
            a, b, cc, dd = bbox(subs)
            x0, y0, x1, y1 = min(x0, a - pad), min(y0, b - pad), max(x1, cc + pad), max(y1, dd + pad)
    return x0, y0, x1, y1


def vector(body, size_dp, viewport, scale=1.0, tx=0.0, ty=0.0, tint=None):
    head = (f'<vector {NS}\n    android:width="{size_dp}dp"\n    android:height="{size_dp}dp"\n'
            f'    android:viewportWidth="{viewport:g}"\n    android:viewportHeight="{viewport:g}"'
            + (f'\n    android:tint="{tint}"' if tint else "") + ">\n")
    group = (f'    <group android:scaleX="{scale:.4f}" android:scaleY="{scale:.4f}" '
             f'android:translateX="{tx:.2f}" android:translateY="{ty:.2f}">\n')
    lines = "".join(f"        {x}\n" for x in body)
    return HEADER + head + group + lines + "    </group>\n</vector>\n"


def framed(drawing, frame, size_dp=48):
    """정사각 틀(frame) 가운데에 그림을 놓는다. 틀은 100 단위 viewport 로 정규화."""
    x0, y0, x1, y1 = frame
    side = max(x1 - x0, y1 - y0) + 4
    s = 100 / side
    tx = (100 - (x1 - x0) * s) / 2 - x0 * s
    ty = (100 - (y1 - y0) * s) / 2 - y0 * s
    return vector([x for x, _, _ in drawing.items], size_dp, 100, s, tx, ty)


def launcher_foreground(drawing, frame):
    """적응형 아이콘 108×108. 안전 영역은 가운데 지름 66 원. 잔은 위아래가 길고 김 쪽 모서리가 비어 높이 64 까지 들어간다."""
    x0, y0, x1, y1 = frame
    s = 64 / (y1 - y0)
    tx = 54 - (x0 + x1) / 2 * s
    ty = 54 - (y0 + y1) / 2 * s
    return vector([x for x, _, _ in drawing.items], 108, 108, s, tx, ty)


def launcher_monochrome(frame):
    """테마 아이콘 층. 시스템이 한 색으로 칠하므로 면 없이 선만 — 잔·손잡이·테·김·눈."""
    d = Drawing(Noise(SEEDS["base"]))
    white = "#FFFFFFFF"
    d.path(parse(STEAM), stroke=white, width=5)
    d.path(parse(HANDLE), stroke=white, width=5)
    d.path(parse(MUG), stroke=white, width=5)
    d.path(ellipse(100, 92, 46, 9), stroke=white, width=5)
    d.path(ellipse(82, 124, 4.5, 4.5) + ellipse(118, 124, 4.5, 4.5), fill=white)
    return launcher_foreground(d, frame)


def tile():
    """퀵 설정 타일·알림 작은 아이콘. 모락에서 단색으로 남는 것은 S자 김 한 가닥뿐이다."""
    return (HEADER + f'<vector {NS}\n    android:width="24dp"\n    android:height="24dp"\n'
            '    android:viewportWidth="24"\n    android:viewportHeight="24"\n'
            '    android:tint="?android:attr/colorControlNormal">\n'
            '    <path\n        android:pathData="M11,22 C7,18.5 15,16 11.5,12 C8.5,9 12,4.5 16,3"\n'
            '        android:strokeColor="@android:color/white"\n        android:strokeWidth="2.4"\n'
            '        android:strokeLineCap="round" />\n</vector>\n')


def outputs():
    faces = {e: morak(e) for e in ("base", "happy", "sad")}
    mug = morak("base", faceless=True)
    frame = extent(list(faces.values()))
    return {
        DRAWABLE / "morak_base.xml": framed(faces["base"], frame),
        DRAWABLE / "morak_happy.xml": framed(faces["happy"], frame),
        DRAWABLE / "morak_sad.xml": framed(faces["sad"], frame),
        # 버블 기호. 얼굴은 46dp 버블 안에서 뭉개진다 — 몸통과 김만. 틀은 자기 경계로(버블 가운데에 오게).
        DRAWABLE / "morak_mug.xml": framed(mug, extent([mug])),
        DRAWABLE / "ic_launcher_foreground.xml": launcher_foreground(faces["base"], frame),
        DRAWABLE / "ic_launcher_monochrome.xml": launcher_monochrome(frame),
        DRAWABLE / "ic_tile.xml": tile(),
    }


def check_colors(files):
    have = set(re.findall(r'<color name="([a-z0-9_]+)"', COLORS.read_text(encoding="utf-8")))
    used = {m for text in files.values() for m in re.findall(r"@color/([a-z0-9_]+)", text)}
    missing = sorted(used - have)
    if missing:
        print(f"없는 색: {', '.join(missing)} — tokens.json 의 comp.mascot 에 넣고 tokens/build.py 를 돌릴 것.",
              file=sys.stderr)
        return False
    return True


def main(argv):
    files = outputs()
    if not check_colors(files):
        return 2
    if "--check" in argv:
        bad = [p for p, text in files.items() if (p.read_text(encoding="utf-8") if p.exists() else "") != text]
        for p in bad:
            print(f"어긋남: {p.relative_to(ROOT)} 가 design/mascot/build.py 와 다르다. 다시 생성할 것.", file=sys.stderr)
        if bad:
            return 1
        print(f"일치: 모락 그림 {len(files)}개")
        return 0
    for p, text in files.items():
        p.write_text(text, encoding="utf-8")
        print(f"생성: {p.relative_to(ROOT)}  ({len(text) // 1024} KB)")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
