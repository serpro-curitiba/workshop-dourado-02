#!/usr/bin/env python3
"""
Validador de rastreabilidade legado -> spec (ADR-003).

Para cada arquivo de spec em specs/**/spec.md, parseia REQ-IDs em YAML e valida:
  (a) campo source_legacy presente e nao vazio
  (b) se aponta para 01-arqueologia/legado-sifap/..., o arquivo existe
      e (se incluir #L<start>-L<end>) as linhas existem
  (c) se for [GREENFIELD], tem texto de justificativa apos o marcador
  (d) campo acceptance tem pelo menos 1 criterio

Saida: codigo 0 se tudo OK; 1 se houver violacoes (lista impressa em stderr).

Uso:
  python3 scripts/validate-traceability.py             # valida todos os specs
  python3 scripts/validate-traceability.py specs/X/spec.md  # valida 1 arquivo
"""
from __future__ import annotations

import re
import sys
from dataclasses import dataclass
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
LEGACY_PREFIX = "01-arqueologia/legado-sifap/"
RANGE_RE = re.compile(r"^(?P<path>[^#\s]+?)(?:#L(?P<start>\d+)(?:-L(?P<end>\d+))?)?$")
REQ_HEADER_RE = re.compile(r"^(REQ-[A-Z0-9]+-\d+):\s*$")
FIELD_RE = re.compile(r"^\s{2,4}([a-z_]+):\s*(.*)$")
LIST_ITEM_RE = re.compile(r"^\s{4,6}-\s+")


@dataclass
class Violation:
    spec_file: Path
    req_id: str
    rule: str
    detail: str

    def render(self) -> str:
        rel = self.spec_file.relative_to(REPO_ROOT)
        return f"::error file={rel}::[{self.req_id}] {self.rule}: {self.detail}"


def parse_requirements(spec_path: Path) -> list[dict]:
    """Parse REQ-IDs do arquivo de spec. Aceita YAML embutido em codeblock ou direto."""
    reqs: list[dict] = []
    current: dict | None = None
    current_field: str | None = None
    in_code_block = False

    for raw_line in spec_path.read_text(encoding="utf-8").splitlines():
        if raw_line.strip().startswith("```"):
            in_code_block = not in_code_block
            continue
        # so consideramos linhas dentro de blocos yaml ou em estilo plano
        line = raw_line.rstrip()
        if not line:
            current_field = None
            continue

        header = REQ_HEADER_RE.match(line)
        if header:
            if current is not None:
                reqs.append(current)
            current = {"_req_id": header.group(1), "_line": 0, "acceptance": []}
            current_field = None
            continue

        if current is None:
            continue

        if LIST_ITEM_RE.match(raw_line) and current_field == "acceptance":
            current["acceptance"].append(raw_line.strip()[1:].strip())
            continue

        m = FIELD_RE.match(raw_line)
        if m:
            key, value = m.group(1), m.group(2).strip()
            if key == "acceptance":
                current_field = "acceptance"
                current["acceptance"] = []
            else:
                current_field = key
                current[key] = value.strip('"').strip("'")
    if current is not None:
        reqs.append(current)
    return reqs


def validate_source_legacy(value: str) -> tuple[bool, str]:
    """Retorna (ok, mensagem_de_erro_se_nao_ok)."""
    if not value:
        return False, "source_legacy vazio"

    if value.startswith("[GREENFIELD]"):
        justification = value[len("[GREENFIELD]"):].strip()
        if not justification:
            return False, "GREENFIELD sem justificativa apos o marcador"
        return True, ""

    if not value.startswith(LEGACY_PREFIX):
        return False, f"source_legacy deve comecar com '{LEGACY_PREFIX}' ou '[GREENFIELD]', obtido: '{value}'"

    m = RANGE_RE.match(value)
    if not m:
        return False, f"formato invalido: '{value}'"

    target = REPO_ROOT / m.group("path")
    if not target.exists():
        return False, f"arquivo legado nao existe: {m.group('path')}"

    start = m.group("start")
    end = m.group("end")
    if start:
        try:
            line_count = sum(1 for _ in target.open(encoding="utf-8", errors="replace"))
        except OSError as e:
            return False, f"erro lendo {m.group('path')}: {e}"
        start_i = int(start)
        end_i = int(end) if end else start_i
        if start_i < 1 or end_i > line_count or start_i > end_i:
            return False, (
                f"intervalo de linhas L{start_i}-L{end_i} invalido "
                f"para {m.group('path')} (arquivo tem {line_count} linhas)"
            )
    return True, ""


def validate_spec(spec_path: Path) -> list[Violation]:
    reqs = parse_requirements(spec_path)
    violations: list[Violation] = []
    if not reqs:
        violations.append(Violation(spec_path, "<none>", "no-req-ids",
                                    "nenhum REQ-ID encontrado no spec"))
        return violations

    for r in reqs:
        rid = r["_req_id"]
        src = r.get("source_legacy", "").strip()
        ok, msg = validate_source_legacy(src)
        if not ok:
            violations.append(Violation(spec_path, rid, "source_legacy", msg))
        acc = r.get("acceptance", [])
        if not isinstance(acc, list) or len(acc) == 0:
            violations.append(Violation(spec_path, rid, "acceptance",
                                        "deve ter pelo menos 1 criterio"))
    return violations


def main(argv: list[str]) -> int:
    if len(argv) > 1:
        targets = [Path(p) for p in argv[1:]]
    else:
        targets = sorted(REPO_ROOT.glob("specs/**/spec.md"))

    if not targets:
        print("Nenhum spec encontrado em specs/**/spec.md", file=sys.stderr)
        return 0

    all_violations: list[Violation] = []
    for spec in targets:
        rel = spec.relative_to(REPO_ROOT) if spec.is_absolute() else spec
        v = validate_spec(spec if spec.is_absolute() else REPO_ROOT / spec)
        if v:
            all_violations.extend(v)
            print(f"[FAIL] {rel} ({len(v)} violacoes)", file=sys.stderr)
        else:
            print(f"[ OK ] {rel}")

    if all_violations:
        print("\n--- VIOLACOES ---", file=sys.stderr)
        for v in all_violations:
            print(v.render(), file=sys.stderr)
        print(f"\nTotal: {len(all_violations)} violacoes em {len(targets)} arquivo(s).", file=sys.stderr)
        return 1
    print(f"\nTodos os {len(targets)} spec(s) passaram na rastreabilidade.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
