#!/usr/bin/env python3
"""
디자인 토큰 생성기 — tokens.json (W3C DTCG) → ui/theme/Tokens.kt

    python3 design/tokens/build.py            # 생성
    python3 design/tokens/build.py --check    # 생성물이 원본과 어긋났으면 실패

표준 라이브러리만 쓴다. 규칙은 design/tokens/README.md 에 있다. 요지:

- 토큰 = `$value` 가 있는 노드. 그룹 = 나머지. `$type` 은 그룹에서 상속된다.
- alias `"{ref.palette.white}"` 는 Kotlin 참조(`Ref.Palette.white`)로 낸다 — 값을 풀어 쓰면
  층 구조가 생성물에서 사라진다.
- 층 규칙: ref 는 리터럴만. sys 는 ref 만 가리킨다. comp 는 sys·ref 만 가리킨다.
  ref 밖의 리터럴은 `number`(비율)만 허용한다 — 비율에는 팔레트가 없다.
- dimension 의 px 는 논리 픽셀 → `.dp`. 경로에 fontSize / letterSpacing 이 있으면 `.sp`.
- `Color(...)` 는 완전한 이름으로 낸다. `object Color` 안에서 `Color(...)` 는 자기를 가리킨다.
"""
import json
import re
import sys
import textwrap
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path(__file__).with_name("tokens.json")
TARGET = ROOT / "app/src/main/java/com/jjw/blackscreen/ui/theme/Tokens.kt"
# Compose 밖(테마 XML·런처 아이콘·알림)에서 쓰는 색. 색 토큰만, alias 를 끝까지 풀어서 낸다.
TARGET_XML = ROOT / "app/src/main/res/values/tokens_colors.xml"

LAYER_MAY_REFERENCE = {"ref": set(), "sys": {"ref"}, "comp": {"ref", "sys"}}
ALIAS = re.compile(r"^\{([A-Za-z0-9_.]+)\}$")
SP_SEGMENTS = {"fontsize", "letterspacing"}


class TokenError(Exception):
    pass


def pascal(name: str) -> str:
    return name[:1].upper() + name[1:]


def kotlin_path(path):
    """['sys', 'color', 'canvas'] → 'Sys.Color.canvas'"""
    return ".".join([*(pascal(p) for p in path[:-1]), path[-1]])


def collect(node, path, inherited_type, out, groups):
    """토큰을 (path, type, value, description) 으로 평탄화. 그룹 설명은 groups 에."""
    if "$description" in node and "$value" not in node and path:
        groups[tuple(path)] = node["$description"]
    node_type = node.get("$type", inherited_type)
    if "$value" in node:
        if node_type is None:
            raise TokenError(f"{'.'.join(path)}: $type 이 없다")
        out[tuple(path)] = (node_type, node["$value"], node.get("$description"))
        return
    for key, child in node.items():
        if key.startswith("$"):
            continue
        if not isinstance(child, dict):
            raise TokenError(f"{'.'.join(path + [key])}: 그룹도 토큰도 아니다")
        collect(child, path + [key], node_type, out, groups)


def fmt_number(v):
    """JSON 리터럴이 실수(1.0, 0.7)면 Float, 정수(50)면 Int. 1.0 을 1 로 접지 않는다 — 불투명도 1.0 은 Float 이어야 한다."""
    if isinstance(v, bool) or not isinstance(v, (int, float)):
        raise TokenError(f"number 가 아니다: {v!r}")
    return repr(float(v)) + "f" if isinstance(v, float) else str(v)


