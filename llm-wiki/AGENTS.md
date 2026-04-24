# seaFOX LLM Wiki Schema

Dieses Schema steuert nur die Pflege von `llm-wiki/`. Fuer Codeaenderungen, Builds, Tests und Produktregeln gilt weiterhin der Projektcodex in `../AGENTS.md`.

## Schichten

- `raw/`: unveraenderte Quellen. Externe Quellen hier ablegen und nach dem Ingest nicht umschreiben.
- `wiki/`: LLM-gepflegte Markdown-Seiten. Diese Seiten duerfen vom Agenten erstellt, zusammengefuehrt und aktualisiert werden.
- `AGENTS.md`: diese Arbeitsanleitung. Passe sie an, wenn sich die Wiki-Konventionen bewahrt oder geaendert haben.

## Grundsaetze

- Quellen zuerst, Synthese danach. Jede wesentliche Behauptung braucht eine Quelle, einen Codepfad oder einen klaren Hinweis, dass sie eine Ableitung ist.
- Wiki-Seiten sind Arbeitsgedaechtnis, nicht Autoritaet. Bei Produkt-, Safety-, Lizenz-, Datenschutz- oder Release-Fragen die Primarquellen pruefen.
- Ein Thema pro Seite. Lieber bestehende Seiten aktualisieren als nahe Duplikate anlegen.
- Verlinke verwandte Seiten mit `[[wikilinks]]`; verlinke Code und Dokumente zusaetzlich mit normalen Markdown-Links.
- Markiere Widersprueche sichtbar in einem Abschnitt `Contradictions / Checks`, statt sie still zu glaetten.
- Halte `wiki/index.md` und `wiki/log.md` aktuell.

## Seitenformat

Nutze YAML-Frontmatter, wenn eine Seite Wissen zusammenfasst:

```markdown
---
title: Kurzer Titel
type: index | log | overview | module | source | decision | question
status: current | draft | stale | needs-check
updated: YYYY-MM-DD
sources:
  - ../relative/source.md
---
```

Danach bevorzugt diese Abschnitte:

- `Summary`
- `Key Facts`
- `Important Links`
- `Open Questions`
- `Contradictions / Checks` nur wenn relevant

## Ingest Workflow

1. Quelle identifizieren. Externe Quellen in `raw/` ablegen; interne Repo-Dateien direkt zitieren.
2. `wiki/index.md`, relevante Seiten und die letzten Eintraege in `wiki/log.md` lesen.
3. Eine Quellenzusammenfassung unter `wiki/sources/` erstellen oder aktualisieren.
4. Relevante Themen-, Modul- oder Entscheidungsseiten aktualisieren.
5. Neue Seiten im Index eintragen.
6. Einen Log-Eintrag an `wiki/log.md` anhaengen:

```markdown
## [YYYY-MM-DD] ingest | Kurztitel

- Sources: `path`, URL
- Updated: `wiki/page.md`, `wiki/other.md`
- Notes: wichtigste Aenderung oder offene Pruefung
```

## Query Workflow

1. `wiki/index.md` lesen.
2. Relevante Wiki-Seiten lesen.
3. Bei Bedarf Quellen oder Codepfade nachpruefen.
4. Antwort mit Quellenpfaden geben.
5. Wenn die Antwort wiederverwendbar ist, als neue Wiki-Seite oder Abschnitt ablegen und loggen.

## Lint Workflow

Regelmaessig oder auf Anfrage:

- orphan pages finden, die im Index nicht vorkommen.
- veraltete Claims mit neueren Quellen vergleichen.
- tote oder unklare Links markieren.
- fehlende Modul-/Entscheidungsseiten vorschlagen.
- offene Fragen nach Prioritaet sortieren.

Nutze einfache Unix-Werkzeuge zuerst:

```bash
find llm-wiki/wiki -name "*.md" | sort
rg -n "TODO|needs-check|Contradictions|Open Questions" llm-wiki/wiki
rg -n "\\[\\[" llm-wiki/wiki
```