def kotlin_literal(path, ttype, value):
    if ttype == "color":
        m = re.fullmatch(r"#([0-9A-Fa-f]{6})([0-9A-Fa-f]{2})?", value)
        if not m:
            raise TokenError(f"{'.'.join(path)}: 색은 #RRGGBB 또는 #RRGGBBAA 여야 한다: {value}")
        rgb, alpha = m.group(1).upper(), (m.group(2) or "FF").upper()
        return f"androidx.compose.ui.graphics.Color(0x{alpha}{rgb})", None
    if ttype == "dimension":
        if not (isinstance(value, dict) and value.get("unit") == "px"):
            raise TokenError(f"{'.'.join(path)}: dimension 은 {{value, unit: px}} 여야 한다: {value}")
        unit = "sp" if any(seg.lower() in SP_SEGMENTS for seg in path) else "dp"
        return f"{fmt_number(value['value'])}.{unit}", unit
    if ttype == "duration":
        if not (isinstance(value, dict) and value.get("unit") == "ms"):
            raise TokenError(f"{'.'.join(path)}: duration 은 {{value, unit: ms}} 여야 한다: {value}")
        return f"{int(value['value'])}", "duration"
    if ttype == "number":
        return fmt_number(value), None
    raise TokenError(f"{'.'.join(path)}: 지원하지 않는 $type {ttype}")


def kdoc(text, indent):
    if not text:
        return []
    # 설명에 "*/" 가 있으면 KDoc 이 일찍 닫혀 생성물이 컴파일되지 않는다.
    text = text.replace("*/", "*\u2215")
    lines = textwrap.wrap(text, width=96 - len(indent) - 3)
    if len(lines) == 1:
        return [f"{indent}/** {lines[0]} */"]
    return [f"{indent}/**", *(f"{indent} * {l}" for l in lines), f"{indent} */"]


def build_tree(tokens):
    tree = {}
    for path in tokens:
        cur = tree
        for seg in path[:-1]:
            cur = cur.setdefault(seg, {})
        cur[path[-1]] = None  # leaf marker
    return tree


def emit(tokens, groups):
    used_units = set()
    out = []

    def emit_group(name, subtree, path, indent):
        out.extend(kdoc(groups.get(tuple(path)), indent))
        out.append(f"{indent}object {pascal(name)} {{")
        inner = indent + "    "
        first = True
        for key, child in subtree.items():
            if not first:
                out.append("")
            first = False
            child_path = path + [key]
            if child is None:
                ttype, value, desc = tokens[tuple(child_path)]
                out.extend(kdoc(desc, inner))
                if isinstance(value, str) and ALIAS.match(value):
                    target = ALIAS.match(value).group(1).split(".")
                    out.append(f"{inner}val {key} = {kotlin_path(target)}")
                else:
                    literal, unit = kotlin_literal(child_path, ttype, value)
                    if unit in ("dp", "sp"):
                        used_units.add(unit)
                    decl = ": Int" if unit == "duration" else ""
                    out.append(f"{inner}val {key}{decl} = {literal}")
            else:
                emit_group(key, child, child_path, inner)
        out.append(f"{indent}}}")

    tree = build_tree(tokens)
    for layer, subtree in tree.items():
        emit_group(layer, subtree, [layer], "")
        out.append("")

    header = [
        "// ⚠️ 생성된 파일이다. 손으로 고치지 말 것.",
        "//",
        "// 원본: design/tokens/tokens.json (W3C Design Tokens 형식)",
        "// 생성: python3 design/tokens/build.py",
        "// 검사: python3 design/tokens/build.py --check",
        "//",
        "// 값을 바꾸려면 JSON 을 고치고 다시 생성한다. 여기서 바꾸면 다음 생성에서 사라진다.",
        "// 층 규칙 — Ref 는 원시값, Sys 는 의도(다른 제품에 가져가는 층), Comp 는 이 앱의 부품.",
        "// Sys 는 Ref 만, Comp 는 Sys·Ref 만 가리킨다.",
        "",
        "package com.jjw.blackscreen.ui.theme",
        "",
    ]
    imports = [f"import androidx.compose.ui.unit.{u}" for u in sorted(used_units)]
    return "\n".join(header + imports + [""] + out).rstrip("\n") + "\n"


def validate(tokens):
    """층 규칙과 alias 대상 존재 여부."""
    for path, (ttype, value, _) in tokens.items():
        layer = path[0]
        if layer not in LAYER_MAY_REFERENCE:
            raise TokenError(f"{'.'.join(path)}: 최상위는 ref / sys / comp 여야 한다")
        if isinstance(value, str) and ALIAS.match(value):
            target = tuple(ALIAS.match(value).group(1).split("."))
            if target not in tokens:
                raise TokenError(f"{'.'.join(path)}: alias 대상이 없다: {value}")
            if target[0] not in LAYER_MAY_REFERENCE[layer]:
                raise TokenError(f"{'.'.join(path)}: {layer} 는 {target[0]} 를 가리킬 수 없다")
            target_type = tokens[target][0]
            if target_type != ttype:
                raise TokenError(f"{'.'.join(path)}: $type {ttype} 인데 대상은 {target_type}")
        elif layer != "ref" and ttype != "number":
            raise TokenError(f"{'.'.join(path)}: {layer} 층의 {ttype} 리터럴. ref 를 가리킬 것 (리터럴은 number 만)")


def resolve(tokens, path):
    """alias 를 끝까지 따라가 리터럴 값을 돌려준다."""
    seen = set()
    ttype, value, _ = tokens[path]
    while isinstance(value, str) and ALIAS.match(value):
        if path in seen:
            raise TokenError(f"{'.'.join(path)}: alias 순환")
        seen.add(path)
        path = tuple(ALIAS.match(value).group(1).split("."))
        ttype, value, _ = tokens[path]
    return value


def emit_xml(tokens):
    """sys·comp 의 색 토큰 → <color name="sys_color_canvas">#AARRGGBB</color>. ref 는 내지 않는다 — XML 도 의도 이름으로 쓴다."""
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        "<!--",
        "  ⚠️ 생성된 파일이다. 손으로 고치지 말 것.",
        "  원본: design/tokens/tokens.json · 생성: python3 design/tokens/build.py",
        "  Compose 밖(테마 XML·런처 아이콘·알림)에서 쓰는 색. sys·comp 의 색 토큰만, alias 를 풀어서 낸다.",
        "-->",
        "<!-- 전부 내보내므로 XML 에서 안 쓰는 색이 대부분이다. 미사용 경고는 의도된 것. -->",
        '<resources xmlns:tools="http://schemas.android.com/tools" tools:ignore="UnusedResources">',
    ]
    for path, (ttype, _, desc) in tokens.items():
        if ttype != "color" or path[0] == "ref":
            continue
        hexv = resolve(tokens, path)
        rgb, alpha = hexv[1:7].upper(), (hexv[7:9] or "FF").upper()
        name = "_".join(re.sub(r"(?<!^)(?=[A-Z])", "_", seg).lower() for seg in path)
        lines.append(f'    <color name="{name}">#{alpha}{rgb}</color>')
    lines.append("</resources>")
    return "\n".join(lines) + "\n"


def main(argv):
    data = json.loads(SOURCE.read_text(encoding="utf-8"))
    tokens, groups = {}, {}
    for layer in ("ref", "sys", "comp"):
        if layer in data:
            collect(data[layer], [layer], None, tokens, groups)
    validate(tokens)
    outputs = {TARGET: emit(tokens, groups), TARGET_XML: emit_xml(tokens)}

    if "--check" in argv:
        bad = [t for t, text in outputs.items()
               if (t.read_text(encoding="utf-8") if t.exists() else "") != text]
        for t in bad:
            print(f"어긋남: {t.relative_to(ROOT)} 가 tokens.json 과 다르다. build.py 를 돌릴 것.", file=sys.stderr)
        if bad:
            return 1
        print(f"일치: {', '.join(str(t.relative_to(ROOT)) for t in outputs)} ({len(tokens)} 토큰)")
        return 0

    for t, text in outputs.items():
        t.write_text(text, encoding="utf-8")
        print(f"생성: {t.relative_to(ROOT)}")
    print(f"({len(tokens)} 토큰)")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main(sys.argv[1:]))
    except TokenError as e:
        print(f"토큰 오류: {e}", file=sys.stderr)
        sys.exit(2)
